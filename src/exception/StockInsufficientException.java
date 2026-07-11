package exception;

public class StockInsufficientException extends BookNotAvailableException {
    public StockInsufficientException(String message) {
        super(message);
    }
}
