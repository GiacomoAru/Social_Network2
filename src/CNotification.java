import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

class CNotification{
    //0=notifica follow
    //1=notifica aggiornamento wallet
    public final int type;

    //descrizione della notifica (in caso type 0, nome user)
    public final String dex;

    //altra informazione utile in caso la notifica sia user
    public final int action;

    public final Calendar date = Calendar.getInstance();
    private DateFormat df = new SimpleDateFormat("(dd/MM/yyyy hh:mm:ss)");

    public CNotification(int type, String dex, int action){
        this.dex = dex;
        this.action = action;
        this.type = type;
    }

    public String prettyPrint(){
        switch (type) {
            case 0:
                StringBuilder ret = new StringBuilder("*" + dex);
                if(action >= 0) ret.append(" ha appena iniziato a seguirti " + df.format(date.getTime()) + "*");
                else ret.append(" ha appena smesso di seguirti " + df.format(date.getTime()) + "*");
                return ret.toString();
            case 1: return "*Portafoglio aggiornato " + df.format(date.getTime()) + "*";
            default: return toString();
        }
    }

    @Override
    public String toString() {
        return "CNotification{" +
                "type=" + type +
                ", dex='" + dex + '\'' +
                ", action=" + action +
                ", date=" + df.format(date.getTime()) +
                '}';
    }
}