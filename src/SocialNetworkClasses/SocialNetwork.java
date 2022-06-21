package SocialNetworkClasses;

import CustomException.InvalidOperationException;
import CustomException.PostNotFoundException;
import CustomException.UserAlreadyExistException;
import CustomException.UserNotFoundException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;

public class SocialNetwork implements SocialNetworkInterface {

    //formato di stampa della data
    protected static DateFormat df = new SimpleDateFormat("(dd/MM/yyyy hh:mm:ss)");

    //valori modificabili
    private double ricompenseAutore;

    //hash map degli utenti non automaticamente sincronizzata
    //reade write lock associate
    protected final HashMap<String, User> users;
    protected final ReentrantReadWriteLock rwlock;
    protected final ReentrantReadWriteLock.ReadLock userRLock;
    protected final ReentrantReadWriteLock.WriteLock userWLock;

    //autonomamente thread safe
    protected final ConcurrentHashMap<Long, Post> posts;
    protected final ConcurrentHashMap<Long, Comment> comments;

    //variabili per gli id di post/commenti/transazioni
    AtomicLong idP = new AtomicLong(0);
    AtomicLong idC = new AtomicLong(0);
    AtomicLong idWT = new AtomicLong(0);

    public SocialNetwork(double ricompensaAutore) {
        users = new HashMap<>();
        posts = new ConcurrentHashMap<>();
        comments = new ConcurrentHashMap<>();

        //è implementat ana politica fair
        rwlock = new ReentrantReadWriteLock(true);
        userRLock = rwlock.readLock();
        userWLock = rwlock.writeLock();

        this.ricompenseAutore = ricompensaAutore;
    }

    @Override
    public void createUser(String name, String[] tags) throws UserAlreadyExistException {
        userWLock.lock();

        try{
            User dummy = new User(name, tags);
            //il valore di ritorno è null se la key non era associata a nulla (non permettiamo aggiunte di bind key-null)
            if (users.putIfAbsent(name, dummy) != null) throw new UserAlreadyExistException(name);

        }finally{userWLock.unlock();}
    }

    //post
    @Override
    public void createPost(String title, String content, String author) throws IllegalArgumentException, UserNotFoundException {
        userRLock.lock();
        try{
            User dummyU = users.get(author);
            if(dummyU != null){
                Post dummyP = new Post(title, content, author, idP.getAndIncrement());
                posts.put(dummyP.id, dummyP);
                dummyU.newPost(dummyP);
            }
            else throw new UserNotFoundException(author);
        }finally{userRLock.unlock();}
    }
    @Override
    public void rewinPost(String author, long post) throws UserNotFoundException, PostNotFoundException, InvalidOperationException {
        userRLock.lock();
        try{
            //autore deve esistere
            User dummyU = users.get(author);
            if(dummyU != null){
                //il post da ricondividere deve esistere
                Post target = posts.get(post);
                if(target != null) {

                    //user ha il post nel feed e non è il suo post
                    if(!dummyU.followed.contains(target.getOriginalAuthor())) throw new InvalidOperationException("post " + post  + " not in feed");

                    //creiamo
                    Post dummyP = new PostRewin(author, idP.getAndIncrement(), target, target.id);
                    posts.put(dummyP.id, dummyP);
                    dummyU.newPost(dummyP);
                }else throw new PostNotFoundException(post + "");
            }
            else throw new UserNotFoundException(author);
        }finally{userRLock.unlock();}
    }

    @Override
    public boolean deletePost(String user, long id) throws InvalidOperationException, UserNotFoundException {
        userRLock.lock();
        try{
            Post dummyP = posts.get(id);

            if(dummyP == null) return false; //post inesistente
            if(users.get(user) == null) throw new UserNotFoundException(user);

            if(!dummyP.getAuthor().equals(user)) throw new InvalidOperationException("author != user");

            dummyP = posts.remove(id);

            if(dummyP != null){
                users.get(dummyP.getAuthor()).deletePost(dummyP);//se c'è il post c'è l'autore

                //ormai il post è irraggiungibile dagli altri thread perchè non presente ne
                //nella hashtable dei post ne nell'istanza dell'autore
                //ci manca solo rimuovere tutti i commenti
                for (Long l: dummyP.getAllComment()) {
                    comments.remove(l);
                }

                //eliminiamo tutti i rewin associati
                for (Post p :
                        posts.values()) {
                    if(p.rewin){
                        if(((PostRewin) p).getReference() == dummyP.id){
                            Post d2 = posts.remove(p.id);
                            users.get(p.getAuthor()).deletePost(p);//se c'è il post c'è l'autore
                            for (Long l: d2.getAllComment()) {
                                comments.remove(l);
                            }
                        }
                    }
                }

                //post eliminato completamente
            }
        }finally{userRLock.unlock();}
        return true;
    }
    @Override
    public void ratePost(String user, long idPost, int vote) throws UserNotFoundException, PostNotFoundException, InvalidOperationException {
        userRLock.lock();
        try{
            //user esiste
            User dummyU = users.get(user);
            if(dummyU == null)  throw new UserNotFoundException(user);

            //post esiste
            Post dummyP =  posts.get(idPost);
            if(dummyP == null) throw new PostNotFoundException(""+idPost);

            //user ha il post nel feed e non è il suo post
            if(!dummyU.followed.contains(dummyP.getOriginalAuthor())) throw new InvalidOperationException("post " + idPost+ " not in feed");

            if(!dummyP.newVote(user, vote)) throw new InvalidOperationException(""+vote);
        }finally{userRLock.unlock();}
    }

    @Override
    public boolean isFeedModified(String user, Calendar time) throws UserNotFoundException {
        boolean ret = false;

        userRLock.lock();
        try{

            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);
            System.out.println(u.isFollowedModified(time));
            if(u.isFollowedModified(time)) return true;

            User dummyU;

            for (String s :
                    u.getFollowed()) {
                dummyU = users.get(s);//se lo stato del social è corretto non può restituire null
                if(dummyU.isModified(time)){
                    ret = true;
                    break;
                }
            }
        }catch(Exception e) {e.printStackTrace();} finally{userRLock.unlock();}
        return ret;
    }
    @Override
    public ArrayList<PostToken> getFeed(String user) throws UserNotFoundException {
        userRLock.lock();
        ArrayList<PostToken> ret = new ArrayList<>();
        try{
            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);


            User dummyU;

            for (String s :
                    u.getFollowed()) {
                dummyU = users.get(s);

                ret.addAll(dummyU.getBlog());//super complessità
            }
        }finally{userRLock.unlock();}
        return ret;
    }
    @Override
    public ArrayList<PostToken> getBlog(String user) throws UserNotFoundException {
        userRLock.lock();
        ArrayList<PostToken> ret;
        try{
            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);

            ret = u.getBlog();
        }finally{userRLock.unlock();}
        return ret;
    }
    @Override
    public Post getPost(long id) throws PostNotFoundException {
        Post ret = posts.get(id);
        if(ret == null) throw new PostNotFoundException(""+id);

        return ret.copy();
    }

    //commenti
    @Override
    public void createComment(String author, String text, long reference) throws PostNotFoundException, UserNotFoundException, InvalidOperationException {
        userRLock.lock();
        try{
            if( users.containsKey(author) ){
                //post esiste
                Post dummyP = posts.get(reference);
                if(dummyP != null){
                    //post nel feed e non è suo
                    if(!users.get(author).followed.contains(dummyP.getOriginalAuthor())) throw new InvalidOperationException("post " + dummyP.id+ " not in feed");

                    Comment dummyC = new Comment(author, text, reference, idC.getAndIncrement());
                    dummyP.addComment(dummyC);
                    comments.put(dummyC.id, dummyC);
                }else throw new PostNotFoundException(reference + "");
            }
            else throw new UserNotFoundException(author);
        }finally{userRLock.unlock();}
    }

    //follow
    @Override
    public boolean follow(String user1, String user2) throws UserNotFoundException, InvalidOperationException{
        if(user1.equals(user2)) throw new InvalidOperationException(user1 + " == " + user2);
        boolean ret;
        userRLock.lock();
        try{
            User u1 = users.get(user1);
            User u2 = users.get(user2);

            if(u1 == null) throw new UserNotFoundException(user1);
            if(u2 == null) throw new UserNotFoundException(user2);

            //aggiungono la stringa se non è già duplicata
            ret = u1.addFollowed(user2) && u2.addFollower(user1);
        }finally{userRLock.unlock();}
        return ret;
    }
    @Override
    public boolean unfollow(String user1, String user2) throws UserNotFoundException, InvalidOperationException{
        if(user1.equals(user2)) throw new InvalidOperationException(user1 + " == " + user2);
        boolean ret;
        userRLock.lock();
        try{
            User u1 = users.get(user1);
            User u2 = users.get(user2);

            if(u1 == null) throw new UserNotFoundException(user1);
            if(u2 == null) throw new UserNotFoundException(user2);

            //aggiungono la stringa se non è già duplicata
            ret = u1.deleteFollowed(user2) && u2.deleteFollower(user1);
        }finally{userRLock.unlock();}
        return ret;
    }

    @Override
    public boolean isFollowersModified(String user, Calendar time) throws UserNotFoundException {
        userRLock.lock();
        boolean ret = true;
        try{
            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);

            ret = u.isFollowersModified(time);
        }finally{userRLock.unlock();}
        return ret;
    }
    @Override
    public ArrayList<String> getFollowers(String user) throws UserNotFoundException {
        userRLock.lock();
        ArrayList<String> ret = null;
        try{
            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);

            ret = u.getFollowers();
        }finally{userRLock.unlock();}
        return ret;
    }

    @Override
    public List<String> getFollowed(String user) throws UserNotFoundException {
        userRLock.lock();
        ArrayList<String> ret = null;
        try{
            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);

            ret = u.getFollowed();
        }finally{userRLock.unlock();}
        return ret;
    }

    //wallet
    @Override
    public boolean isWHModified(String user, Calendar time) throws UserNotFoundException {
        boolean ret = true;
        userRLock.lock();
        try{
            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);

            ret = u.isWHModified(time);
        }finally{userRLock.unlock();}
        return ret;
    }
    @Override
    public double getWalletHistory(String user, List<WalletTransaction> walletH) throws UserNotFoundException {
        userRLock.lock();
        double ret;
        try{
            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);

            ret = u.getWalletValue();
            u.getWalletHistory(walletH);
        }finally{userRLock.unlock();}

        return ret;
    }
    @Override
    public double getWalletValue(String user) throws UserNotFoundException {
        userRLock.lock();
        double ret;
        try{
            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);

            ret = u.getWalletValue();
        }finally{userRLock.unlock();}
        return ret;
    }

    @Override
    public void updateWallet() {
        for (Post p :
                posts.values()) {
            if(p.rewin) continue;
            int primoLog;
            double secondoLog;

            synchronized (p) {
                //influenza voti
                primoLog = p.getCommentAuthorCache().size() - p.getDownVoteCache().size();
                secondoLog = 1;

                //influenza commenti (complicato)
                for (String aut :
                        p.getCommentAuthorCache()) {
                    int altriComm = 0;
                    for (Comment c : p.getComments()) if (c.author.equals(aut)) altriComm++;
                    secondoLog += 2 / (1 + Math.exp(-altriComm + 1));
                }

                //calcoliamo la ricompensa totale
                double tot = Math.log(Math.max(primoLog, 0) + 1) + Math.log(secondoLog);
                tot /= p.getIterNumber();
                p.addIterNumber();
                if(tot > 0) {
                    userRLock.lock();
                    try {
                        //ricompensa assegnata all'autore del post
                        User dummyU = users.get(p.getOriginalAuthor());
                        dummyU.newTransation(new WalletTransaction(Calendar.getInstance(), p.id + "_A", dummyU.name, tot * ricompenseAutore, idWT.getAndIncrement()));

                        //ricompense assegnate a i curatori, solo quelli corretti
                        TreeSet<String> cur = p.getCommentAuthorCache();
                        cur.addAll(p.getUpVoteCache());
                        double ricompensaCuratore = (1 - ricompenseAutore) / cur.size();

                        for (String s : cur) {
                            dummyU = users.get(s);
                            dummyU.newTransation(new WalletTransaction(Calendar.getInstance(), p.id + "_C", dummyU.name, tot * ricompensaCuratore, idWT.getAndIncrement()));
                        }
                    } finally {
                        userRLock.unlock();
                    }
                }
                //reset della "cache"
                p.clearCache();
            }
        }
    }

    //serializzazione
    @Override
    public boolean serialize(String path) {
        //prendiamo la lock più potente così da evitare al massimo errori, da ricontrollare però

        userWLock.lock();
        //aggiorniamo i portafogli prima di chiudere, così da liberare tutte le cache
        //dato che le lock sono reentrant quindi è possibile fare prima write poi read
        //this.updateWallet();
        try {
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
            boolean first = true;

            //user
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path + "\\users.txt"))) {
                out.write("[");
                for (User u : this.users.values()) {
                    if(first) first = false;
                    else  out.write(",");
                    out.write(gson.toJson(u));
                }
                out.write("]");
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            //post
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path + "\\posts.txt"))) {
                first = true;
                out.write("[");
                for (Post p : this.posts.values()){
                    if(!p.rewin) {
                        if (first) first = false;
                        else out.write(",");
                        out.write(gson.toJson(p));
                    }
                }
                out.write("]");
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            //rewin
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path + "\\postsR.txt"))) {
                first = true;
                out.write("[");
                for (Post p : this.posts.values()){
                    if(p.rewin) {
                        if (first) first = false;
                        else out.write(",");
                        out.write(gson.toJson(p));
                    }
                }
                out.write("]");
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            //comments
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path + "\\comments.txt"))) {
                first = true;
                out.write("[");
                for (Comment c : this.comments.values()){
                    if(first) first = false;
                    else  out.write(",");
                    out.write(gson.toJson(c));
                }
                out.write("]");
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            //altre variabili del social
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path + "\\social.txt"))) {

                out.write(gson.toJson(this.idP));
                out.write("\n");
                out.write(gson.toJson(this.idC));
                out.write("\n");
                out.write(gson.toJson(this.idWT));
                out.write("\n");

            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }finally{userWLock.unlock();}
        return true;
    }
    @Override
    public boolean deserialize(String path) {
        //prendiamo la lock più potente così da evitare al massimo errori, da ricontrollare però
        userWLock.lock();

        //cancelliamo lo stato attuale
        users.clear();
        comments.clear();
        posts.clear();

        System.gc();

        try {

            //se lo stato prima della serializzazione era corretto allora termina correttamente
            //altrimenti boom

            //user
            try (FileInputStream in = new FileInputStream(path + "\\users.txt")) {
                //creiamo l'oggetto per deserializzare con lo stream
                JsonReader reader = new JsonReader(new InputStreamReader(in));
                //inizia l'array di oggetti
                reader.beginArray();
                while (reader.hasNext()) {
                    //deserializziamo
                    User u = new Gson().fromJson(reader, User.class);
                    //ripristiniamo lo stato dell'oggetto inizializzando alcune variabili non deserializzate
                    u.recoveryState();

                    //ripristiniamo correttamente lo stato del social
                    this.users.put(u.name, u);
                }
                reader.endArray();
                reader.close();
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            //post
            try (FileInputStream in = new FileInputStream(path + "\\posts.txt")) {
                //tutto guale a sopra
                JsonReader reader = new JsonReader(new InputStreamReader(in));
                reader.beginArray();
                while (reader.hasNext()) {
                    Post p = new Gson().fromJson(reader, Post.class);
                    p.recoveryState();

                    //aggiungendo l'oggetto in tutte le strutture dati corrette
                    User dummyU = this.users.get(p.getAuthor());
                    //errori accadono, cerchiamo di risolverli
                    if(dummyU!= null){
                        dummyU.newPost(p);
                        //ripristiniamo correttaente lo stato del social
                        this.posts.put(p.id, p);
                    }
                }
                reader.endArray();
                reader.close();
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            //postR
            try (FileInputStream in = new FileInputStream(path + "\\postsR.txt")) {
                //tutto guale a sopra
                JsonReader reader = new JsonReader(new InputStreamReader(in));
                reader.beginArray();
                while (reader.hasNext()) {
                    PostRewin p = new Gson().fromJson(reader, PostRewin.class);
                    p.recoveryState(posts.get(p.getReference()));

                    //ripristiniamo correttaente lo stato del social
                    this.posts.put(p.id, p);
                    //aggiungendo l'oggetto in tutte le strutture dati corrette
                    User dummyU = this.users.get(p.getAuthor());
                    dummyU.newPost(p);
                }
                reader.endArray();
                reader.close();
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            //comments
            try (FileInputStream in = new FileInputStream(path + "\\comments.txt")) {
                JsonReader reader = new JsonReader(new InputStreamReader(in));
                reader.beginArray();
                while (reader.hasNext()) {
                    Comment c = new Gson().fromJson(reader, Comment.class);
                    this.comments.put(c.id, c);
                    Post dummyP = this.posts.get(c.reference);
                    dummyP.addComment(c);
                }
                reader.endArray();
                reader.close();
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            //altro
            try (FileInputStream in = new FileInputStream(path + "\\social.txt")) {
                JsonReader reader = new JsonReader(new InputStreamReader(in));
                Gson gson = new GsonBuilder().create();

                idP = gson.fromJson(reader, idP.getClass());
                idC = gson.fromJson(reader, idP.getClass());
                idWT = gson.fromJson(reader, idP.getClass());

                reader.close();
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }

        }finally{userWLock.unlock();}
        return true;
    }

    //altro
    @Override
    public TreeSet<String> getSimilarUser(String user) throws UserNotFoundException {
        userRLock.lock();
        TreeSet<String> ret = new TreeSet<>();
        try{
            User u = users.get(user);
            if(u == null) throw new UserNotFoundException(user);

            for (String s :
                    u.getTags()) {
                for (User dummyU :
                        users.values() ) {
                    if(!dummyU.name.equals(user) && dummyU.haveTag(s)) ret.add(dummyU.name);
                }
            }
        }finally{userRLock.unlock();}
        return ret;
    }
    @Override
    public void setRicAut(float val) throws IllegalArgumentException {
        if(val >= 0 && val <= 1) this.ricompenseAutore = val;
        else throw new IllegalArgumentException(val + " non valid");
    }

    //forse non concurrent, non importa
    public String toString(){
        StringBuilder str = new StringBuilder("SOCIAL NETWORK:\n");
        str.append("[NPost:" + idP + ",NComment:" + idC + ",NWT:" + idWT + "]\n");
        for (Post p :
                posts.values()) {
            str.append(p + "\n");
        }
        for (Comment c :
                comments.values()) {
            str.append(c + "\n");
        }
        userRLock.lock();
        try {
            for (User u :
                    users.values()) {
                str.append(u + "\n");
            }
        }finally{userRLock.unlock();}

        return str.toString();
    }
}

