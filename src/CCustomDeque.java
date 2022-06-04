import java.util.concurrent.ConcurrentLinkedDeque;

public class CCustomDeque<E extends CNotification> extends ConcurrentLinkedDeque<E> {

    @Override
    public boolean add(E e) {
        if(e.type == 1){
            this.removeIf(elem -> elem.type == 1);
        }
        return super.add(e);
    }
}
