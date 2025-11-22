package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for venue map with all stalls and their availability
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueMapResponse {
    private List<StallMapDto> stalls;
    private MapBounds bounds;
    private MapStatistics statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MapBounds {
        private Double minLongitude;
        private Double maxLongitude;
        private Double minLatitude;
        private Double maxLatitude;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MapStatistics {
        private Integer totalStalls;
        private Integer availableStalls;
        private Integer reservedStalls;
        private List<String> zones;
        private List<Integer> floors;
    }
}
