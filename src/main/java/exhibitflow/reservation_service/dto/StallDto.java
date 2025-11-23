package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Stall data received from Stall Service
 * Includes spatial data for venue mapping
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
    private String code;
    private String description;
    
    // Spatial data for venue map (received from Stall service)
    private CoordinateDto location;  // Center point [longitude, latitude]
    private List<List<Double>> boundary;  // Polygon coordinates [[lng, lat], ...]
    
    /**
     * Coordinate DTO for point locations
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoordinateDto {
        private Double longitude;
        private Double latitude;
    }
}
