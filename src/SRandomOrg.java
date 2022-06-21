import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Random;
import java.util.Scanner;

public class SRandomOrg {

    /**
     * @param precisione: la precisione del numero decimale (max 9)
     * @return un numero decimale random tra 0 e 1
     */
    public static double getDouble(int precisione){
        HttpURLConnection connection = null;
        double ret = 0;
        try {
            URL url = new URL("https://www.random.org/integers/?num=1&min=1&max="+ (int) Math.pow(10, precisione) +"&col=1&base=10&format=plain&rnd=new");

            //invia la richiesta http
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            //la risposta è una linea contenente un numero
            InputStream is = connection.getInputStream();
            Scanner rd = new Scanner(new InputStreamReader(is));
            int out = rd.nextInt();
            rd.close();

            ret = (double)out/Math.pow(10, precisione);


            ServerMain.stamp("Numero ricevuto " + (double)out/Math.pow(10, precisione));

        } catch (ProtocolException | MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e){

            //se non va a buon fine per errori di rete

            ServerMain.stamp("ERRORE DI RETE!");
            ServerMain.stamp(e.getMessage());
            ServerMain.stamp("GENERAZIONE NUMERO AUTOMATICA");

            Random r = new Random();
            ret = r.nextDouble();//niente precisione

        } finally{
            if (connection != null) connection.disconnect();
        }

        return ret;
    }

    /**
     * @param lenght: la lunghezza della stringa (max 20)
     * @return un numero decimale random tra 0 e 1
     */
    public static String getString(int lenght){
        HttpURLConnection connection = null;
        String ret = null;

        try {
            URL url = new URL("https://www.random.org/strings/?num=1&len="+ lenght +"&digits=on&upperalpha=on&loweralpha=on&unique=on&format=plain&rnd=new");
            //invia la richiesta http
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            //la risposta è una linea contenente una stringa random
            InputStream is = connection.getInputStream();
            Scanner rd = new Scanner(new InputStreamReader(is));
            ret = rd.nextLine();
            rd.close();

            ServerMain.stamp("stringa ricevuta: " + ret);

        } catch (ProtocolException | MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            //se non va a buon fine per errori di rete

            ServerMain.stamp("ERRORE DI RETE!");
            ServerMain.stamp(e.getMessage());
            ServerMain.stamp("GENERAZIONE STRINGA AUTOMATICA");

            Random r = new Random();

            //dovrebbe essere infallibile, credo

            ret = SHash.bytesToHex(SHash.sha256(String.valueOf(r.nextLong())));
            ret = ret.substring(0, lenght);


        }finally {
            if (connection != null) connection.disconnect();
        }

        return ret;
    }

}
