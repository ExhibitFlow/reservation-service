package exhibitflow.reservation_service.constants;

/**
 * Constants for HTTP headers used throughout the application
 */
public final class HeaderConstants {
    
    /**
     * Header name for request tracking ID
     */
    public static final String REQUEST_ID = "X-Request-Id";
    
    /**
     * Header name for authenticated user ID
     * Set by API Gateway after JWT authentication
     */
    public static final String USER_ID = "X-User-Id";
    
    private HeaderConstants() {
        // Prevent instantiation
    }
}
