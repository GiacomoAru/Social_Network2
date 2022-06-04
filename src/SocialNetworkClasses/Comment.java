package SocialNetworkClasses;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.Calendar;

//i commenti ai post sono un entità distinta dai post stessi
public class Comment implements Comparable<Comment>, Serializable {

    //autore
    @Expose
    public final String author;
    //contenuto (max 200 caratteri)
    @Expose
    public final String content;
    //il post di cui è commento
    @Expose
    public final long reference;
    //id univoco per ogni commento
    @Expose
    public final long id;
    //data creazione
    @Expose
    public final Calendar date;

    public Comment(String author, String text, long reference, long id) throws IllegalArgumentException{
        if(text == null || text.length() > 200) throw new IllegalArgumentException("text");
        if(author == null) throw new IllegalArgumentException("author");
        this.author = author;
        this.content = text;
        this.reference = reference;

        this.id = id;
        date = Calendar.getInstance();
    }

    //compare corretto senza errori che si basa sull'univocità dell'id
    @Override
    public int compareTo(Comment o) {
        long ret = this.id - o.id;
        try {
            return Math.toIntExact(ret);
        }catch(ArithmeticException e){
            if(ret > 0) return Integer.MAX_VALUE;
            else return Integer.MIN_VALUE;
        }
    }

    @Override
    public String toString() {
        return "SocialNetworkClasses.SComment{" +
                "author='" + author + '\'' +
                ", content='" + content + '\'' +
                ", reference=" + reference +
                ", id=" + id +
                ", date=" + SocialNetwork.df.format(date.getTime()) +
                '}';
    }

    public String prettyPrint(){
        return "(" + id + ")\tA:" + author + "\n\tC:" + content;
    }
}
