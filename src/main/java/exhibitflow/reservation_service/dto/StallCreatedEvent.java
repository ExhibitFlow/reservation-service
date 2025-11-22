package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Event published by Stall Service when a new stall is created
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StallCreatedEvent {
    private Long stallId;
    private String stallCode;
    private String size;
    private Double price;
    private String zone;
    private Integer floorNumber;
    private String description;
    private CoordinateDto location;
    private List<List<Double>> boundary;
    private Long timestamp;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoordinateDto {
        private Double longitude;
        private Double latitude;
    }
}
