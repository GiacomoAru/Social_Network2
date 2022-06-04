import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SNotifyFollow extends Remote {

    public void registerForCallback (CNotifyFollow client, String name) throws RemoteException;
    public void unregisterForCallback (String name) throws RemoteException;
}
