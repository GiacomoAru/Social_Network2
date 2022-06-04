package SocialNetworkClasses;

import CustomException.InvalidOperationException;
import CustomException.PostNotFoundException;
import CustomException.UserAlreadyExistException;
import CustomException.UserNotFoundException;

import java.util.*;

public interface SocialNetworkInterface {

    //i post, utenti e commenti devono essere creati necessariamente dal social
    //per una transizione di stato corretta
    //L'information hiding è rispettata perchè si restituiscono copie delle istanze

    /**
     *	Metodo per creare un utente all'interno del social network
     *	@param name nome univoco dell'utente
     *  @param tags insieme di max 5 tag da associare all'utente
     */
    public void createUser(String name, String[] tags) throws UserAlreadyExistException;

    /**
     *	Metodo per creare un post associato ad un utente all'interno del social network
     *	@param title titolo del post
     *  @param content contenuto testuale del post
     *  @param author l'autore del post
     */
    public void createPost(String title, String content, String author) throws IllegalArgumentException, UserNotFoundException;
    public void rewinPost(String author, long post) throws UserNotFoundException, PostNotFoundException, InvalidOperationException;

    //se il post non esiste non fa nulla
    public boolean deletePost(String user, long id) throws UserNotFoundException, InvalidOperationException;

    public void createComment(String author, String text, long reference) throws PostNotFoundException, UserNotFoundException, InvalidOperationException;

    /**
     *	Metodo che aggiunge la relazione di follow all'interno del social
     *	@param user1 user che vuole seguire user2
     *  @param user2 user che è verrà seguito da user1
     *  @return true se lo stato viene aggiornato, falso se user1 seguiva già user2
     */
    public boolean follow(String user1, String user2) throws UserNotFoundException, InvalidOperationException;
    /**
     *	Metodo che rimuove la relazione di follow all'interno del social
     *	@param user1 user che vuole smettere di seguire user2
     *  @param user2 user che non sarà più seguito da user1
     *  @return true se lo stato viene aggiornato, falso se user1 non seguiva user2
     */
    public boolean unfollow(String user1, String user2) throws UserNotFoundException , InvalidOperationException;

    public void ratePost(String user, long idPost, int vote) throws UserNotFoundException, PostNotFoundException, InvalidOperationException;

    //operazione molto costosa, si cerca di prevenirla il più possibile
    public boolean isFeedModified(String user, Calendar time) throws UserNotFoundException;
    public List<PostToken> getFeed(String user) throws UserNotFoundException;


    public List<PostToken> getBlog(String user) throws UserNotFoundException;
    public Post getPost(long id) throws PostNotFoundException;

    public boolean isFollowersModified(String user, Calendar time) throws UserNotFoundException;
    public List<String> getFollowers(String user) throws UserNotFoundException;

    /**
     *	Metodo che restituisce la lista di utenti deguiti da user
     *	@param user nome dell'utente di cui si richiede la lista di utenti che segue
     */
    public List<String> getFollowed(String user) throws UserNotFoundException;

    public boolean isWHModified(String user, Calendar time) throws UserNotFoundException;
    public double getWalletHistory(String user, List<WalletTransaction> walletH) throws UserNotFoundException;
    public double getWalletValue(String user) throws UserNotFoundException;

    /**
     *	Metodo che restituisce una lista di utenti che hanno in comune almeno un tag con user
     *	@param user nome dell'utente di cui si richiede la lista di utenti simili
     */
    public Set<String> getSimilarUser(String user) throws UserNotFoundException;

    //aggiornamento portafogli
    public void updateWallet();
    public void setRicAut(float val) throws IllegalArgumentException;

    //salvataggio stato del social
    public boolean serialize(String path);
    public boolean deserialize(String path);
}
