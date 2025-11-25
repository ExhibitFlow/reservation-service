package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StallMapDto {
    private Long id;
    private String stallCode;
    private String size;
    private Double price;
    private Boolean isReserved;
    private String code;
    private String description;
    
    
    private CoordinateDto location;  
    private List<List<Double>> boundary;
    

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoordinateDto {
        private Double longitude;
        private Double latitude;
    }
}
