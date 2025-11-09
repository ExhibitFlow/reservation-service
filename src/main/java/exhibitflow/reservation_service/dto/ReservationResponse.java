package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String businessName;
    private Long stallId;
    private String stallCode;
    private String stallSize;
    private LocalDateTime createdAt;
    private String status;
    private String qrCodeBase64; // Base64 encoded QR code
    private LocalDateTime paymentExpiresAt; // When payment lock expires (5 minutes)
    private LocalDateTime paymentCompletedAt; // When payment was completed
}
