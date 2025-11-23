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
        
        // Fetch all stalls from Stall service
        List<StallDto> stalls = stallServiceClient.getAllStalls();
        
        // Get reserved stall IDs from local reservations
        Set<Long> reservedStallIds = getReservedStallIds();
        
        // Convert to map DTOs and update reservation status
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
     * Get venue map filtered by code
     */
    @Transactional(readOnly = true)
    public VenueMapResponse getVenueMapByCode(String code) {
        log.info("Fetching venue map for code: {}", code);
        
        List<StallDto> stalls = stallServiceClient.getStallsByCode(code);
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
     * Get available stalls only
     */
    @Transactional(readOnly = true)
    public VenueMapResponse getAvailableStallsMap() {
        log.info("Fetching venue map with available stalls only");
        
        List<StallDto> allStalls = stallServiceClient.getAllStalls();
        Set<Long> reservedStallIds = getReservedStallIds();
        
        // Filter to only available stalls
        List<StallMapDto> stallMapDtos = allStalls.stream()
                .filter(stall -> !reservedStallIds.contains(stall.getId()))
                .map(stall -> convertToStallMapDto(stall, false))
                .collect(Collectors.toList());
        
        return VenueMapResponse.builder()
                .stalls(stallMapDtos)
                .bounds(calculateMapBounds(allStalls.stream()
                        .filter(s -> !reservedStallIds.contains(s.getId()))
                        .collect(Collectors.toList())))
                .statistics(calculateStatistics(allStalls, reservedStallIds))
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
     * Convert StallDto to StallMapDto
     * The reservation status is determined locally, overriding what Stall service reports
     */
    private StallMapDto convertToStallMapDto(StallDto stall, boolean isReserved) {
        StallMapDto.StallMapDtoBuilder builder = StallMapDto.builder()
                .id(stall.getId())
                .stallCode(stall.getStallCode())
                .size(stall.getSize())
                .price(stall.getPrice())
                .isReserved(isReserved)  // Use local reservation status
                .code(stall.getCode())
                .description(stall.getDescription())
                .boundary(stall.getBoundary());
        
        // Convert location if present
        if (stall.getLocation() != null) {
            builder.location(StallMapDto.CoordinateDto.builder()
                    .longitude(stall.getLocation().getLongitude())
                    .latitude(stall.getLocation().getLatitude())
                    .build());
        }
        
        return builder.build();
    }

    /**
     * Calculate map bounds from stalls
     */
    private VenueMapResponse.MapBounds calculateMapBounds(List<StallDto> stalls) {
        if (stalls.isEmpty()) {
            return null;
        }

        double minLng = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;

        for (StallDto stall : stalls) {
            if (stall.getLocation() != null) {
                double lng = stall.getLocation().getLongitude();
                double lat = stall.getLocation().getLatitude();
                
                minLng = Math.min(minLng, lng);
                maxLng = Math.max(maxLng, lng);
                minLat = Math.min(minLat, lat);
                maxLat = Math.max(maxLat, lat);
            }
        }

        return VenueMapResponse.MapBounds.builder()
                .minLongitude(minLng)
                .maxLongitude(maxLng)
                .minLatitude(minLat)
                .maxLatitude(maxLat)
                .build();
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
