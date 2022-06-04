package SocialNetworkClasses;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.Calendar;

public class WalletTransaction implements Serializable, Comparable<WalletTransaction> {

    //id transazione
    @Expose
    public final long id;
    //utente di riferimento per il portafoglio
    @Expose
    public final String user;
    //data corrispondente alla transazione
    @Expose
    public final Calendar date;

    //descrive la transazione in termini di cosa ha generato quanto della transazione
    //una transazione è fatta a seguito della valutazione di più post dell'utente
    @Expose
    public final String des;

    //valore della transazione in valuta del social
    @Expose
    public final double value;


    public WalletTransaction(Calendar date, String des, String user, double value, long id){
        this.date = date;
        this.des = des;
        this.user = user;
        this.value = value;
        this.id = id;
    }

    @Override
    public String toString() {
        return "SWalletTransaction{" +
                "id=" + id +
                ", user='" + user + '\'' +
                ", date=" + SocialNetwork.df.format(date.getTime()) +
                ", des='" + des + '\'' +
                ", value=" + value +
                '}';
    }
    @Override
    public int compareTo(WalletTransaction o) {
        long ret = this.id - o.id;
        try {
            return Math.toIntExact(ret);
        }catch(ArithmeticException e){
            if(ret > 0) return Integer.MAX_VALUE;
            else return Integer.MIN_VALUE;
        }
    }

    public String prettyPrint(){
        return "(" + id + ")\t" +
                "valore: " + value +
                " " + SocialNetwork.df.format(date.getTime()) +
                " [" + des + "]";
    }
}
