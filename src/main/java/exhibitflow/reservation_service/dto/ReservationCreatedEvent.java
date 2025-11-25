package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a reservation is created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreatedEvent {
    private Long reservationId;
    private String userId;
    private Long stallId;
    private Double totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime paymentDeadline;
}
