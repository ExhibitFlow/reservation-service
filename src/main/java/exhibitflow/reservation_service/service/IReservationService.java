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
    ReservationResponse createReservation(CreateReservationRequest request, String userId);

    /**
     * Completes payment for a pending reservation
     */
    ReservationResponse completePayment(Long reservationId, String userId);

    /**
     * Retrieves all reservations for a specific user
     */
    List<ReservationResponse> getUserReservations(String userId);

    /**
     * Retrieves all reservations with pagination
     */
    PagedResponse<ReservationSummary> getAllReservations(Pageable pageable);

    /**
     * Retrieves a specific reservation by ID
     */
    ReservationResponse getReservationById(Long reservationId, String userId);

    /**
     * Cancels a reservation
     */
    void cancelReservation(Long reservationId, String userId);

    /**
     * Checks if a stall has active reservations
     */
    boolean hasActiveReservation(Long stallId);

    /**
     * Counts active reservations for a user
     */
    long countActiveReservationsForUser(String userId);

    /**
     * Temporarily holds a stall for a user
     */
    void holdStall(Long stallId, String userId);
}
