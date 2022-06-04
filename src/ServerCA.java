import SocialNetworkClasses.SocialNetworkInterface;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

public class ServerCA implements Runnable{

    private final SocialNetworkInterface social;
    private final ExecutorService tPool;
    private final ServerSocket ss;
    private final HashMap<String, SHPsw> ru;
    private final SNotifyFollowImp snfi;
    private final String[] multicastAddress;

    public ServerCA(SocialNetworkInterface social, ServerSocket ss, ExecutorService tPool, HashMap<String, SHPsw> ru, SNotifyFollowImp snfi, String[] multicastAddress) {
        this.social = social;
        this.tPool = tPool;
        this.ss = ss;
        this.ru = ru;
        this.snfi = snfi;
        this.multicastAddress = multicastAddress;
    }

    @Override
    public void run() {
        Socket clientSocket;
        ServerMain.stamp("Client accepter avviato");

        while(!Thread.currentThread().isInterrupted()) {
            try {
                //in attesa di nuove connessioni: login
                //la close() terminerà il ciclo

                clientSocket = ss.accept();

                ServerMain.stamp("Client connesso -> " + clientSocket.getRemoteSocketAddress().toString());

                tPool.submit(new ServerRH(clientSocket, social, ru, snfi, multicastAddress));

            } catch (SocketException e) {
                ServerMain.stamp("Client accepter chiuso");
            }catch (IOException e) {
                ServerMain.stamp("errore accept");
                e.printStackTrace();
            }
        }
    }
}