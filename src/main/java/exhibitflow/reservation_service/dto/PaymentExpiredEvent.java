package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when payment expires for a reservation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentExpiredEvent {
    private Long reservationId;
    private String userId;
    private Long stallId;
    private LocalDateTime paymentDeadline;
    private LocalDateTime expiredAt;
}
