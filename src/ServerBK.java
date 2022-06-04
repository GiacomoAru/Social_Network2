import SocialNetworkClasses.SocialNetworkInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.HashMap;


public class ServerBK extends Thread {

    private final SocialNetworkInterface social;
    private final HashMap<String, SHPsw> ru;
    private int pause;

    private final String mainDir;
    private final String nameD = "\\statoSocial";
    private final String pwName = "\\pswFile.txt";
    private final String sDir = "\\social";
    private int counter = 0;
    private final int maxBK;

    public ServerBK(int pause, SocialNetworkInterface social, HashMap<String, SHPsw> ru, String path, int backupMax) {
        this.social = social;
        this.ru = ru;
        this.pause = pause;
        this.maxBK = backupMax;

        mainDir = path;
    }

    @Override
    public void run() {
        int i = 0;
        while(!Thread.currentThread().isInterrupted()) {
            //pausa
            try {
                Thread.sleep(pause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ServerMain.stamp("bk chiusura...");
                continue;
            }

            saveState();
        }
    }

    //ripristina lo stato della hashmap dei registrati e social
    public void recState(){
        recState(0);
    }
    public void recState(int n){
        ServerMain.stamp("Inizio ripristino dello stato...");

        String attuale = mainDir + nameD + n;
        File f = new File(attuale);
        if(!f.exists()){
            return;
        }

        synchronized (ru) {
            f = new File(attuale + pwName);
            if (f.exists()) {
                try (FileInputStream in = new FileInputStream(f)) {
                    //creiamo l'oggetto per deserializzare con lo stream
                    JsonReader reader = new JsonReader(new InputStreamReader(in));
                    //inizia l'array di oggetti
                    reader.beginArray();
                    while (reader.hasNext()) {
                        //deserializziamo
                        SHPsw d = new Gson().fromJson(reader, SHPsw.class);
                        //non serve la lock perchè siamo in fase di start
                        ru.putIfAbsent(d.user, d);
                    }
                    reader.endArray();
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                social.deserialize(attuale + sDir);
            }
        }
        ServerMain.stamp("Fine ripristino dello stato");
    }

    //salva lo stato della hashmap dei registrati e social
    public void saveState(){
        int dummy = counter;
        if(counter == maxBK - 1){
            counter = 0;
        }else{
            counter++;
        }
        saveState(dummy);
    }
    public void saveState(int n){
        ServerMain.stamp("Inizio Bakcup... [" + n + "]");

        String attuale = mainDir + nameD + n;
        File f = new File(attuale);
        if(!f.exists()){
            f.mkdir();
            f = new File(attuale + sDir);
            f.mkdir();
        }

        synchronized (ru){
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(attuale + pwName))) {
                boolean first = true;
                out.write("[");
                for (SHPsw u : ru.values()) {
                    if(first) first = false;
                    else  out.write(",");
                    out.write(gson.toJson(u));
                }
                out.write("]");
            }catch (IOException e) {
                e.printStackTrace();
                return;
            }
            social.serialize(attuale + sDir);
        }
        ServerMain.stamp("Fine Bakcup [" + n + "]");
    }
}
