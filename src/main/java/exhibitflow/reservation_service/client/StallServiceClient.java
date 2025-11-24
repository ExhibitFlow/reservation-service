package exhibitflow.reservation_service.client;

import exhibitflow.reservation_service.config.FeignClientConfig;
import exhibitflow.reservation_service.dto.PagedResponse;
import exhibitflow.reservation_service.dto.StallDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for communicating with Stall Service
 */
@FeignClient(
    name = "stall-service",
    url = "${services.stall-service.url}",
    configuration = FeignClientConfig.class,
    path = "/api/stalls"
)
public interface StallServiceClient {

    /**
     * Get stall by ID from Stall Service
     */
    @GetMapping("/{stallId}")
    StallDto getStallById(@PathVariable("stallId") Long stallId);

    /**
     * Reserve a stall (mark as reserved)
     */
    @PutMapping("/{stallId}/reserve")
    StallDto reserveStall(@PathVariable("stallId") Long stallId);

    /**
     * Hold a stall (temporarily hold for reservation)
     */
    @PutMapping("/{stallId}/hold")
    StallDto holdStall(@PathVariable("stallId") Long stallId);

    /**
     * Release a stall reservation (mark as available)
     */
    @PutMapping("/{stallId}/release")
    StallDto releaseStall(@PathVariable("stallId") Long stallId);
    
    /**
     * Get all stalls (for venue map) with pagination support
     */
    @GetMapping
    PagedResponse<StallDto> getAllStalls(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "1000") int size
    );


    /**
     * Get stalls by code
     */
    @GetMapping("/code/{code}")
    List<StallDto> getStallsByCode(@PathVariable("code") String code);
}
