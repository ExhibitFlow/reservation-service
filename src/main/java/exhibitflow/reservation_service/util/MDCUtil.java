package exhibitflow.reservation_service.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for managing Mapped Diagnostic Context (MDC)
 * Enables request tracking and correlation across logs
 */
public class MDCUtil {

    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String RESERVATION_ID = "reservationId";

    private MDCUtil() {
        // Utility class
    }

    /**
     * Generates and sets a unique request ID
     */
    public static String generateRequestId() {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID, requestId);
        return requestId;
    }

    /**
     * Sets an existing request ID in MDC
     * Used when propagating request ID from upstream services
     */
    public static void setRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isEmpty()) {
            MDC.put(REQUEST_ID, requestId);
        }
    }

    /**
     * Sets user ID in MDC
     */
    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
    }

    /**
     * Sets reservation ID in MDC
     */
    public static void setReservationId(Long reservationId) {
        if (reservationId != null) {
            MDC.put(RESERVATION_ID, reservationId.toString());
        }
    }

    /**
     * Clears all MDC data
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * Gets current request ID
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }
}
