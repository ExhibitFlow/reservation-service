package exhibitflow.reservation_service.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for creating topics, managing Kafka settings, and implementing retry/DLQ strategy.
 */
@Configuration
@Slf4j
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

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${kafka.consumer.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${kafka.consumer.retry.backoff-interval:2000}")
    private long backoffInterval;

    private static final int DEFAULT_PARTITIONS = 3;
    private static final int DEFAULT_REPLICAS = 1;

    /**
     * Topic for reservation created events.
     */
    @Bean
    public NewTopic reservationCreatedTopic() {
        return createTopic(reservationCreatedTopic);
    }

    /**
     * Topic for reservation updated events.
     */
    @Bean
    public NewTopic reservationUpdatedTopic() {
        return createTopic(reservationUpdatedTopic);
    }

    /**
     * Topic for reservation cancelled events.
     */
    @Bean
    public NewTopic reservationCancelledTopic() {
        return createTopic(reservationCancelledTopic);
    }

    /**
     * Topic for payment completed events.
     */
    @Bean
    public NewTopic paymentCompletedTopic() {
        return createTopic(paymentCompletedTopic);
    }

    /**
     * Topic for payment expired events.
     */
    @Bean
    public NewTopic paymentExpiredTopic() {
        return createTopic(paymentExpiredTopic);
    }

    /**
     * Dead Letter Queue topic for failed messages.
     */
    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name("reservation-service.DLQ")
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    /**
     * Consumer factory with error handling deserializers.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // Use ErrorHandlingDeserializer to handle deserialization errors gracefully
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.lang.Object");
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka listener container factory with retry and DLQ configuration.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate));
        
        // Enable batch listening if needed
        factory.setBatchListener(false);
        
        return factory;
    }

    /**
     * Error handler with retry logic and DLQ publishing.
     * Messages are retried based on configured attempts and backoff interval.
     * After max retries, messages are sent to the Dead Letter Queue.
     */
    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // DeadLetterPublishingRecoverer sends failed messages to DLQ
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (consumerRecord, exception) -> {
                    log.error("Message processing failed after {} retries. Sending to DLQ. Topic: {}, Partition: {}, Offset: {}, Error: {}",
                            maxRetryAttempts,
                            consumerRecord.topic(),
                            consumerRecord.partition(),
                            consumerRecord.offset(),
                            exception.getMessage());
                    return new org.apache.kafka.common.TopicPartition("reservation-service.DLQ", consumerRecord.partition());
                });

        // DefaultErrorHandler provides retry mechanism with backoff
        // FixedBackOff: interval in ms, maxAttempts (-1 because first attempt is not a retry)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(backoffInterval, maxRetryAttempts - 1));

        // Log each retry attempt
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> 
                log.warn("Retry attempt {} for message from topic: {}, partition: {}, offset: {}. Error: {}",
                        deliveryAttempt,
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        ex.getMessage()));

        // Don't retry for certain exceptions (add specific exceptions as needed)
        // errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }

    /**
     * Creates a Kafka topic with default partitions and replicas.
     */
    private NewTopic createTopic(String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }
}
