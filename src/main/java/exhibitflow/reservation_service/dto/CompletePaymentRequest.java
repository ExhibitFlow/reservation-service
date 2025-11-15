package exhibitflow.reservation_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for completing payment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompletePaymentRequest {
    
    @NotNull(message = "Payment confirmation is required")
    private Boolean paymentConfirmed;
    
    private String transactionId; // Optional payment transaction reference
    
    private LocalDateTime paymentTimestamp;
}
