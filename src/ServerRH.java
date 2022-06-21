import CustomException.InvalidOperationException;
import CustomException.PostNotFoundException;
import CustomException.UserNotFoundException;
import SocialNetworkClasses.Post;
import SocialNetworkClasses.PostToken;
import SocialNetworkClasses.SocialNetworkInterface;
import SocialNetworkClasses.WalletTransaction;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.Pattern;

public class ServerRH implements Runnable {

    private final Socket s;
    private final SocialNetworkInterface social;
    private final HashMap<String, SHPsw> regUser;
    private final SNotifyFollowImp snfi;
    private final String[] multicastAdd;

    private String nameUser = null;

    public ServerRH(Socket s, SocialNetworkInterface social, HashMap<String, SHPsw> regUser, SNotifyFollowImp snfi, String[] multicAdd){
        this.s = s;
        this.social = social;
        this.regUser = regUser;
        this.snfi = snfi;
        this.multicastAdd = multicAdd;
    }

    //

    //richieste da parte di client: sintassi
    //CODICERICHIESTA|ARGOMENTO1|ARGOMENTO2...|~
    //
    //0|nome|password|~                 -> login
    //1|~                               -> logout
    //2|~                               -> listUser
    //3|~                               -> listFollowing (lista di followed)
    //4|user|~                          -> follow
    //5|user|~                          -> unfollow
    //6|~                               -> viewBlog
    //7|title|content|~                 -> createPost
    //8|idPost|~                        -> rewinPost
    //9|idPost|~                        -> deletePost
    //10|idPost|voto|~                  -> ratePost
    //11|data|~                         -> showFeed
    //12|idPost|~                       -> showPost
    //13|idPost|content|~               -> comment
    //14|~                              -> getWallet
    //15|~                              -> getWalletBtc
    //16|~                              -> getFollowList
    //17|~                              -> getMulticastAddr



    //risposte da parte del server: sintassi
    //CODICEERRORE|ARGOMENTO1|ARGOMENTO2...|~
    //
    //login: 0=accettato, 1=password sbagliata
    //logout: nessuna risposta
    //listUser: 0=richiesta accettata, 1=errore generico
    //listFollowing: 0=richiesta accettata, 1=user not found
    //follow: 0=richiesta accettata, 1=richiesta superflua, 2=richiesta malformata, 3=user not found, 4=invalid op
    //listUser: 0=richiesta accettata, 1=richiesta superflua, 2=richiesta malformata, 3=user not found, 4=invalid op
    //viewBlog: 0=richiesta accettata, 1=user not fund
    //createPost: 0=richiesta accettata, 1= argomenti errati, 2=user not found
    //rewinPost: 0=richiesta accettata, 1=user not found, 2=post not found, 3=invalid op, 4=argomenti errati
    //deletePost: 0=richiesta accettata, 1=invalid op, 2=richiesta superflua, 3=argomenti errati, 4=user not found
    //ratePost: 0=richiesta accettata, 1=user not found, 2=post not found, 3=invalid op, 4=richiesta malformata
    //showFeed: 0=richiesta accettata (feed modificato), 1=richiesta accettata (feed invariato), 2=user not found
    //showPost: 0=richiesta accettata, 1=richiesta malformata, 2=post not found
    //comment: 0=richiesta accettata, 1=post not found, 2=invalid op, 3=user not found, 4=richiesta malformata
    //getWallet: 0=richiesta accettata, 1=user not found
    //getWalletBtc: 0=richiesta accettata, 1=user not found
    //getFollowList: 0=richiesta accettata, 1=errore generico
    //getMulticastAddr: 0=richiesta accettata

    @Override
    public void run() {

        try(InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream()){

            //lettura richiesta da parte del server
            while(!Thread.currentThread().isInterrupted()){

                String request = read(in);

                //richiesta letta, ora dobbiamo eseguirla
                String[] reqArray = request.split(Pattern.quote("|"));
                //ServerMain.stamp("[" + Thread.currentThread() + "] " + request);
                boolean dummyB;

                //con il nostro client non può arrivare una richiesta che non sia login come prima, perchè la connessione
                //viene instaurata subito prima della richiesta di login
                switch(reqArray[0]){

                    case "0"://login
                        dummyB = login(reqArray[1], reqArray[2]);
                        if(dummyB){
                            String risp = "0|~";
                            write(out, risp);

                            nameUser = reqArray[1];
                            ServerMain.stamp(Thread.currentThread().getName() + " log");
                        }
                        else{
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " logError");
                            closeTcp();
                        }
                        break;

                    case "1": //logout
                        //nessuna risposta
                        closeTcp();
                        ServerMain.stamp(Thread.currentThread().getName() + " unlog");
                        break;

                    case "2": //listUser
                        try {
                            Set<String> users = social.getSimilarUser(nameUser);

                            //creazione messaggio da inviare
                            StringBuilder risp = new StringBuilder("0|");
                            for (String s : users) risp.append(s).append("|");
                            risp.append("~");

                            ServerMain.stamp(Thread.currentThread().getName() + " listUser");

                            write(out, risp.toString());
                        }
                        catch (UserNotFoundException e) {
                            //errori nel socket
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " listUserError");

                            closeTcp();//errore non previsto
                        }
                        break;
                    case "3": //listFollowing
                        try {
                            List<String> users = social.getFollowed(nameUser);

                            //creazione messaggio da inviare
                            StringBuilder risp = new StringBuilder("0|");
                            for (String s : users) risp.append(s).append("|");
                            risp.append("~");

                            ServerMain.stamp(Thread.currentThread().getName() + " followedList");

                            write(out, risp.toString());
                        }
                        catch (UserNotFoundException e) {
                            //errori nel socket
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " followedListError");

                            closeTcp();//errore non previsto
                        }
                        break;
                    case "4": //follow
                        try {
                            //richiesta malformata
                            if(reqArray.length != 2) throw new IllegalArgumentException();
                            String risp;
                            if(social.follow(nameUser, reqArray[1])) {
                                risp = "0|~";//richiesta accettata
                            }else{
                                risp = "1|~";//richiesta superflua
                            }

                            snfi.notifyFollow(reqArray[1], nameUser, 1);//notifiche ai client registrati

                            ServerMain.stamp(Thread.currentThread().getName() + " follow");
                            write(out, risp);
                        }
                        catch (IllegalArgumentException e) {
                                //errore richiesta
                                String risp = "2|~";
                                write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " followError");
                        }
                        catch (UserNotFoundException e) {
                            //errore richiesta
                            String risp = "3|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " followError");
                        }
                        catch (InvalidOperationException e) {
                            //errore richiesta
                            String risp = "4|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " followError");
                        }
                        break;
                    case "5": //unfollow
                        try {
                            //richiesta malformata
                            if(reqArray.length != 2) throw new IllegalArgumentException();
                            String risp;

                            if(social.unfollow(nameUser, reqArray[1])){
                                risp = "0|~";//richiesta accettata
                            }else{
                                risp = "1|~";//richiesta superflua
                            }

                            snfi.notifyFollow(reqArray[1], nameUser, -1);//notifiche ai client registrati

                            ServerMain.stamp(Thread.currentThread().getName() + " unfollow");

                            write(out, risp);
                        }
                        catch (IllegalArgumentException e) {
                            //errore richiesta
                            String risp = "2|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " unfollowError");
                        }
                        catch (UserNotFoundException e) {
                            //errore richiesta
                            String risp = "3|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " unfollowError");
                        }
                        catch (InvalidOperationException e) {
                            //errore richiesta
                            String risp = "4|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " unfollowError");
                        }
                        break;
                    case "6": //viewBlog
                        try {
                            List<PostToken> dummyL = social.getBlog(nameUser);
                            StringBuilder risp = new StringBuilder("0|");

                            Gson gson = new Gson();
                            for (PostToken p : dummyL) risp.append(gson.toJson(p)).append("|");

                            risp.append("~");

                            ServerMain.stamp(Thread.currentThread().getName() + " viewBlog");
                            write(out, risp.toString());
                        }
                        catch (UserNotFoundException e) {
                            //errore richiesta
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " viewBlogError");
                        }
                        break;
                    case "7": //createPost
                        try {
                            //richiesta malformata
                            if(reqArray.length != 3) throw new IllegalArgumentException();
                            String risp;

                            social.createPost(reqArray[1], reqArray[2], nameUser);

                            risp = "0|~";
                            ServerMain.stamp(Thread.currentThread().getName() +" create post");
                            write(out, risp);
                        }
                        catch (IllegalArgumentException e) {
                            //errore richiesta
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" createPostError");
                        }
                        catch (UserNotFoundException e) {
                            //errore richiesta
                            String risp = "2|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" createPostError");
                        }
                        break;
                    case "8": //rewinPost
                        try {
                            //richiesta malformata
                            if(reqArray.length != 2) throw new IllegalArgumentException();
                            String risp;

                            social.rewinPost(nameUser,  Long.parseLong(reqArray[1]));

                            risp = "0|~";
                            ServerMain.stamp(Thread.currentThread().getName() +" rewin post");
                            write(out, risp);
                        }
                        catch (IllegalArgumentException e) {
                            //errore richiesta
                            String risp = "4|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" rewinPostError");
                        }
                        catch (UserNotFoundException e) {
                            //errore richiesta
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" rewinPostError");
                        }
                        catch (PostNotFoundException e) {
                            //errore richiesta
                            String risp = "2|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" rewinPostError");
                        }
                        catch (InvalidOperationException e) {
                            //errore richiesta
                            String risp = "3|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" rewinPostError");
                        }
                        break;
                    case "9": //deletePost
                        try {
                            //richiesta malformata
                            if(reqArray.length != 2) throw new IllegalArgumentException();
                            String risp;

                            boolean dummy = social.deletePost(nameUser, Long.parseLong(reqArray[1]));

                            if(dummy){
                                risp = "0|~";
                                ServerMain.stamp(Thread.currentThread().getName() +" deletePost");
                                write(out, risp);
                            }else{
                                risp = "2|~";
                                ServerMain.stamp(Thread.currentThread().getName() +" deletePostError");
                                write(out, risp);
                            }
                        }
                        catch (InvalidOperationException e) {
                            //errore richiesta
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" deletePostError");
                        }
                        catch (IllegalArgumentException e){
                            //errore richiesta
                            String risp = "3|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" deletePostError");
                        }
                        catch (UserNotFoundException e) {
                            String risp = "4|~";
                            ServerMain.stamp(Thread.currentThread().getName() +" deletePostError");
                            write(out, risp);
                        }
                        break;
                    case "10": //ratePost
                        try {
                            //richiesta malformata
                            if(reqArray.length != 3) throw new IllegalArgumentException();
                            String risp;

                            social.ratePost(nameUser, Long.parseLong(reqArray[1]), Integer.parseInt(reqArray[2]));

                            risp = "0|~";
                            ServerMain.stamp(Thread.currentThread().getName() +" ratePost");
                            write(out, risp);
                        }
                        catch (UserNotFoundException e) {
                            //errore richiesta
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" ratePostError");
                        }
                        catch (PostNotFoundException e) {
                            //errore richiesta
                            String risp = "2|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" ratePostError");
                        }
                        catch (InvalidOperationException e) {
                            //errore richiesta
                            String risp = "3|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" ratePosError");
                        }
                        catch (IllegalArgumentException e){
                            //errore richiesta
                            String risp = "4|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" ratePostError");
                        }
                        break;
                    case "11": //showFeed
                        try {
                            String risp = showFeed(reqArray);

                            ServerMain.stamp(Thread.currentThread().getName() +" showFeed");
                            write(out, risp);
                        }
                        catch (Exception e) {
                            //errore richiesta
                            String risp = "2|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" showFeedError");
                            e.printStackTrace();
                        }
                        break;
                    case "12": //showPost
                        try {
                            //richiesta malformata
                            if(reqArray.length != 2) throw new IllegalArgumentException();

                            //get post, throw eccez.
                            Post p = social.getPost(Long.parseLong(reqArray[1]));

                            StringBuilder risp = new StringBuilder("0|");
                            Gson g = new Gson();
                            risp.append(g.toJson(p)).append("|~");

                            ServerMain.stamp(Thread.currentThread().getName() +" showPost");
                            write(out, risp.toString());
                        }
                        catch (IllegalArgumentException e) {
                            //errore richiesta
                            String risp = "2|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" showPostError");
                        }
                        catch (PostNotFoundException e) {
                            //errore richiesta
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" showPostError");
                        }
                        break;
                    case "13": //comment
                        try {
                            //richiesta malformata
                            if(reqArray.length != 3) throw new IllegalArgumentException();
                            String risp;

                            social.createComment(nameUser, reqArray[2], Long.parseLong(reqArray[1]));

                            risp = "0|~";
                            ServerMain.stamp(Thread.currentThread().getName() +" comment");
                            write(out, risp);
                        }
                        catch (IllegalArgumentException e) {
                            //errore richiesta
                            String risp = "3|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" commentError");
                        }
                        catch (PostNotFoundException e) {
                            //errore richiesta
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" commentError");
                        }
                        catch (UserNotFoundException e) {
                            //errore richiesta
                            String risp = "4|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" commentError");
                        }
                        catch (InvalidOperationException e) {
                            //errore richiesta
                            String risp = "2|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" commentError");
                        }
                        break;
                    case "14": //getWallet
                        try {
                            List<WalletTransaction> dummyL = new ArrayList<>();

                            StringBuilder risp = new StringBuilder("0|");
                            risp.append(social.getWalletHistory(nameUser, dummyL)).append("|");

                            Gson gson = new Gson();
                            for (WalletTransaction w : dummyL) risp.append(gson.toJson(w)).append("|");

                            risp.append("~");

                            ServerMain.stamp(Thread.currentThread().getName() +" getWallet");
                            write(out, risp.toString());
                        }
                        catch (UserNotFoundException e) {
                            //errore richiesta
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" getWalletError");
                        }
                        break;
                    case "15": //getWalletBtc
                        try {

                            double convers = SRandomOrg.getDouble(5);
                            String risp = "0|" + social.getWalletValue(nameUser)*convers + "|~";

                            ServerMain.stamp(Thread.currentThread().getName() +" getWalletBtc");
                            write(out, risp.toString());
                        } catch (UserNotFoundException e) {
                            //errore richiesta
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() +" getWalletBtcError");
                        }
                        break;
                    case "16": //getFollowList
                        try {
                            List<String> users = social.getFollowers(nameUser);

                            //creazione messaggio da inviare
                            StringBuilder risp = new StringBuilder("0|");
                            for (String s : users) risp.append(s).append("|");
                            risp.append("~");

                            ServerMain.stamp(Thread.currentThread().getName() + " followerList");

                            write(out, risp.toString());
                        }
                        catch (UserNotFoundException e) {
                            //errori nel socket
                            String risp = "1|~";
                            write(out, risp);

                            ServerMain.stamp(Thread.currentThread().getName() + " followerListError");

                            closeTcp();//errore non previsto
                        }
                        break;
                    case "17": //getMulticastAddress

                        String risp = "0|" + multicastAdd[0] + "|" + multicastAdd[1] + "|~";

                        ServerMain.stamp(Thread.currentThread().getName() +" getMulticastAddress");
                        write(out, risp);

                        break;
                    default: //boh
                        ServerMain.stamp(Thread.currentThread().getName() +" richiesta non riconosciuta");
                        System.out.println(reqArray);
                        break;

                }
            }
            s.close();
            ServerMain.stamp(Thread.currentThread().getName() +" Client disconnesso");
        } catch (Exception e) {
            ServerMain.stamp(Thread.currentThread().getName() +" Client disconnesso");
        }
    }

    private boolean login(String userName, String password){
        synchronized (regUser){
            SHPsw dummy = regUser.get(userName);
            if(dummy != null){
                return (dummy.test(password));
            }
            else return false;
        }
    }
    private void closeTcp(){
        ServerMain.stamp(Thread.currentThread().getName() +" closing tcp");
        try {
            if(s != null && !s.isClosed()) s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread.currentThread().interrupt();
    }

    private String showFeed(String[] reqArray) throws UserNotFoundException {
        if(reqArray.length != 2) throw new IllegalArgumentException();
        Gson g = new Gson();
        Calendar c = g.fromJson(reqArray[1], Calendar.class);

        if(!social.isFeedModified(nameUser, c)){
            return "1|~";
        }else{
            List<PostToken> dummyL = social.getBlog(nameUser);
            StringBuilder risp = new StringBuilder("0|");

            dummyL = social.getFeed(nameUser);
            for (PostToken p : dummyL) {
                risp.append(g.toJson(p)).append("|");
            }
            risp.append("~");

            return risp.toString();
        }
    }

    private String read(InputStream in){
        Scanner s = new Scanner(in).useDelimiter("~");
        return s.next();
    }
    //scrive
    private void write(OutputStream out, String s) throws IOException {
        out.write(s.getBytes());
    }
}