package SocialNetworkClasses;

import com.google.gson.annotations.Expose;

import java.util.*;

public class PostRewin extends Post {


    private Post reference;
    @Expose
    private long idRef;

    public PostRewin(String author, long id, Post reference, long idRef) throws IllegalArgumentException {
        super("", "", author, id, Calendar.getInstance(), null, true);


        this.reference = reference;
        this.idRef = idRef;
    }

    public synchronized long getReference(){
        return idRef;
    }

    //aggiunte non aggiungono duplicati in automatico
    public synchronized void addComment(Comment c){
        reference.addComment(c);
    }
    //ritorna una vista dui commenti
    public synchronized SortedSet<Comment> getComments(){
        return reference.getComments();
    }

    //getter
    public synchronized String getTitle() {
        return reference.getTitle();
    }
    public synchronized String getContent() {
        return reference.getContent();
    }
    public synchronized Calendar getDate(){ return reference.getDate();}

    public synchronized long getUpVoteN() {
        return reference.getUpVoteN();
    }
    public synchronized long getDownVoteN() {
        return reference.getDownVoteN();
    }

    public synchronized int getNComments(){
        return reference.getNComments();
    }

    //da tenere a mente che lo stato del post può variare dopo che questo metodo si è risolto
    public synchronized List<Long> getAllComment(){
        return reference.getAllComment();
    }

    public int getUpVote() {
        return reference.getUpVote();
    }
    public int getDownVote() {
        return reference.getDownVote();
    }
    public TreeSet<String> getUserWhoVote() {
        return reference.getUserWhoVote();
    }
    public TreeSet<String> getUpVoteCache() {
        return reference.getUpVoteCache();
    }
    public TreeSet<String> getDownVoteCache() {
        return reference.getDownVoteCache();
    }
    public int getIterNumber() {
        return reference.getIterNumber();
    }
    public TreeSet<String> getCommentAuthorCache() {
        return reference.getCommentAuthorCache();
    }

    public String getOriginalAuthor(){
        return reference.getOriginalAuthor();
    }

    public synchronized boolean newVote(String user, int vote) {
        return reference.newVote(user, vote);
    }

    public synchronized void addIterNumber(){reference.addIterNumber();}

    public synchronized void clearCache(){
        //??
    }

    public synchronized Post copy(){
        Post p = reference.copy();
        //attenzione
        p.id = this.id;
        p.rewin = true;
        return p;
    }

    //metodo da usare dopo la deserializzazione per inizializzare alcune variabili
    public synchronized void recoveryState(Post p){
        reference = p;
        recoveryState();
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
                "author='" + getAuthor() + '\'' +
                ", id=" + id +
                ", rewin=" + rewin +
                ", referenceId=" + idRef +
                ", date=" + SocialNetwork.df.format(getDate().getTime()) +
                '}';
    }

    public synchronized String prettyPrint(){
        return this.copy().prettyPrint();
    }
}
