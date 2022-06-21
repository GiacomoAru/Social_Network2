package SocialNetworkClasses;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class User implements Comparable<User>, Serializable {

    //definisce univocamente lo stesso utente logico
    @Expose
    public final String name;
    //data di creazione di un utente, corrisponde abbastanza fedelmente alla data di registrazione di un utente
    @Expose
    public final Calendar date;
    //non tutti i tag != null, minimo 1 massimo 5
    @Expose
    private final String[] tags = new String[5];
    //data della ultima modifica all'insieme dei post creati
    @Expose
    private Calendar lastModify;

    //portafoglio
    @Expose
    private double wallet = 0;
    @Expose
    private final TreeSet<WalletTransaction> walletHistory;

    //follower: persone che seguono this
    //followed: persone seguite da this
    //post: post pubblicati da this
    //tutto sincronizzato per evitare problemi di concorrenza
    @Expose
    protected final TreeSet<String> followers;
    @Expose
    private Calendar followerLastModify;
    @Expose
    protected final TreeSet<String> followed;
    @Expose
    private Calendar followedLastModify;
    private TreeSet<Post> blog;

    //almeno 1 tag
    //tag null == null pointer exception
    public User(String name, String[] tags){
        if(tags.length > 5) throw new IllegalArgumentException("Too many tags");
        this.name = name;

        int i = 0;
        for(int k = 0; k<tags.length; k++){
            if(tags[k] != null) this.tags[i++] = tags[k];
        }
        //ordiniamo l'array di tags
        //può lanciare null pointer exception
        Arrays.sort(this.tags, 0, i);

        //data creazione account
        date = Calendar.getInstance();

        //collezioni
        followers = new TreeSet<>();
        followed = new TreeSet<>();
        blog = new TreeSet<>();

        followerLastModify = Calendar.getInstance();
        followedLastModify = Calendar.getInstance();

        walletHistory = new TreeSet<>();
        lastModify = Calendar.getInstance();
    }

    //rimozioni e aggiunte si basano sul confronto tramite id, le aggiunte non duplicano il contenuto
    public synchronized boolean addFollowed(String user){
        boolean dummy = followed.add(user);
        if(dummy)
            followedLastModify = Calendar.getInstance();
        return dummy;
    }
    public synchronized boolean deleteFollowed(String user){
        boolean dummy = followed.remove(user);
        if(dummy)
            followedLastModify = Calendar.getInstance();
        return dummy;
    }
    public synchronized boolean addFollower(String user){
        boolean dummy = followers.add(user);
        if(dummy)
            followerLastModify = Calendar.getInstance();
        return dummy;
    }
    public synchronized boolean deleteFollower(String user){
        boolean dummy = followers.remove(user);
        if(dummy)
            followerLastModify = Calendar.getInstance();
        return dummy;
    }

    public synchronized void newPost(Post p){
        //esistono i rewin...
        //if(!p.author.equals(this.name)) throw new IllegalArgumentException(p.author + "!=" + this.name);
        blog.add(p); //non aggiunge copie
        modify();
    }
    public synchronized void deletePost(Post p){ if(blog.remove(p)) modify();}

    public synchronized ArrayList<PostToken> getBlog(){
        ArrayList<PostToken> ret = new ArrayList<>();

        for (Post p :
                blog) {
            ret.add(new PostToken(p));
        }
        return ret;
    }

    public synchronized int getNFollower(){
        return followers.size();
    }
    public synchronized int getNFollowed(){
        return followed.size();
    }
    public synchronized int getNPost(){
        return blog.size();
    }

    public synchronized String[] getTags(){return tags.clone();}
    public synchronized boolean haveTag(String s){
        if(s == null) return false;
        if(tags[0] != null && s.equals(tags[0])) return true;
        if(tags[1] != null && s.equals(tags[1])) return true;
        if(tags[2] != null && s.equals(tags[2])) return true;
        if(tags[3] != null && s.equals(tags[3])) return true;
        if(tags[4] != null && s.equals(tags[4])) return true;
        return false;
    }

    public synchronized boolean isModified(Calendar since){return lastModify.after(since);}
    public synchronized boolean isFollowersModified(Calendar since){return followerLastModify.after(since);}
    public synchronized boolean isFollowedModified(Calendar since){return followedLastModify.after(since);}
    public synchronized boolean isWHModified(Calendar since){return walletHistory.last().date.after(since);}

    public synchronized ArrayList<String> getFollowers(){
        ArrayList<String> ret = new ArrayList<>();

        for (String s :
                followers) {
            ret.add(s);
        }
        return ret;
    }
    public synchronized ArrayList<String> getFollowed(){
        ArrayList<String> ret = new ArrayList<>();

        for (String s :
                followed) {
            ret.add(s);
        }
        return ret;
    }

    public synchronized double getWalletValue() {
        return wallet;
    }
    public synchronized boolean getWalletHistory(List<WalletTransaction> ret) {
        ret.clear();
        ret.addAll(walletHistory);
        return true;
    }
    public synchronized void newTransation(WalletTransaction t){
        walletHistory.add(t);
        wallet += t.value;
    }

    //metodo da usare dopo la deserializzazione per inizializzare alcune variabili
    public synchronized void recoveryState(){
        this.blog = new TreeSet<>();
    }

    private void modify(){
        lastModify = Calendar.getInstance();
    }


    @Override
    public int compareTo(User o) {
        return name.compareTo(o.name);
    }
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("SocialNetworkClasses.SUser{" +
                "name='" + name + '\'' +
                ", tags=" + Arrays.toString(tags) +
                ", date=" + SocialNetwork.df.format(date.getTime()) +
                ", follower=" + getNFollower() +
                ", followed=" + getNFollowed() +
                ", blog=" + getNPost() +
                '}');
        if(!walletHistory.isEmpty()) {
            str.append("\nWALLET: " + this.wallet + " last mod: " + SocialNetwork.df.format(walletHistory.last().date.getTime()) + "\n");
            boolean first = true;
            for (WalletTransaction t :
                    this.walletHistory) {
                if (first) {
                    first = false;
                    str.append("\t" + t);
                } else str.append("\n\t" + t);
            }
        }

        return str.toString();
    }
}
