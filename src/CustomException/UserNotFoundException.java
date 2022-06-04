package CustomException;

public class UserNotFoundException extends WinsomeException{
    public UserNotFoundException(String s) {
        super(s);
    }
}
