package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.client.StallServiceClient;
import exhibitflow.reservation_service.dto.StallDto;
import exhibitflow.reservation_service.dto.StallMapDto;
import exhibitflow.reservation_service.dto.VenueMapResponse;
import exhibitflow.reservation_service.entity.Reservation;
import exhibitflow.reservation_service.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for venue map operations
 * Fetches stall data from Stall service and combines with reservation status
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VenueMapService {

    private final StallServiceClient stallServiceClient;
    private final ReservationRepository reservationRepository;

    /**
     * Get complete venue map with all stalls and their reservation status
     */
    @Transactional(readOnly = true)
    public VenueMapResponse getVenueMap() {
        log.info("Fetching venue map with all stalls from Stall service");
        
        List<StallDto> stalls = fetchAllStalls();
        return buildVenueMapResponse(stalls);
    }

    /**
     * Get venue map filtered by code
     */
    @Transactional(readOnly = true)
    public VenueMapResponse getVenueMapByCode(String code) {
        log.info("Fetching venue map for code: {}", code);
        
        StallDto stall = stallServiceClient.getStallByCode(code);
        List<StallDto> stalls = stall != null ? List.of(stall) : List.of();
        return buildVenueMapResponse(stalls);
    }

    /**
     * Fetch all stalls from the Stall service, handling pagination
     */
    private List<StallDto> fetchAllStalls() {
        List<StallDto> allStalls = new java.util.ArrayList<>();
        int page = 0;
        int pageSize = 100; // Fetch 100 stalls per page
        boolean hasMorePages = true;

        while (hasMorePages) {
            var pagedResponse = stallServiceClient.getAllStalls(page, pageSize);
            allStalls.addAll(pagedResponse.getContent());

            log.debug("Fetched page {} with {} stalls. Total so far: {}",
                     page, pagedResponse.getContent().size(), allStalls.size());

            hasMorePages = !pagedResponse.isLast();
            page++;
        }

        log.info("Fetched total of {} stalls from Stall service", allStalls.size());
        return allStalls;
    }

    /**
     * Get available stalls only
     */
    @Transactional(readOnly = true)
    public VenueMapResponse getAvailableStallsMap() {
        log.info("Fetching venue map with available stalls only");
        
        List<StallDto> allStalls = fetchAllStalls();
        Set<Long> reservedStallIds = getReservedStallIds();
        
        // Filter to only available stalls
        List<StallDto> availableStalls = allStalls.stream()
                .filter(stall -> !reservedStallIds.contains(stall.getId()))
                .collect(Collectors.toList());
        
        return buildVenueMapResponse(availableStalls);
    }

    /**
     * Build VenueMapResponse from stall list
     */
    private VenueMapResponse buildVenueMapResponse(List<StallDto> stalls) {
        Set<Long> reservedStallIds = getReservedStallIds();
        
        List<StallMapDto> stallMapDtos = stalls.stream()
                .map(stall -> convertToStallMapDto(stall, reservedStallIds.contains(stall.getId())))
                .collect(Collectors.toList());
        
        return VenueMapResponse.builder()
                .stalls(stallMapDtos)
                .bounds(calculateMapBounds(stalls))
                .statistics(calculateStatistics(stalls, reservedStallIds))
                .build();
    }

    /**
     * Get reserved stall IDs from local reservation database
     */
    private Set<Long> getReservedStallIds() {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CONFIRMED ||
                            r.getStatus() == Reservation.ReservationStatus.PENDING_PAYMENT)
                .map(Reservation::getStallId)
                .collect(Collectors.toSet());
    }

    /**
     * Convert StallDto to StallMapDto for map display
     * The reservation status is determined locally, overriding what Stall service reports
     */
    private StallMapDto convertToStallMapDto(StallDto stall, boolean isReserved) {
        return StallMapDto.builder()
                .id(stall.getId())
                .stallCode(stall.getCode())
                .size(stall.getSize())
                .price(stall.getPrice() != null ? stall.getPrice().doubleValue() : 0.0)
                .isReserved(isReserved)  // Use local reservation status
                .code(stall.getCode())
                .description(null)  // No longer provided by Stall Service
                .boundary(null)  // No longer provided by Stall Service
                .location(null)  // Location is now a string in StallDto, not coordinates for map
                .build();
    }

    /**
     * Calculate map bounds from stalls
     * Note: Since location is now a string (e.g., "Hall A - North Wing, Row 1"),
     * we cannot calculate geographic bounds. This would need to be updated if
     * boundary coordinates are used instead.
     */
    private VenueMapResponse.MapBounds calculateMapBounds(List<StallDto> stalls) {
        if (stalls.isEmpty()) {
            return null;
        }

        // TODO: Calculate bounds from boundary polygons if needed
        // For now, return null since we don't have coordinate-based locations
        return null;
    }

    /**
     * Calculate statistics for the map
     */
    private VenueMapResponse.MapStatistics calculateStatistics(List<StallDto> stalls, Set<Long> reservedStallIds) {
        int total = stalls.size();
        int reserved = (int) stalls.stream()
                .filter(s -> reservedStallIds.contains(s.getId()))
                .count();
        int available = total - reserved;

        List<String> codes = stalls.stream()
                .map(StallDto::getCode)
                .distinct()
                .sorted()
                .collect(Collectors.toList());


        return VenueMapResponse.MapStatistics.builder()
                .totalStalls(total)
                .availableStalls(available)
                .reservedStalls(reserved)
                .codes(codes)
                .build();
    }
}
