package SocialNetworkClasses;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class PostToken implements Comparable<PostToken>, Serializable {
    public static DateFormat df = new SimpleDateFormat("(dd/MM/yyyy hh:mm)");

    public final String postTitle;
    public final long postId;
    public final String postAuthor;
    private final Calendar date;
    public final boolean rewin;


    public PostToken(Post p) {
        this.postTitle = p.getTitle();
        this.postAuthor = p.getOriginalAuthor();
        this.postId = p.id;
        this.date = p.getDate();
        this.rewin = p.rewin;
    }

    public Calendar getDate(){return (Calendar) date.clone();}

    @Override
    public int compareTo(PostToken o) {
        return this.date.compareTo(o.date);
    }

    @Override
    public String toString() {
        return "SPostToken{" +
                "postTitle='" + postTitle + '\'' +
                ", postId=" + postId +
                ", postAuthor='" + postAuthor + '\'' +
                ", date=" + df.format(date.getTime()) +
                ", rewin=" + rewin +
                '}';
    }

    public String prettyPrint(){
        String append;
        if(rewin) append = "*";
        else append = "";

        return "(" + postId + append + ")\tA:" + postAuthor + " T:" + postTitle + " " + df.format(date.getTime());
    }
}
