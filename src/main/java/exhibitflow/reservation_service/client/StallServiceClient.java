package exhibitflow.reservation_service.client;

import exhibitflow.reservation_service.config.FeignClientConfig;
import exhibitflow.reservation_service.dto.StallDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

/**
 * Feign client for communicating with Stall Service
 */
@FeignClient(
    name = "stall-service",
    url = "${services.stall-service.url}",
    configuration = FeignClientConfig.class
)
public interface StallServiceClient {

    /**
     * Get stall by ID from Stall Service
     */
    @GetMapping("/api/stalls/{stallId}")
    StallDto getStallById(@PathVariable("stallId") Long stallId);

    /**
     * Reserve a stall (mark as reserved)
     */
    @PutMapping("/api/stalls/{stallId}/reserve")
    @CacheEvict(value = "stalls", key = "#stallId")
    StallDto reserveStall(@PathVariable("stallId") Long stallId);

    /**
     * Release a stall reservation (mark as available)
     */
    @PutMapping("/api/stalls/{stallId}/release")
    @CacheEvict(value = "stalls", key = "#stallId")
    StallDto releaseStall(@PathVariable("stallId") Long stallId);
    
    /**
     * Get all stalls (for venue map)
     */
    @GetMapping("/api/stalls")
    List<StallDto> getAllStalls();
    
    /**
     * Get stalls by floor
     */
    @GetMapping("/api/stalls/floor/{floorNumber}")
    List<StallDto> getStallsByFloor(@PathVariable("floorNumber") Integer floorNumber);
    
    /**
     * Get stalls by zone
     */
    @GetMapping("/api/stalls/zone/{zone}")
    List<StallDto> getStallsByZone(@PathVariable("zone") String zone);
}
