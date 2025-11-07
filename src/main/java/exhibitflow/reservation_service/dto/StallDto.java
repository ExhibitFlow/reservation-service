package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Stall data received from Stall Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StallDto {
    private Long id;
    private String stallCode;
    private String size;
    private Double price;
    private Boolean isReserved;
}
