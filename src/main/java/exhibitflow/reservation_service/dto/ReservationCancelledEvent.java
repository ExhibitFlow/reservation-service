package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a reservation is cancelled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCancelledEvent {
    private Long reservationId;
    private Long userId;
    private Long stallId;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
}
