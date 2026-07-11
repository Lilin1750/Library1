package exception;

public class InvalidPasswordException extends ValidationException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
