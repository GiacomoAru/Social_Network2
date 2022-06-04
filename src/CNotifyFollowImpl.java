import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Deque;


public class CNotifyFollowImpl implements CNotifyFollow{

    private final ArrayList<String> follower;
    private Deque<CNotification> list;

    public CNotifyFollowImpl(ArrayList<String> follower, Deque<CNotification> notList){
        super();
        this.follower = follower;
        this.list = notList;
    }

    @Override
    public void notifyF(String userName, int action) throws RemoteException {
        synchronized (follower){
            if(action >= 0 && !follower.contains(userName)) {
                follower.add(userName);
                //modifichiamo la data di sincronizzazione per la notifica
                list.add(new CNotification(0,userName, action));
            }
            else if(follower.contains(userName)){
                follower.remove(userName);
                //modifichiamo la data di sincronizzazione per la notifica
                list.add(new CNotification(0,userName, action));
            }
        }
    }
}
