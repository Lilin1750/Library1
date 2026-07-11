package exception;

public class BorrowLimitExceededException extends BorrowException {
    public BorrowLimitExceededException(String message) {
        super(message);
    }
}
