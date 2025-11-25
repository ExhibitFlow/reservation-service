package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private Long reservationId;
    private String userId;
    private Long stallId;
    private Double amount;
    private String paymentMethod;
    private LocalDateTime paidAt;
}
