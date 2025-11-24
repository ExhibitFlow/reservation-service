package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for Stall data received from Stall Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StallDto {
    private Long id;
    private String code;
    private String size;
    private String location;  // Changed from CoordinateDto to String to match API response
    private Double price;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private String description;
    private List<List<Double>> boundary;
    private Boolean isReserved;
}
