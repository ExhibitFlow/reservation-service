package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a stall is successfully reserved after payment completion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StallReservedEvent {
    private String eventId;
    private String eventType;
    private String eventVersion;
    private String occurredAt;

    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String reservationId;
        private String stallId;
        private String userId;
        private Size size;
        private ReservationDetails reservationDetails;
        private String qrCode;
        private double paidAmount;
    }

    public enum Size {
        SMALL,
        MEDIUM,
        LARGE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationDetails {
        private String reservedAt;
        private String expiresAt;
        private String status;
        private String notes;
    }
}
