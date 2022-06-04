import CustomException.UserAlreadyExistException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SRegisterService extends Remote {
    int register(String nomeUser, String[] tags, String password) throws RemoteException, UserAlreadyExistException;
}
