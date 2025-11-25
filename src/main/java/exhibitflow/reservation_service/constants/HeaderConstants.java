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
    
    /**
     * Header name for JWT authorization token
     * Used for role-based authorization with external services
     */
    public static final String AUTHORIZATION = "Authorization";

    private HeaderConstants() {
        // Prevent instantiation
    }
}
