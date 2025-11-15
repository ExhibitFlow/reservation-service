package exhibitflow.reservation_service.client;

import exhibitflow.reservation_service.dto.StallDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Feign client for communicating with Stall Service
 */
@FeignClient(
    name = "stall-service",
    url = "${services.stall-service.url}"
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
    StallDto reserveStall(@PathVariable("stallId") Long stallId);

    /**
     * Release a stall reservation (mark as available)
     */
    @PutMapping("/api/stalls/{stallId}/release")
    StallDto releaseStall(@PathVariable("stallId") Long stallId);
}
