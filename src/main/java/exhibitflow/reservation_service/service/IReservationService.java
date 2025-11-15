package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.dto.CreateReservationRequest;
import exhibitflow.reservation_service.dto.PagedResponse;
import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.dto.ReservationSummary;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for reservation operations
 * Provides abstraction for easier testing and future implementations
 */
public interface IReservationService {

    /**
     * Creates a new reservation with payment lock
     */
    ReservationResponse createReservation(CreateReservationRequest request, Long userId);

    /**
     * Completes payment for a pending reservation
     */
    ReservationResponse completePayment(Long reservationId, Long userId);

    /**
     * Retrieves all reservations for a specific user
     */
    List<ReservationResponse> getUserReservations(Long userId);

    /**
     * Retrieves all reservations with pagination
     */
    PagedResponse<ReservationSummary> getAllReservations(Pageable pageable);

    /**
     * Retrieves a specific reservation by ID
     */
    ReservationResponse getReservationById(Long reservationId, Long userId);

    /**
     * Cancels a reservation
     */
    void cancelReservation(Long reservationId, Long userId);

    /**
     * Checks if a stall has active reservations
     */
    boolean hasActiveReservation(Long stallId);

    /**
     * Counts active reservations for a user
     */
    long countActiveReservationsForUser(Long userId);
}
