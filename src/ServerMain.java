
import SocialNetworkClasses.SocialNetwork;
import SocialNetworkClasses.SocialNetworkInterface;
import java.io.*;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.*;




public class ServerMain{

    //file di config
    private final String configFilePath;

    private int registerServicePort = 6969;
    private int notifyFollowServicePort = 6968;
    private int serverPort = 6967;
    private String registerServiceName = "REGISTER_SERVICE";
    private String notifyFollowServiceName = "NOTIFY_FOLLOW_SERVICE";

    private int multPort = 6966;
    private String multicastAddr = "230.0.0.1";

    //directory base (da richiedere in input)
    private String dir = null;
    //numero backup differenti
    private int backupMax = 3;

    //ogni quanti cicli di aggiornamento del portafoglio fare un beckup del server
    private int backupTimer = 300000;
    //ongi quanto aggiornare i portafogli
    private int walletTimer = 150000;

    //attivare le stampe
    private static boolean verbose = true;

    //ricompensa autore
    private double authorPerc = 0.7;

    //max thread
    private int nThread = 0;

    //lista utenti registrati e social
    private final HashMap<String, SHPsw> registedUser = new HashMap<>();
    private SocialNetworkInterface social;

    //thread e task importanti per l'esecuzione del server
    //alcune non necessarie come variabili globali
    private Thread serverCA;
    private ServerBK bakcupT;
    private ServerWU walletT;
    private ServerSocket ss;
    private ExecutorService tpool;

    //avvio
    public static void main(String[] args) throws Exception {
        if(args.length >= 1) (new ServerMain(args[0])).start();
        else (new ServerMain(null)).start();
    }

    public ServerMain(String config){
        configFilePath = config;
    }

    public void start() throws IOException, InterruptedException {

        //parsing file di config
        if(configFilePath != null){
            File config = new File(configFilePath);
            if(config.exists()){
                parseConfig(config);
                System.out.println("Avvio server con file config: " + configFilePath);
            }
        }else{
            System.out.println("Avvio server con parametri di default");
        }

        //strutture dati ecc
        social = new SocialNetwork(authorPerc);
        ss = new ServerSocket(serverPort);
        if(nThread == 0)
            tpool = Executors.newCachedThreadPool();
        else
            tpool = Executors.newFixedThreadPool(nThread);


        //ripristino stato
        if(dir != null){
            bakcupT = new ServerBK(backupTimer, social, registedUser,dir, backupMax);
            bakcupT.recState();
        }

        //INIZIO INIZIO RMI 1
        /* Creazione di un'istanza dell'oggetto EUStatsService */
        stamp("Inizializzando il servizio di registrazione...");
        SRegisterService statsService = new SRegisterServiceImp(registedUser, social);
        SRegisterService stub1 = (SRegisterService) UnicastRemoteObject.exportObject(statsService, 0);

        LocateRegistry.createRegistry(registerServicePort);
        Registry reg1 = LocateRegistry.getRegistry(registerServicePort);

        reg1.rebind(registerServiceName , stub1);
        stamp("Servizio di registrazione pronto");
        //FINE INIZO 1

        //INIZIO INIZIO RMI 2
        stamp("Inizializzando il servizio di notifica dei follow...");
        SNotifyFollowImp followService = new SNotifyFollowImp();
        SNotifyFollow stub2 = (SNotifyFollow) UnicastRemoteObject.exportObject(followService,0);

        LocateRegistry.createRegistry(notifyFollowServicePort);
        Registry reg2 =LocateRegistry.getRegistry(notifyFollowServicePort);
        reg2.rebind(notifyFollowServiceName, stub2);
        stamp("Servizio di notifica dei follow pronto");
        //FINE INIZO 2


        //avvio thread utili
        //client accepter
        serverCA = new Thread(new ServerCA(social,ss,tpool,registedUser, followService, new String[]{multicastAddr, ""+multPort}));
        serverCA.setDaemon(true); //per chiusure forzare easy
        serverCA.start();
        //backup thread
        if(dir != null) {
            bakcupT.setDaemon(true);
            bakcupT.start();
        }
        //wallet updater
        walletT = new ServerWU(walletTimer, social, multicastAddr, multPort);
        walletT.setDaemon(true);
        walletT.start();


        try {
            //attesa comandi console (stop per terminare)
            Scanner s = new Scanner(System.in);
            String in = s.nextLine();
            while (!in.equals("stop")) {
                switch (in) {
                    case "social":
                        System.out.println(social);
                        break;
                    case "reg":
                        for (SHPsw dummy : registedUser.values()) {
                            System.out.println(dummy);
                        }
                        break;
                    case "update":
                        social.updateWallet();
                        System.out.println("Aggiornamento portafogli effettuato");
                        break;
                    case "help":
                        System.out.println("\nComandi disponibili: \nsocial : stampa una semplice rappresentazione del contenuto del social\nreg : " +
                                "stampa una semplice rappresentazione dell'insieme di utenti registrati con associati dadi di login" +
                                "\nupdate : aggiorna i portafogli degli utenti nel social (non resetta il timer per il prossimo aggiornamento)" +
                                "\nstop : termina l'esecuzione del server eseguendo prima un salvataggio dello stato");
                        break;
                    default:
                        System.out.println("comando non riconosciuto");
                }
                in = s.nextLine();
            }
        } catch (Exception e) {
            System.out.println("Errore ricezione comandi, chiusura server");
        }

        stamp("Interruzione thread e termine accettazione richieste dai client...");
        //interruzione thread
        //bakcup
        if(dir != null) {
            bakcupT.interrupt();
        }
        //client accepter
        serverCA.interrupt();
        ss.close();
        //wallet updater
        walletT.interrupt();

        //tpool
        tpool.shutdownNow();
        tpool.awaitTermination(100, TimeUnit.SECONDS);
        stamp("Attesa terminata");

        //INIZIO FINE RMI 1
        stamp("Chiudendo il servizio di registrazione...");
        UnicastRemoteObject.unexportObject(statsService , true);
        stamp("Servizio di registrazione chiuso");
        //FINE FINE RMI 1
        //INIZIO FINE RMI 2
        stamp("Chiudendo il servizio di notifica dei follow...");
        UnicastRemoteObject.unexportObject(followService, true);
        stamp("Servizio di notifica dei follow chiuso");
        //FINE FINE RMI 2


        //serializzazione
        if(dir != null) {
            bakcupT.saveState(0);
        }
    }

    //metodo che esegue il parsing del file di configurazione
    public void parseConfig(File f) throws FileNotFoundException {
        Scanner in = new Scanner(f);
        int i = 1;

        while(in.hasNextLine()){
            String line = in.nextLine();
            //commento
            if(line.length() == 0 || line.charAt(0) == '#'){
                i++;
                continue;
            }
            //vengono modificte le variabili globali se viene trovata una riga corrispondente corretta nel file
            try {
                //eliminazione spazzi e parsing della linea
                String[] arg = line.replace(" ", "").split("=");
                if(arg.length != 2) throw new IllegalArgumentException();//linea scritta male
                switch (arg[0]) {
                    case "backupTimer": backupTimer = Integer.parseInt(arg[1]); break;
                    case "backupPath": dir = arg[1]; break;
                    case "backupMax":
                        backupMax = Integer.parseInt(arg[1]);
                        if(backupMax < 0) backupMax = 1;
                        if(backupMax > 15) backupMax = 15;
                        break;
                    case "walletTimer": walletTimer = Integer.parseInt(arg[1]); break;
                    case "authorPerc": authorPerc = Double.parseDouble(arg[1]); break;
                    case "verbose":
                        if (arg[1].equals("true") || arg[1].equals("TRUE") || arg[1].equals("True")
                                || arg[1].equals("1")) verbose = true;
                        else if (arg[1].equals("false") || arg[1].equals("FALSE") || arg[1].equals("False")
                                || arg[1].equals("0")) verbose = false;
                        break;
                    case "registerServicePort": registerServicePort = Integer.parseInt(arg[1]); break;
                    case "notifyFollowServicePort": notifyFollowServicePort = Integer.parseInt(arg[1]); break;
                    case "registerServiceName": registerServiceName = arg[1]; break;
                    case "notifyFollowServiceName": notifyFollowServiceName = arg[1]; break;
                    case "serverPort": serverPort = Integer.parseInt(arg[1]); break;
                    case "nThread": nThread = Integer.parseInt(arg[1]); break;
                    case "multicastAddress": multicastAddr = arg[1]; break;
                    case "multicastPort": multPort = Integer.parseInt(arg[1]); break;
                    default: System.out.println("parametro \"" + arg[0] + "\" non riconosciuto");
                }
            }catch (Exception e){
                System.out.println("errore linea " + i);
            }
            i++;
        }
    }

    public static void stamp(String s){
        if(verbose) System.out.println(s);
    }
}

