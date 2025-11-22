package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base event for all reservation-related Kafka events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEvent {
    private Long reservationId;
    private Long userId;
    private Long stallId;
    private String status;
    private LocalDateTime timestamp;
    private String eventType;
}
