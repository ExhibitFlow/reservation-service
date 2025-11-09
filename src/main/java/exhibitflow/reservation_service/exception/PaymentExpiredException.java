package exhibitflow.reservation_service.exception;

public class PaymentExpiredException extends RuntimeException {
    public PaymentExpiredException(String message) {
        super(message);
    }
}
