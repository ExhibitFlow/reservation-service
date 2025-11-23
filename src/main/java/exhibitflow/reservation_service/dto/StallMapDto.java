package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for displaying stall on venue map with spatial data
 */
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
    
    // Spatial data as GeoJSON-compatible structures
    private CoordinateDto location;  // Center point
    private List<List<Double>> boundary;  // Polygon coordinates
    
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
