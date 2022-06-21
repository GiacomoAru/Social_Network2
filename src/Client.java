import CustomException.UserAlreadyExistException;
import SocialNetworkClasses.Post;
import SocialNetworkClasses.PostToken;
import SocialNetworkClasses.WalletTransaction;
import com.google.gson.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.*;

public class Client implements Runnable{

    private Socket serverTcpS;
    private MulticastSocket multS;
    private SNotifyFollow serverObj;
    private InputStream in;
    private OutputStream out;
    private String name;
    private Thread notWallet;

    //visto che la showFeed è molto costosa il client terrà una chace del feed dell'utente che rappresenta
    private List<PostToken> feed;
    //data di sincrinizzazione del feed;
    private Calendar feedDate = Calendar.getInstance();

    //struttura dati per memorizzare i follow
    //utilizzare il lock sul monitor per ecitare problemi di concorrenza
    private ArrayList<String> follower = new ArrayList<>();
    private final Deque<CNotification> followNotication = new CCustomDeque<>();

    //input
    private int regPort = 6969;
    private int followPort = 6968;
    private int tcpPort = 6967;

    private int timeout = 15000;

    private String addressReg = "localhost";
    private String addressFollow = "localhost";
    private String addressServer = "localhost";

    private String regName = "REGISTER_SERVICE";
    private String followName = "NOTIFY_FOLLOW_SERVICE";

    private boolean debug = false;

    public Client(String pathConfig){
        parseConfig(pathConfig);
        resetFeed();
        resetFollow();
    }

    //metodo per far partite il client che attende comandi dalla shell e esegue
    public void run(){
        Scanner s = new Scanner(System.in);
        stamp(0, "Client avviato con successo");

        String in = s.nextLine();
        while(!in.equals("close")){

            strignParser(in);
            in = s.nextLine();
        }

        chiusuraTcp();
    }

    //trasforma i comandi passati in input in richieste al server, se serve
    public void strignParser(String com){
        String[] split = com.split(" ");
        if(split.length < 1){
            stamp(1, "errore");
        }else{
            switch (split[0]){
                case "register":
                    if(split.length < 4) stamp(1, "parametri sbagliati, provare comando help");
                    else{
                        String[] tag = new String[5];

                        System.arraycopy(split, 3, tag, 0, Math.min(3 + 5, split.length) - 3);
                        try {
                            register(split[1], tag, split[2]);
                        } catch (Exception e) {
                            stamp(1, "errore servizio di registrazione");
                        }
                    }
                break;
                case "login":
                    if(split.length < 3) stamp(1, "parametri sbagliati, provare comando help");
                    else{
                        login(split[1], split[2]);
                    }
                break;
                case "logout" :
                    logout();
                break;
                case "list"://user following etc
                    if(split.length < 2) stamp(1, "comando non riconosciuto, provare comando help");
                    else{
                        switch (split[1]){
                            case "users": listUser(); break;
                            case "followers": listFollower(); break;
                            case "following": listFollowing(); break;
                            default: stamp(1, "comando non riconosciuto, provare comando help"); break;
                        }
                    }
                break;
                case "follow":
                    if(split.length < 2) stamp(1, "parametri sbagliati, provare comando help");
                    else{
                        followUser(split[1]);
                    }
                break;
                case "unfollow":
                    if(split.length < 2) stamp(1, "parametri sbagliati, provare comando help");
                    else{
                        unfollowUser(split[1]);
                    }
                break;
                case "blog":
                    viewBlog();
                break;
                case "post":
                    if(split.length < 3) stamp(1, "parametri sbagliati, provare comando help");
                    else{
                        StringBuilder cont = new StringBuilder(split[2]);
                        for(int i = 3; i<split.length; i++) cont.append(" ").append(split[i]);
                        createPost(split[1], cont.toString());
                    }
                break;
                case "show": //feed, post...
                    if(split.length < 2) stamp(1, "comando non riconosciuto, provare comando help");
                    else{
                        switch (split[1]){
                            case "feed": showFeed(); break;
                            case "post":
                                if(split.length < 3) stamp(1, "parametri sbagliati, provare comando help");
                                else {
                                    try{
                                        showPost(Long.parseLong(split[2]));
                                    }catch (Exception e){
                                        stamp(1, "parametri sbagliati, provare comando help");
                                    }
                                }
                            break;
                            default: stamp(1, "comando non riconosciuto, provare comando help"); break;
                        }
                    }
                break;
                case "delete":
                    if(split.length < 2) stamp(1, "parametri sbagliati, provare comando help");
                    else{
                        try{
                            deletePost(Long.parseLong(split[1]));
                        }catch (Exception e){
                            stamp(1, "parametri sbagliati, provare comando help");
                        }
                    }
                break;
                case "rewin":
                    if(split.length < 2) stamp(1, "parametri sbagliati, provare comando help");
                    else{
                        try{
                            rewinPost(Long.parseLong(split[1]));
                        }catch (Exception e){
                            stamp(1, "parametri sbagliati, provare comando help");
                        }
                    }
                break;
                case "rate":
                    if(split.length < 3) stamp(1, "parametri sbagliati, provare comando help");
                    else{
                        try{
                            long a = Long.parseLong(split[1]);
                            int b = Integer.parseInt(split[2]);
                            if(b == -1 || b == 1) ratePost(a,b);
                            else throw new IllegalArgumentException();//parametri sbagliati
                        }catch (Exception e){
                            stamp(1, "parametri sbagliati, provare comando help");
                        }
                    }
                break;
                case "comment":
                    if(split.length < 3) stamp(1, "parametri sbagliati, provare comando help");
                    else{
                        try {
                            StringBuilder cont = new StringBuilder(split[2]);
                            for(int i = 3; i<split.length; i++) cont.append(" ").append(split[i]);
                            comment(Long.parseLong(split[1]), cont.toString());
                        }
                        catch (NumberFormatException e){
                            stamp(1, "parametri sbagliati, provare comando help");
                        }
                    }
                break;
                case "wallet"://btc e no
                    if(split.length == 1) getWallet();
                    else if(split[1].equals("btc")) getWalletBtc();
                    else stamp(1, "Comando non riconosciuto, provare comando help");
                break;
                case "help":
                    stamp(1, "Comandi accettati:\n" +
                        "register <username> <password> <tags>\n" +
                        "login <username> <password>\n" +
                        "logout\n" +
                        "list users\n" +
                        "list followers\n" +
                        "list following\n" +
                        "follow <username>\n" +
                        "unfollow <username>\n" +
                        "blog\n" +
                        "post <title> <content>\n" +
                        "show feed\n" +
                        "show post <id>\n" +
                        "delete <idPost>\n" +
                        "rewin <idPost>\n" +
                        "rate <idPost> <vote>\n" +
                        "comment <idPost> <comment>\n" +
                        "wallet\n" +
                        "wallet btc\n" +
                        "close"); break;
                default: stamp(1, "Comando non riconosciuto, provare comando help"); break;
            }
        }
    }

    //register con rmi
    private void register(String nomeUser, String[] tags, String password) throws RemoteException, NotBoundException{
        Registry r = LocateRegistry.getRegistry(addressReg, regPort);
        Remote remoteObject = r.lookup(regName);
        SRegisterService serverObject = (SRegisterService) remoteObject;

        try {
            serverObject.register(nomeUser, tags, password);
            stamp(1,"Utente registrato");
        } catch (UserAlreadyExistException e) {
            //messagio errore
            stamp(1,"Utente già registrato");
        }
    }

    //instaurazione connessione tcp e login, registrazione eccetera
    private void login(String name, String password){
        try {
            //login già effettuato, connessione già aperta
            if(serverTcpS != null && serverTcpS.isConnected()){
                stamp(1, "Utente già connesso, effettuare prima il logout");
                return;
            }

            //instaurazione connessione tcp
            serverTcpS = new Socket();
            serverTcpS.connect(new InetSocketAddress(addressServer, tcpPort), timeout);

            in = serverTcpS.getInputStream();
            out = serverTcpS.getOutputStream();

            //fase di login efettiva, post registrazione
            out.write(("0|" + name + "|" + password + "|~").getBytes());

            //legge la risposta
            String mess = read(in);
            stamp(2, mess);

            String[] messS = mess.split(Pattern.quote("|"));

            if(messS[0].equals("0")){
                this.name = name;

                //ricezione esttremi per le notifiche multicast
                //fase di login efettiva, post registrazione
                out.write(("17|~").getBytes());

                //legge la risposta
                mess = read(in);
                stamp(2, mess);
                messS = mess.split(Pattern.quote("|"));

                //inizio ascolto notifiche relative agli aggiornamenti portafogli
                CPackReceiver task = new CPackReceiver(followNotication);
                multS = task.open(messS[1], Integer.parseInt(messS[2]));
                notWallet = new Thread(task);
                notWallet.setDaemon(true);
                notWallet.start();

                //inizio instaurazione comunicazione via callback per i follower
                Registry registry = LocateRegistry.getRegistry(addressFollow, followPort);
                serverObj = (SNotifyFollow) registry.lookup(followName);

                //registrazione per il callback
                CNotifyFollow callbackObj = new CNotifyFollowImpl(follower, followNotication);
                CNotifyFollow stub = (CNotifyFollow) UnicastRemoteObject.exportObject(callbackObj, 0);
                serverObj.registerForCallback(stub, name);


                //sincronizzazione lista follow
                //fase di login efettiva, post registrazione
                out.write(("16|~").getBytes());//getFollowList

                String[] userNames = read(in).split(Pattern.quote("|"));
                if(userNames[0].equals("0")){
                    follower.addAll(Arrays.asList(userNames).subList(1, userNames.length));
                }else{
                    throw new Exception();//errore
                }

                stamp(1, "Login effettuato con successo!");
            } else{
                stamp(1, "Password o nome utente errati");
                chiusuraTcp();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            stamp(1, "errore connessione");
            chiusuraTcp();
        }
    }
    //logout
    private void logout(){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1, "Effettuare prima il login");
            return;
        }
        try {
            out.write(("1|~").getBytes());

            chiusuraTcp();
            stamp(1, "Logout effettuato");
        } catch (IOException e) {
            stamp(1, "Chiusura della connessione a seguito di un errore, esegui");
        }
    }

    //stampa a schermo la lista user ricevuta
    private void listUser() {
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1, "Effettuare prima il login");
            return;
        }

        try {
            //richiesta della lista
            out.write(("2|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String mess = read(in);
        stamp(2, mess);
        String[] userNames = mess.split(Pattern.quote("|"));
        if(userNames[0].equals("0")){
            if(userNames.length > 1){
                stamp(0, "Utenti suggeriti: ");
                for(int i = 1; i<userNames.length; i++)
                    stamp(0, userNames[i]);
                stamp(1, "");
            }
            else stamp(1, "Nessun utente suggerito");
        }else{
            stamp(1,"Errore");//non dovrebbe accadere
            chiusuraTcp();
        }

    }

    //list following
    private void listFollowing(){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            //richiesta della lista
            out.write(("3|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String mess = read(in);
        stamp(2, mess);
        String[] userNames = mess.split(Pattern.quote("|"));
        if(userNames[0].equals("0")){
            if(userNames.length > 1){
                stamp(0,"Utenti seguiti: ");
                for(int i = 1; i<userNames.length; i++)
                    stamp(0,userNames[i]);
                stamp(1,"");
            }
            else stamp(1,"Nessun utente seguito, per ora!");
        }else{
            stamp(1,"Errore");
            chiusuraTcp();
        }
    }

    //list follower
    private void listFollower(){
        if(!follower.isEmpty()){
            stamp(0,"Utenti che ti seguono:");
            for (String s : follower) stamp(0,s);
            stamp(1,"");
        }else{
            stamp(1,"Non sei seguito da nessun utente, per ora!");
        }
    }

    //follow
    private void followUser(String user){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            //richiesta della lista
            out.write(("4|" + user + "|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //legge la risposta
        String mess = read(in);
        stamp(2, mess);
        String[] messS = mess.split(Pattern.quote("|"));

        switch (messS[0]) {
            case "0":  stamp(1,"Ora segui: " + user + "!"); break;
            case "1": stamp(1,"Utente già seguito"); break;
            case "3": stamp(1,"Utente non trovato"); break;
            case "4": stamp(1,"Operazione non permessa"); break;
            default:  stamp(1,"errore sconosciuto"); //non dovrebbe accadere se il client funziona bene
        }
    }
    private void unfollowUser(String user){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            //richiesta della lista
            out.write(("5|" + user + "|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //legge la risposta
        String mess = read(in);
        stamp(2, mess);
        String[] messS = mess.split(Pattern.quote("|"));

        switch (messS[0]) {
            case "0": stamp(1,"Ora non segui più: " + user + "!"); break;
            case "1": stamp(1,"Utente non seguito"); break;
            case "3": stamp(1,"Utente non trovato"); break;
            case "4": stamp(1,"Operazione non permessa"); break;
            default: stamp(1,"errore sconosciuto"); //non dovrebbe accadere se il client funziona bene
        }
    }

    //blog
    private void viewBlog(){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            //richiesta del blog
            out.write(("6|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        PostToken dummyP;
        Gson gson = new Gson();

        String mess = read(in);
        stamp(2, mess);
        String[] messS = mess.split(Pattern.quote("|"));
        if(messS[0].equals("0")){
            if(messS.length <= 1){
                stamp(1,"Blog vuoto");
            }else{
                stamp(0,"Post presenti nel blog:");
                for(int i = 1; i<messS.length; i++){
                    dummyP = gson.fromJson(messS[i], PostToken.class);
                    stamp(0,dummyP.prettyPrint());
                }
                stamp(1,"");
            }
        }else stamp(1,"Errore sconosciuto");
    }

    //operazioni su post
    private void createPost(String title, String content){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            out.write(("7|" + title + "|" + content + "|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //legge la risposta
        String mess = read(in);
        stamp(2,mess);

        String[] messS = mess.split(Pattern.quote("|"));

        switch (messS[0]) {
            case "0": stamp(1,"Post creato!"); break;
            case "1": stamp(1,"Dimensione campi inadeguata"); break;
            default: stamp(1,"Errore sconosciuto");
        }
    }
    private void rewinPost(long id){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            out.write(("8|" + id + "|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //legge la risposta
        String mess = read(in);
        stamp(2,mess);

        String[] messS = mess.split(Pattern.quote("|"));

        switch (messS[0]) {
            case "0": stamp(1,"Post creato!"); break;
            case "1": stamp(1,"Utente non trovato"); break;
            case "2": stamp(1,"Post non trovato"); break;
            case "3":  stamp(1,"Operazione non permessa"); break;
            default: stamp(1,"Errore sconosciuto"); //non dovrebbe accadere se il client funziona bene
        }
    }
    private void deletePost(long id){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            out.write(("9|" + id + "|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //legge la risposta
        String mess = read(in);
        stamp(2,mess);

        String[] messS = mess.split(Pattern.quote("|"));

        switch (messS[0]) {
            case "0": stamp(1,"Post eliminato!"); break;
            case "1": stamp(1,"Operazione non permessa"); break;
            case "2": stamp(1,"Post inesistente"); break;
            default:  stamp(1,"Errore sconosciuto"); //non dovrebbe accadere se il client funziona bene
        }
    }
    private void ratePost(long id, int vote){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            out.write(("10|" + id + "|" + vote + "|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //legge la risposta
        String mess = read(in);
        stamp(2,mess);

        String[] messS = mess.split(Pattern.quote("|"));

        switch (messS[0]) {
            case "0": stamp(1,"Voto assegnato (" + vote + ")"); break;
            case "2": stamp(1,"Post inesistente"); break;
            case "3":  stamp(1,"Operazione non permessa"); break;
            default: stamp(1,"Errore sconosciuto"); //non dovrebbe accadere se il client funziona bene
        }
    }

    //feed
    private void showFeed(){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }
        Gson g = new Gson();
        //costruzione messaggio
        StringBuilder str = new StringBuilder("11|");
        str.append(g.toJson(feedDate)).append("|~");

        try {
            //richiesta
            out.write(str.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String mess = read(in);
        stamp(2,mess);

        String[] messS = mess.split(Pattern.quote("|"));
        if(messS[0].equals("0")){
            //setta la data di sincronizzazione
            feedDate = Calendar.getInstance();
            feed = new ArrayList<>();
            //costruisce il feed (funziona anche se vuoto)
            for(int i = 1; i<messS.length; i++)  feed.add(g.fromJson(messS[i], PostToken.class));
        }else if(!messS[0].equals("1")){ //se il server non risponde o 0 o 1 che sono codici positivi in questo caso
            stamp(1,"errore sconosciuto"); //??????
            resetFeed();
            return;
        }

        if(feed.size() >= 1){
            stamp(0,"Post presenti nel feed:");

            for (PostToken p : feed) stamp(0,p.prettyPrint());
            stamp(1, "");
        }else{
            stamp(1,"Feed vuoto");
        }
    }

    //leggere il post
    private void showPost(long postId){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }
        Gson g = new Gson();

        try {
            //richiesta
            out.write(("12|" + postId + "|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace(); //serve return pazzerello?
        }

        String mess = read(in);
        stamp(2,mess);

        String[] messS = mess.split(Pattern.quote("|"));

        switch (messS[0]) {
            case "0": stamp(1,g.fromJson(messS[1], Post.class).prettyPrint()); break;//stampa il post
            case "1": stamp(1,"Post inesistente"); break;
            default: stamp(1,"Errore sconosciuto"); ///????????
        }
    }

    //commenti
    private void comment(long postId, String content){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            out.write(("13|" + postId + "|" + content + "|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //legge la risposta
        String mess = read(in);
        stamp(2,mess);

        String[] messS = mess.split(Pattern.quote("|"));

        switch (messS[0]) {
            case "0": stamp(1,"Commento creato!"); break;
            case "1": stamp(1,"Post inesistente"); break;
            case "2": stamp(1,"Operazione non permessa"); break;
            default:  stamp(1,"Errore sconosciuto");
        }
    }

    //getWallet
    private void getWallet(){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            //richiesta del blog
            out.write(("14|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        WalletTransaction dummyT;
        double value;

        Gson gson = new Gson();
        String mess = read(in);
        stamp(2,mess);

        String[] messS = mess.split(Pattern.quote("|"));
        value = Double.parseDouble(messS[1]);//valore port

        if(messS[0].equals("0")){

            stamp(0,"Bilancio: " + value);
            if(messS.length > 2){
                stamp(0,"Transazioni:");
                for(int i = 2; i<messS.length; i++){
                    dummyT = gson.fromJson(messS[i], WalletTransaction.class);
                    stamp(0,dummyT.prettyPrint());
                }
                stamp(1, "");
            }
            else{
                stamp(1,"Ancora nessuna transazione");
            }
        }else stamp(1,"Errore sconosciuto"); //??
    }
    //getWalletBtc
    private void getWalletBtc(){
        //se la connessione è aperta
        if(serverTcpS == null || serverTcpS.isClosed()){
            stamp(1,"Effettuare prima il login");
            return;
        }

        try {
            //richiesta del blog
            out.write(("15|~").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }


        String mess = read(in);
        stamp(2,mess);

        String[] messS = mess.split(Pattern.quote("|"));
        double value = Double.parseDouble(messS[1]);//valore port
        switch (messS[0]) {
            case "0": stamp(1,"Bilancio: " + value); break;
            default: stamp(1,"Errore sconosciuto"); ///????????
        }
    }


    //resetta la data della chace e la chace
    private void resetFeed(){
        feed = new ArrayList<>();
        feedDate.setTime(new Date(0));
    }
    //resetta la lista follower memorizzata
    private void resetFollow(){
        follower = new ArrayList<>();
        followNotication.clear();
    }

    //procedura di chiusura connessione corretta
    private void chiusuraTcp(){
        if(serverTcpS == null) return;

        if(serverObj != null) {
            try {
                serverObj.unregisterForCallback(name);
            } catch (RemoteException ignored) {
            }
        }
        if(notWallet != null && notWallet.isAlive()){
            notWallet.interrupt();//non termina il metodo di attesa pacchetto
            multS.close();
        }

        try {
            serverTcpS.close();
        } catch (IOException ignored) {
        }
        serverTcpS = null;
        in = null;
        out = null;

        resetFeed();
    }
    //read dal socket bloccante
    private String read(InputStream in){
        Scanner s = new Scanner(in).useDelimiter("~");
        return s.next();
    }

    //lettura file config
    public void parseConfig(String f){
        Scanner inc;

        try {
            File file = new File(f);
            if(!file.exists()) throw new IllegalArgumentException();
            inc = new Scanner(file);
        } catch (Exception e) {
            stamp(0,"Errore lettura file di configurazione, avvio con parametri standard");
            return;
        }

        int i = 1;

        while(inc.hasNextLine()){
            String line = inc.nextLine();
            if(line.length() == 0 || line.charAt(0) == '#'){
                i++;
                continue;
            }
            try {
                String[] arg = line.replace(" ", "").split("=");
                if(arg.length != 2) throw new IllegalArgumentException();
                switch (arg[0]) {
                    case "serverAddress":  addressServer = arg[1]; break;
                    case "serverTCPPort": tcpPort = Integer.parseInt(arg[1]); break;

                    case "registerServiceAddress": addressReg = arg[1]; break;
                    case "registerServicePort": regPort = Integer.parseInt(arg[1]); break;
                    case "registerServiceName": regName = arg[1]; break;

                    case "notifyFollowServiceAddress": addressFollow = arg[1]; break;
                    case "notifyFollowServicePort": followPort = Integer.parseInt(arg[1]); break;
                    case "notifyFollowServiceName": followName = arg[1]; break;

                    case "timeout": timeout = Integer.parseInt(arg[1]); break;

                    case "debug":
                        if(arg[1].equals("true") || arg[1].equals("TRUE") || arg[1].equals("True") || arg[1].equals("1")) debug = true;
                        else if(arg[1].equals("false") || arg[1].equals("FALSE") || arg[1].equals("False") || arg[1].equals("0")) debug = false;
                        else throw new IllegalArgumentException();
                        break;

                    default: stamp(0,"parametro \"" + arg[0] + "\" non riconosciuto");

                }
            }catch (Exception e){
                stamp(0,"errore linea " + i);
            }
            i++;
        }
    }

    //stampa a schermo
    private void stamp(int action, String s){
        if(action == 1){
            if(!s.isEmpty()) System.out.println(s);
            while(serverTcpS != null && !serverTcpS.isClosed() && !followNotication.isEmpty()){
                System.out.println(followNotication.poll().prettyPrint());
            }

            System.out.println("####################################################################");
        }
        else if(action == 2){
            if(debug) System.out.println("#" + s + "#");
        }
        else if(action == 0){
            if(!(s.length() == 0)) System.out.println(s);
        }
    }
}
