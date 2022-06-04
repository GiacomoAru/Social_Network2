import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Deque;

public class CPackReceiver implements Runnable{
    private String address;
    private int port;
    private final Deque<CNotification> q;
    InetAddress group = null;
    MulticastSocket ms = null;

    public CPackReceiver(Deque<CNotification> q){
        this.q = q;
    }

    public MulticastSocket open(String address, int port) throws IOException {
        this.address = address;
        this.port = port;

        group = InetAddress.getByName(address);
        ms = new MulticastSocket(port);
        ms.joinGroup(group);

        return ms;
    }

    @Override
    public void run() {
        try {

            byte[] buffer = new byte[8192];

            while(!Thread.currentThread().isInterrupted()){
                try{
                    //ricezione notifica multicast
                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                    ms.receive(dp);
                    //creazione nuova notifica
                    q.add(new CNotification(1, "", 0));

                }catch (Exception ignored){
                }
            }

        } catch (Exception e) {
            System.out.println("chiusura gruppo multicast");
        } finally {
            if (ms != null) {
                try {
                    ms.leaveGroup(group);
                    ms.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
