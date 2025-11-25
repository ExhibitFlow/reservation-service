package exhibitflow.reservation_service.client;

import exhibitflow.reservation_service.config.FeignClientConfig;
import exhibitflow.reservation_service.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = FeignClientConfig.class,
    path = "/api/v1/users"
)
public interface UserServiceClient {

    /**
     * Get user by ID from User Service
     */
    @GetMapping("/{userId}")
    UserDto getUserById(@PathVariable("userId") String userId);
}
