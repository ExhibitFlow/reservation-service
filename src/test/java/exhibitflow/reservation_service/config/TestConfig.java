package exhibitflow.reservation_service.config;

import exhibitflow.reservation_service.client.StallServiceClient;
import exhibitflow.reservation_service.client.UserServiceClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for mocking external service clients
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public UserServiceClient userServiceClient() {
        return mock(UserServiceClient.class);
    }

    @Bean
    @Primary
    public StallServiceClient stallServiceClient() {
        return mock(StallServiceClient.class);
    }
}
