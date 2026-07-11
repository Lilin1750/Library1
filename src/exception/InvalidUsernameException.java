package exception;

public class InvalidUsernameException extends ValidationException {
    public InvalidUsernameException(String message) {
        super(message);
    }
}
