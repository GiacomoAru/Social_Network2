package SocialNetworkClasses;

import CustomException.InvalidOperationException;
import CustomException.PostNotFoundException;
import CustomException.UserAlreadyExistException;
import CustomException.UserNotFoundException;

import java.util.*;

public interface SocialNetworkInterface {

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

    /**
     *	Metodo per eseguire il rewin di un post presente nel social
     *	@param author autore del rewin
     *  @param post id del post di cui si vuole effettuare il rewin
     */
    public void rewinPost(String author, long post) throws UserNotFoundException, PostNotFoundException, InvalidOperationException;

    /**
     *	Metodo per eliminare un post all'interno del social
     *	@param user utente che vuole effettuare l'eliminazione del post
     *  @param id id del post da eliminare
     */
    public boolean deletePost(String user, long id) throws UserNotFoundException, InvalidOperationException;

    /**
     *	Metodo per creare un nuovo commento all'interno del social
     *	@param author autore del commento da creare
     *  @param reference id del post di cui la nuova istanza sarà commento
     */
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

    /**
     *	Metodo per aggiungere allo stato del social un nuovo voto assegnato ad un post da un user
     *	@param user autore del voto
     *  @param idPost id del post di cui si vuole aggiungere il voto
     *  @param vote voto da assegnare al post (positivo o negativo)
     */
    public void ratePost(String user, long idPost, int vote) throws UserNotFoundException, PostNotFoundException, InvalidOperationException;

    /**
     *	Metodo che restituisce true se il feed associato ad un determinato user è cambiato rispetto al feed dello stesso utente in data "time"
     *          false altrimenti
     *	@param user utente del feed di cui si chiedono informazioni
     *  @param time data dell'ultimo feed conosciuto
     */
    public boolean isFeedModified(String user, Calendar time) throws UserNotFoundException;
    /**
     *	Metodo che restituisce la lista dei post presenti nel feed di un utente
     *	@param user utente di cui verrà restituito il feed
     */
    public List<PostToken> getFeed(String user) throws UserNotFoundException;


    /**
     *	Metodo che restituisce il blog di un utente
     *	@param user utente di cui si richiede il blog
     */
    public List<PostToken> getBlog(String user) throws UserNotFoundException;
    /**
     *	Metodo che restituisce una copia di un post presente nel social
     *	@param id id del posst di cui si richiede una copia
     */
    public Post getPost(long id) throws PostNotFoundException;

    /**
     *	Metodo che restituisce true se la lista followers di user è cambiata rispetto a questa in una data definita, false altrimenti
     *	@param user utente di cui si richiede se la lista followers è cambiata
     *  @param time data precedente alle modifiche (se sono state apportate)
     */
    public boolean isFollowersModified(String user, Calendar time) throws UserNotFoundException;
    /**
     *	Metdofo che restituisce la lista followers di un user
     *	@param user utente di cui si chiede la lista followers
     */
    public List<String> getFollowers(String user) throws UserNotFoundException;

    /**
     *	Metodo che restituisce la lista di utenti deguiti da user
     *	@param user nome dell'utente di cui si richiede la lista di utenti che segue
     */
    public List<String> getFollowed(String user) throws UserNotFoundException;

    /**
     *	Metodo che restituisce true se il portafoglio di user è cambiata rispetto allo stesso in una data definita, false altrimenti
     *	@param user utente di cui si richiede se il portafoglio è cambiato
     *  @param time data precedente alle modifiche (se sono state apportate)
     */
    public boolean isWHModified(String user, Calendar time) throws UserNotFoundException;
    /**
     *	Metodo che restituisce il valore del portafoglio di un user e la lista delle transazioni associate
     *	@param user utente di cui si richiede il valore del portafoglio e lista transazioni
     *  @param walletH puntatore a lista in cui verranno aggiunte le transazioni associarte ad user
     */
    public double getWalletHistory(String user, List<WalletTransaction> walletH) throws UserNotFoundException;
    /**
     *	Metodo che restituisce il valore di un portafoglio del social
     *	@param user utente di cui si richiede il valore del portafoglio
     */
    public double getWalletValue(String user) throws UserNotFoundException;

    /**
     *	Metodo che restituisce una lista di utenti che hanno in comune almeno un tag con user
     *	@param user nome dell'utente di cui si richiede la lista di utenti simili
     */
    public Set<String> getSimilarUser(String user) throws UserNotFoundException;

    /**
     *	Metodo che esegue l'aggiornamento dei portafogli nel social
     */
    public void updateWallet();
    /**
     *	Metodo che modifica la percentuale della ricompensa autore
     *  @param val nuovo valore della ricompensa autore
     */
    public void setRicAut(float val) throws IllegalArgumentException;

    /**
     *	Metodo che esegue la serializzazione del social
     *  @param path percorso cartella in cui viene effettuata la serializzazione
     */
    public boolean serialize(String path);
    /**
     *	Metodo che esegue la deserializzazione del social
     *  @param path percorso cartella in cui viene effettuata la deserializzazione
     */
    public boolean deserialize(String path);
}
