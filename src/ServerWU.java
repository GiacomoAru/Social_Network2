import SocialNetworkClasses.SocialNetworkInterface;

import java.io.IOException;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ServerWU extends Thread {

    private final SocialNetworkInterface social;
    private int pause;
    private final int port;
    private final String address;

    public ServerWU(int pause, SocialNetworkInterface social, String address, int port) {
        this.social = social;
        this.pause = pause;
        this.port = port;
        this.address = address;
    }

    @Override
    public void run() {

        byte[] data;
        DateFormat df = new SimpleDateFormat("(dd/MM/YYYY hh:mm:ss:SSS)");
        DatagramSocket ms;
        InetAddress ia;

        try {
            ia = InetAddress.getByName(address);
            ms = new DatagramSocket();
            ms.setReuseAddress(true);
        } catch (UnknownHostException | SocketException e) {
            System.out.println("Errore ThreadWU");
            return;
        }

        while (!Thread.currentThread().isInterrupted()) {
            //pausa
            try {
                Thread.sleep(pause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ServerMain.stamp("WU chiusura...");
                continue;
            }

            ServerMain.stamp("Aggiornamento periodico portafogli in corso...");
            social.updateWallet(); //update portafoglio
            ServerMain.stamp("Aggiornamento portafogli terminato");


            data = df.format(Calendar.getInstance().getTime()).getBytes();
            DatagramPacket dp = new DatagramPacket(data, data.length, ia, port);

            try {
                ms.send(dp);
            } catch (IOException e) {
                ServerMain.stamp("Invio notifica di aggiornamento portafogli");
            }
        }
    }
}