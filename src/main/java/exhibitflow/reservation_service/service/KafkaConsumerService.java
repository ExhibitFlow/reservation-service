package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class KafkaConsumerService {

    /**
     * Listen to reservation created events.
     * Exceptions are handled by the configured error handler with retry and DLQ
     */
    @KafkaListener(
        topics = "${kafka.topics.reservation-created}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReservationCreated(
            @Payload ReservationCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        
        log.info("Received reservation created event from topic '{}' with key '{}': {}", topic, key, event);
        
        // Example processing: You could send notifications, update analytics, etc.
        log.info("Processing reservation created for user {} - Stall: {}, Payment deadline: {}", 
            event.getUserId(), 
            event.getStallId(),
            event.getPaymentDeadline());
    }


    @KafkaListener(
        topics = "${kafka.topics.payment-completed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(
            @Payload PaymentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        
        log.info("Received payment completed event from topic '{}' with key '{}': {}", topic, key, event);
        
        // Example processing: Send confirmation email, update billing system, etc.
        log.info("Payment completed for reservation {} - Amount: {}, Method: {}", 
            event.getReservationId(),
            event.getAmount(),
            event.getPaymentMethod());
    }

   
    @KafkaListener(
        topics = "${kafka.topics.reservation-cancelled}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReservationCancelled(
            @Payload ReservationCancelledEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        
        log.info("Received reservation cancelled event from topic '{}' with key '{}': {}", topic, key, event);
        
        // Example processing: Send cancellation notification, update analytics, etc.
        log.info("Reservation {} cancelled - Reason: {}", 
            event.getReservationId(),
            event.getCancellationReason());
    }

   
    @KafkaListener(
        topics = "${kafka.topics.payment-expired}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentExpired(
            @Payload PaymentExpiredEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        
        log.info("Received payment expired event from topic '{}' with key '{}': {}", topic, key, event);
        
        // Example processing: Send reminder notification, update metrics, etc.
        log.info("Payment expired for reservation {} - Deadline was: {}", 
            event.getReservationId(),
            event.getPaymentDeadline());
    }
}
