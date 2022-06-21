import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SNotifyFollow extends Remote {

    /**
     *	Metodo per registrarsi al servizio di notifica dei followers
     *	@param client oggetto per notificare la modifica della lista al client
     *  @param name nome utente corrispondente all'utente che ha effettuato il login nel client
     */
    public void registerForCallback (CNotifyFollow client, String name) throws RemoteException;
    /**
     *	Metodo per eliminare la registrazione al servizio di notifica dei followers
     *  @param name nome utente corrispondente all'utente che ha effettuato il login nel client
     */
    public void unregisterForCallback (String name) throws RemoteException;
}
