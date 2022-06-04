import java.io.Serializable;

public class SHPsw implements Serializable, Comparable<SHPsw>{
    public final String user;
    public final String hash;
    public final String seed;

    public SHPsw(String user, String hash, String seed){
        this.hash = hash;
        this.seed = seed;
        this.user = user;
    }

    public boolean test(String psw){
        psw = psw + seed;
        return hash.equals(SHash.bytesToHex(SHash.sha256(psw)));
    }


    @Override
    public int compareTo(SHPsw o) {
        return user.compareTo(o.user);
    }
    @Override
    public String toString(){
        return "nome: " + user + "  seed: " + seed + " \nhash: " + hash;
    }
}
