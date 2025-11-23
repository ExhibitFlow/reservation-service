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
@RequestMapping("/api/v1/venue")
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
     * Get venue map for a specific code
     */
    @GetMapping("/map/code/{code}")
    public ResponseEntity<VenueMapResponse> getVenueMapBycode(@PathVariable String code) {
        log.info("GET /api/venue/map/code/{} - Fetching venue map for code", code);
        VenueMapResponse response = venueMapService.getVenueMapByCode(code);
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
