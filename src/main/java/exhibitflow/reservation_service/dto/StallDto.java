package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Stall data received from Stall Service
 * Must match the StallResponse structure from Stall Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StallDto {
    private Long id;
    private String code;
    private String size;  // StallSize enum serialized as string
    private String location;
    private BigDecimal price;  // Changed from Double to BigDecimal
    private String status;  // StallStatus enum serialized as string
    private LocalDateTime createdAt;  // Changed from Instant to LocalDateTime
    private LocalDateTime updatedAt;  // Changed from Instant to LocalDateTime
}
