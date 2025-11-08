package exhibitflow.reservation_service.exception;

public class StallNotAvailableException extends RuntimeException {
    public StallNotAvailableException(String message) {
        super(message);
    }
}
