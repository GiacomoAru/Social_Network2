import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.*;

public class SNotifyFollowImp extends RemoteObject implements SNotifyFollow {

    private Map<String, CNotifyFollow> clients;

    public SNotifyFollowImp()throws RemoteException {
        super();
        clients = new HashMap<>();
    }

    @Override
    public synchronized void registerForCallback(CNotifyFollow client, String name) throws RemoteException {
        if (!clients.containsKey(name)) {
            clients.put(name, client);
            ServerMain.stamp("Nuovo client registrato al callback");
        }
    }

    @Override
    public synchronized void unregisterForCallback(String name) throws RemoteException {
        if (clients.remove(name) != null) ServerMain.stamp("Client ha rimosso la registrazione al callback");
        else ServerMain.stamp("Errore callback");
    }

    public synchronized void notifyFollow(String user, String follow, int action) throws RemoteException {
        if(clients.containsKey(user)){
            clients.get(user).notifyF(follow, action);
        }
    }
}
