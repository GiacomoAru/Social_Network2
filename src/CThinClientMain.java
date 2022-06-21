import java.net.URL;
import java.rmi.RMISecurityManager;
import java.rmi.server.RMIClassLoader;

public class CThinClientMain {

    public static void main(String[]Args){
        if (Args.length == 0) {
            System.err.println("Usage: java LoadClient <remote URL>");
            System.exit(-1);
        }
        try {
            //inizializzazione ed avvio client
            URL url = new URL(Args[0]);
            String configPath = null;
            if (Args.length >= 2) configPath = Args[1];
            System.out.println(configPath);
            Class<?> client_Class = RMIClassLoader.loadClass(url, "Client");
            Runnable client = (Runnable) client_Class.getDeclaredConstructor(String.class).newInstance(configPath);
            //avvio con run
            client.run();
            //terminazione
            System.exit(0);
        }
        catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }
}