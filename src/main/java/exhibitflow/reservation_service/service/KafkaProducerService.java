package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService implements IKafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

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

    @Value("${kafka.topics.stall-reserved}")
    private String stallReservedTopic;

    @Override
    public void publishReservationCreated(ReservationCreatedEvent event) {
        log.info("Publishing reservation created event: {}", event);
        Message<ReservationCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, reservationCreatedTopic)
                .setHeader(KafkaHeaders.KEY, String.valueOf(event.getReservationId()))
                .build();
        
        kafkaTemplate.send(message);
        log.debug("Reservation created event published successfully to topic: {}", reservationCreatedTopic);
    }

    @Override
    public void publishReservationUpdated(ReservationUpdatedEvent event) {
        log.info("Publishing reservation updated event: {}", event);
        Message<ReservationUpdatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, reservationUpdatedTopic)
                .setHeader(KafkaHeaders.KEY, String.valueOf(event.getReservationId()))
                .build();
        
        kafkaTemplate.send(message);
        log.debug("Reservation updated event published successfully to topic: {}", reservationUpdatedTopic);
    }

    @Override
    public void publishReservationCancelled(ReservationCancelledEvent event) {
        log.info("Publishing reservation cancelled event: {}", event);
        Message<ReservationCancelledEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, reservationCancelledTopic)
                .setHeader(KafkaHeaders.KEY, String.valueOf(event.getReservationId()))
                .build();
        
        kafkaTemplate.send(message);
        log.debug("Reservation cancelled event published successfully to topic: {}", reservationCancelledTopic);
    }

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing payment completed event: {}", event);
        Message<PaymentCompletedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, paymentCompletedTopic)
                .setHeader(KafkaHeaders.KEY, String.valueOf(event.getReservationId()))
                .build();
        
        kafkaTemplate.send(message);
        log.debug("Payment completed event published successfully to topic: {}", paymentCompletedTopic);
    }

    @Override
    public void publishPaymentExpired(PaymentExpiredEvent event) {
        log.info("Publishing payment expired event: {}", event);
        Message<PaymentExpiredEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, paymentExpiredTopic)
                .setHeader(KafkaHeaders.KEY, String.valueOf(event.getReservationId()))
                .build();
        
        kafkaTemplate.send(message);
        log.debug("Payment expired event published successfully to topic: {}", paymentExpiredTopic);
    }

    @Override
    public void publishStallReserved(StallReservedEvent event) {
        log.info("Publishing stall reserved event: {}", event);
        Message<StallReservedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, stallReservedTopic)
                .setHeader(KafkaHeaders.KEY, event.getPayload().getReservationId())
                .build();
        
        kafkaTemplate.send(message);
        log.debug("Stall reserved event published successfully to topic: {}", stallReservedTopic);
    }
}
