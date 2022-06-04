import CustomException.UserAlreadyExistException;
import SocialNetworkClasses.SocialNetworkInterface;

import java.rmi.RemoteException;
import java.util.HashMap;

public class SRegisterServiceImp implements SRegisterService {

    private final HashMap<String, SHPsw> map;
    private final SocialNetworkInterface social;


    //il file deve esistere già, al massimo deve essere vuoto
    public SRegisterServiceImp(HashMap<String, SHPsw> map, SocialNetworkInterface social){
        super();
        this.map = map;
        this.social = social;
    }

    @Override
    public int register(String nomeUser, String[] tags, String password) throws RemoteException, UserAlreadyExistException {

        synchronized(map){
            //crea l'utente, se esiste già solleva un eccezione e quindi termina l'esecuzione
            //altrimenti continua la creazione modificando il file delle password
            social.createUser(nomeUser, tags);

            String seed = SRandomOrg.getString(16);
            password = password + seed;
            String hash;

            hash = SHash.bytesToHex(SHash.sha256(password));


            //se tutto è corretto non c'è modo che questa put trovi la entry già mappata
            map.putIfAbsent(nomeUser, new SHPsw(nomeUser, hash, seed));
        }

        return 0;
    }
}
