import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CNotifyFollow extends Remote {

    /**
     *	Metodo per notificare un Client una modifica della lista followers
     *	@param userName nome dell'user che genera la modifica
     *  @param action tipologia di notifica (follow o unfollow)
     */
    public void notifyF(String userName, int action) throws RemoteException;
}
