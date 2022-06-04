package SocialNetworkClasses;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.*;

//oggetto sincronizzato con le synchronized, abbastanza piccolo da accettare una lock generale
public class Post implements Comparable<Post>, Serializable {

    //titolo del post (max 20 caratteri)
    @Expose
    private String title;
    //contenuto del post (max 500 caratteri)
    @Expose
    private String content;
    //atutore del post
    @Expose
    private final String author;
    //id del post (univoco e > 0, all'interno del social)
    @Expose
    protected long id;

    //data creazione
    @Expose
    private final Calendar date;

    //upvote e downvote del post
    @Expose
    private int upVote = 0;
    @Expose
    private int downVote = 0;
    @Expose
    private final TreeSet<String> userWhoVote;
    //upvote e downvote recenti, dopo l'ultimo calcolo ricompense
    @Expose
    private TreeSet<String> upVoteCache;
    @Expose
    private TreeSet<String> downVoteCache;
    @Expose
    private int iterNumber = 1;

    //maggior parte delle operazioni utili in O(logn), compresa eliminazione e contains
    private TreeSet<Comment> comments;
    @Expose
    private TreeSet<String> commentAuthorCache;

    //rewin
    @Expose
    protected boolean rewin;

    //costruttore per copiare un post
    protected Post(String title, String content, String author, long id, Calendar date, TreeSet<Comment> comments, boolean rewin)
            throws IllegalArgumentException{
        if(title == null || title.length() > 20) throw new IllegalArgumentException("title");
        if(content == null || content.length() > 500) throw new IllegalArgumentException("content");
        if(author == null) throw new IllegalArgumentException("author");

        this.title = title;
        this.content = content;
        this.author = author;
        this.rewin = rewin;

        this.comments = comments;
        userWhoVote = new TreeSet<>();
        upVoteCache = new TreeSet<>();
        downVoteCache = new TreeSet<>();
        commentAuthorCache = new TreeSet<>();
        //aggiungiamo l'autore così da semplificare alcuni controlli al social

        //assegnamo id
        this.id = id;
        this.date = date;
    }
    //costruttore standard
    public Post(String title, String content, String author, long id) throws IllegalArgumentException{
        this(title,content, author, id, Calendar.getInstance(), new TreeSet<>(), false);
    }

    //aggiunte non aggiungono duplicati in automatico
    public synchronized void addComment(Comment c){
        comments.add(c);
        commentAuthorCache.add(c.author);
    }
    //ritorna una vista dui commenti
    public synchronized SortedSet<Comment> getComments(){
        return Collections.unmodifiableSortedSet(comments);
    }

    //getter
    public synchronized String getTitle() {
        return title;
    }
    public synchronized String getContent() {
        return content;
    }
    public synchronized Calendar getDate(){ return (Calendar) date.clone();}

    public synchronized long getUpVoteN() {
        return upVote;
    }
    public synchronized long getDownVoteN() {
        return downVote;
    }

    public synchronized int getNComments(){
        return comments.size();
    }

    //da tenere a mente che lo stato del post può variare dopo che questo metodo si è risolto
    public synchronized List<Long> getAllComment(){
        ArrayList<Long> ret = new ArrayList<>();
        for (Comment c :
                comments) {
            ret.add(c.id);
        }
        return ret;
    }

    public int getUpVote() {
        return upVote;
    }
    public int getDownVote() {
        return downVote;
    }
    public TreeSet<String> getUserWhoVote() {
        return userWhoVote;
    }
    public TreeSet<String> getUpVoteCache() {
        return upVoteCache;
    }
    public TreeSet<String> getDownVoteCache() {
        return downVoteCache;
    }
    public TreeSet<String> getCommentAuthorCache() {
        return commentAuthorCache;
    }

    public String getAuthor(){
        return this.author;
    }
    public String getOriginalAuthor(){
        return getAuthor();
    }

    public synchronized boolean newVote(String user, int vote) {
        if(userWhoVote.add(user)){
            if(vote > 0){ upVote++; upVoteCache.add(user);}
            else if(vote < 0){ downVote++; downVoteCache.add(user);}
            else return false;

            return true;
        }
        return false;
    }
    private synchronized void setUpVote(int l){upVote = l;}
    private synchronized void setDownVote(int l){downVote = l;}

    public int getIterNumber() {
        return iterNumber;
    }
    public synchronized void addIterNumber(){iterNumber++;}

    public synchronized void clearCache(){
        this.commentAuthorCache.clear();
        this.upVoteCache.clear();
        this.downVoteCache.clear();
    }

    //crea una copia del post,
    public synchronized Post copy(){
        Post ret = new Post(this.title, this.content, this.author, this.id, this.date, (TreeSet<Comment>) this.comments.clone(),
                this.rewin);

        for (String s :
                this.userWhoVote) {
            ret.newVote(s,1);
        }
        ret.setUpVote(this.upVote);
        ret.setDownVote(this.downVote);

        return ret;
    }

    //metodo da usare dopo la deserializzazione per inizializzare alcune variabili
    public synchronized void recoveryState(){
        this.comments = new TreeSet<>();
    }

    //compare corretto senza errori, si basa sull'univocità dell'id
    @Override
    public int compareTo(Post o) {
        long ret = this.id - o.id;
        try {
            return Math.toIntExact(ret);
        }catch(ArithmeticException e){
            if(ret > 0) return Integer.MAX_VALUE;
            else return Integer.MIN_VALUE;
        }
    }

    @Override
    public synchronized String toString() {
        return "SocialNetworkClasses.SPost{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", author='" + author + '\'' +
                ", id=" + id +
                ", rewin=" + rewin +
                ", iterN=" + iterNumber +
                ", date=" + SocialNetwork.df.format(date.getTime()) +
                ", upVote=" + upVote +
                ", downVote=" + downVote +
                ", comments=" + this.getNComments() +
                '}';
    }

    public synchronized String prettyPrint(){
        StringBuilder ret = new StringBuilder("(" + id );
        if(rewin) ret.append("*");
        ret.append(")\tA:").append(author);
        ret.append("\nT:").append(title);
        ret.append("\nC:").append(content).append("\nUpVote:").append(upVote).append(" DownVote:").append(downVote).append("\nComment:").append(getNComments());

        for (Comment c :
                comments) {
            ret.append("\n").append(c.prettyPrint());
        }
        return ret.toString();
    }

}
