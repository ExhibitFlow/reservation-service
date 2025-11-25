package exhibitflow.reservation_service.controller;

import exhibitflow.reservation_service.dto.VenueMapResponse;
import exhibitflow.reservation_service.service.VenueMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/venue/map")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Venue Map", description = "Endpoints for viewing venue maps and stall availability")
@SecurityRequirement(name = "bearer-jwt")
public class VenueMapController {

    private final VenueMapService venueMapService;


    @GetMapping
    @Operation(
        summary = "Get complete venue map",
        description = "Retrieves the complete venue map showing all stalls with their availability status. Accessible to all authenticated users."
    )
    public ResponseEntity<VenueMapResponse> getVenueMap() {
        log.info("Fetching complete venue map");
        return buildResponse(venueMapService::getVenueMap);
    }

    /**
     * Get venue map for a specific code
     */
    @GetMapping("/code/{code}")
    @Operation(
        summary = "Get venue map by code",
        description = "Retrieves venue map for a specific venue code. Accessible to all authenticated users."
    )
    public ResponseEntity<VenueMapResponse> getVenueMapBycode(@PathVariable String code) {
        log.info("Fetching venue map for code: {}", code);
        return buildResponse(() -> venueMapService.getVenueMapByCode(code));
    }

    /**
     * Get only available (unreserved) stalls on the map
     */
    @GetMapping("/available")
    @Operation(
        summary = "Get available stalls map",
        description = "Retrieves only the available (unreserved) stalls on the venue map. Useful for users looking to make reservations. Accessible to all authenticated users."
    )
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
