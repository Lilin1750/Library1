package exception;

public class BookNotAvailableException extends BookException {
    public BookNotAvailableException(String message) {
        super(message);
    }
}
