package exhibitflow.reservation_service.client;

import exhibitflow.reservation_service.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for communicating with User Service
 */
@Component
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${services.user-service.url:http://localhost:8081}") String userServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    /**
     * Get user by ID from User Service
     */
    public UserDto getUserById(Long userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            logger.debug("Fetching user from User Service: {}", url);
            return restTemplate.getForObject(url, UserDto.class);
        } catch (Exception e) {
            logger.error("Failed to fetch user with ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to fetch user from User Service", e);
        }
    }

    /**
     * Validate user exists and is active
     */
    public boolean validateUser(Long userId) {
        try {
            UserDto user = getUserById(userId);
            return user != null;
        } catch (Exception e) {
            return false;
        }
    }
}
