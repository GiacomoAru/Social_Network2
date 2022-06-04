import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CNotifyFollow extends Remote {

    public void notifyF(String userName, int action) throws RemoteException;
}
