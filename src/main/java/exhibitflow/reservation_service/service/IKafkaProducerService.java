package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.dto.*;

/**
 * Interface for publishing reservation events to Kafka topics.
 */
public interface IKafkaProducerService {
    
    /**
     * Publish a reservation created event.
     *
     * @param event the reservation created event
     */
    void publishReservationCreated(ReservationCreatedEvent event);
    
    /**
     * Publish a reservation updated event.
     *
     * @param event the reservation updated event
     */
    void publishReservationUpdated(ReservationUpdatedEvent event);
    
    /**
     * Publish a reservation cancelled event.
     *
     * @param event the reservation cancelled event
     */
    void publishReservationCancelled(ReservationCancelledEvent event);
    
    /**
     * Publish a payment completed event.
     *
     * @param event the payment completed event
     */
    void publishPaymentCompleted(PaymentCompletedEvent event);
    
    /**
     * Publish a payment expired event.
     *
     * @param event the payment expired event
     */
    void publishPaymentExpired(PaymentExpiredEvent event);
}
