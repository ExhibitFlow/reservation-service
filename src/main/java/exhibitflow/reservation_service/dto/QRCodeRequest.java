package exhibitflow.reservation_service.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRCodeRequest {
    
    @Min(value = 100, message = "Width must be at least 100 pixels")
    @Builder.Default
    private Integer width = 300;
    
    @Min(value = 100, message = "Height must be at least 100 pixels")
    @Builder.Default
    private Integer height = 300;
    
    @Builder.Default
    private String errorCorrectionLevel = "H"; // L, M, Q, H
}
