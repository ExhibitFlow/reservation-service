package exhibitflow.reservation_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for creating topics and managing Kafka settings.
 */
@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.reservation-created}")
    private String reservationCreatedTopic;

    @Value("${kafka.topics.reservation-updated}")
    private String reservationUpdatedTopic;

    @Value("${kafka.topics.reservation-cancelled}")
    private String reservationCancelledTopic;

    @Value("${kafka.topics.payment-completed}")
    private String paymentCompletedTopic;

    @Value("${kafka.topics.payment-expired}")
    private String paymentExpiredTopic;

    /**
     * Topic for reservation created events.
     */
    @Bean
    public NewTopic reservationCreatedTopic() {
        return TopicBuilder.name(reservationCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for reservation updated events.
     */
    @Bean
    public NewTopic reservationUpdatedTopic() {
        return TopicBuilder.name(reservationUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for reservation cancelled events.
     */
    @Bean
    public NewTopic reservationCancelledTopic() {
        return TopicBuilder.name(reservationCancelledTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for payment completed events.
     */
    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(paymentCompletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for payment expired events.
     */
    @Bean
    public NewTopic paymentExpiredTopic() {
        return TopicBuilder.name(paymentExpiredTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
