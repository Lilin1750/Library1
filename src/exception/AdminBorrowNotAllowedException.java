package exception;

public class AdminBorrowNotAllowedException extends BookNotAvailableException {
    public AdminBorrowNotAllowedException(String message) {
        super(message);
    }
}
