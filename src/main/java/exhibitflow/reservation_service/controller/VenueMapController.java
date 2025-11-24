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
@RequestMapping("/api/v1/venue/map")
@RequiredArgsConstructor
@Slf4j
public class VenueMapController {

    private final VenueMapService venueMapService;

    /**
     * Get complete venue map with all stalls
     * Shows available stalls and reserved stalls with spatial data
     */
    @GetMapping
    public ResponseEntity<VenueMapResponse> getVenueMap() {
        log.info("Fetching complete venue map");
        return buildResponse(venueMapService::getVenueMap);
    }

    /**
     * Get venue map for a specific code
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<VenueMapResponse> getVenueMapBycode(@PathVariable String code) {
        log.info("Fetching venue map for code: {}", code);
        return buildResponse(() -> venueMapService.getVenueMapByCode(code));
    }

    /**
     * Get only available (unreserved) stalls on the map
     */
    @GetMapping("/available")
    public ResponseEntity<VenueMapResponse> getAvailableStallsMap() {
        log.info("Fetching available stalls map");
        return buildResponse(venueMapService::getAvailableStallsMap);
    }

    /**
     * Helper method to build ResponseEntity from service call
     */
    private ResponseEntity<VenueMapResponse> buildResponse(java.util.function.Supplier<VenueMapResponse> serviceCall) {
        return ResponseEntity.ok(serviceCall.get());
    }
}
