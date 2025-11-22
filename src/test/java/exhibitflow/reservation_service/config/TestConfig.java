package exhibitflow.reservation_service.config;

import exhibitflow.reservation_service.service.IKafkaProducerService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Test configuration for integration tests
 * Mocks are provided via @MockBean in test classes
 * Kafka components are mocked to avoid requiring a running Kafka instance for tests
 */
@TestConfiguration
public class TestConfig {
    
    /**
     * Mock Kafka producer service to avoid needing Kafka for tests
     */
    @MockBean
    private IKafkaProducerService kafkaProducerService;
    
    /**
     * Mock Kafka admin to prevent topic creation during tests
     */
    @MockBean
    private KafkaAdmin kafkaAdmin;
}
