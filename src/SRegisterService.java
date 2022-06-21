import CustomException.UserAlreadyExistException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SRegisterService extends Remote {
    /**
     *	Metodo per registrare un nuovo utente all'interno del social
     *  @param nomeUser nome del nuovo utente
     *  @param tags elenco tag associati ad un utente
     *  @param password password del nuovo utente
     */
    int register(String nomeUser, String[] tags, String password) throws RemoteException, UserAlreadyExistException;
}
