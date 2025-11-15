package exhibitflow.reservation_service.client;

import exhibitflow.reservation_service.config.FeignClientConfig;
import exhibitflow.reservation_service.dto.StallDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Feign client for communicating with Stall Service
 */
@FeignClient(
    name = "stall-service",
    url = "${services.stall-service.url}",
    configuration = FeignClientConfig.class
)
RequestMapping("/api/stalls")
public interface StallServiceClient {

    /**
     * Get stall by ID from Stall Service
     */
    @GetMapping("/{stallId}")
    @Cacheable(value = "stalls", key = "#stallId")
    StallDto getStallById(@PathVariable("stallId") Long stallId);

    /**
     * Reserve a stall (mark as reserved)
     */
    @PutMapping("/{stallId}/reserve")
    @CacheEvict(value = "stalls", key = "#stallId")
    StallDto reserveStall(@PathVariable("stallId") Long stallId);

    /**
     * Release a stall reservation (mark as available)
     */
    @PutMapping("/{stallId}/release")
    @CacheEvict(value = "stalls", key = "#stallId")
    StallDto releaseStall(@PathVariable("stallId") Long stallId);
}
