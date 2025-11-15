package exhibitflow.reservation_service.client;

import exhibitflow.reservation_service.config.FeignClientConfig;
import exhibitflow.reservation_service.dto.UserDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Feign client for communicating with User Service
 */
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = FeignClientConfig.class
)
@RequestMapping("/api/users")
public interface UserServiceClient {

    /**
     * Get user by ID from User Service
     */
    @GetMapping("/{userId}")
    @Cacheable(value = "users", key = "#userId")
    UserDto getUserById(@PathVariable("userId") Long userId);
}
