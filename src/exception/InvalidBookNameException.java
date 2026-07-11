package exception;

public class InvalidBookNameException extends ValidationException {
    public InvalidBookNameException(String message) {
        super(message);
    }
}
