package exhibitflow.reservation_service.controller;

import exhibitflow.reservation_service.dto.VenueMapResponse;
import exhibitflow.reservation_service.service.VenueMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for venue map operations
 * Provides endpoints to retrieve exhibition venue maps with stall availability
 */
@RestController
@RequestMapping("/api/venue")
@RequiredArgsConstructor
@Slf4j
public class VenueMapController {

    private final VenueMapService venueMapService;

    /**
     * Get complete venue map with all stalls
     * Shows available stalls and reserved stalls with spatial data
     */
    @GetMapping("/map")
    public ResponseEntity<VenueMapResponse> getVenueMap() {
        log.info("GET /api/venue/map - Fetching complete venue map");
        VenueMapResponse response = venueMapService.getVenueMap();
        return ResponseEntity.ok(response);
    }

    /**
     * Get venue map for a specific floor
     */
    @GetMapping("/map/floor/{floorNumber}")
    public ResponseEntity<VenueMapResponse> getVenueMapByFloor(@PathVariable Integer floorNumber) {
        log.info("GET /api/venue/map/floor/{} - Fetching venue map for floor", floorNumber);
        VenueMapResponse response = venueMapService.getVenueMapByFloor(floorNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Get venue map for a specific zone
     */
    @GetMapping("/map/zone/{zone}")
    public ResponseEntity<VenueMapResponse> getVenueMapByZone(@PathVariable String zone) {
        log.info("GET /api/venue/map/zone/{} - Fetching venue map for zone", zone);
        VenueMapResponse response = venueMapService.getVenueMapByZone(zone);
        return ResponseEntity.ok(response);
    }

    /**
     * Get only available (unreserved) stalls on the map
     */
    @GetMapping("/map/available")
    public ResponseEntity<VenueMapResponse> getAvailableStallsMap() {
        log.info("GET /api/venue/map/available - Fetching available stalls map");
        VenueMapResponse response = venueMapService.getAvailableStallsMap();
        return ResponseEntity.ok(response);
    }
}
