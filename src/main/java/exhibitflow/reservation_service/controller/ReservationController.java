package exhibitflow.reservation_service.controller;

import exhibitflow.reservation_service.dto.CreateReservationRequest;
import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.service.ReservationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Reservation Controller for microservices architecture
 * Expects userId to be passed from API Gateway (extracted from JWT)
 */
@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    @Autowired
    private ReservationService reservationService;

    /**
     * Create a new reservation
     * @param request Reservation request
     * @param userId User ID from API Gateway (via header or path variable)
     */
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        logger.info("Reservation request received for user: {}", userId);
        ReservationResponse response = reservationService.createReservation(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all reservations for the authenticated user
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        logger.info("Fetching reservations for user: {}", userId);
        List<ReservationResponse> reservations = reservationService.getUserReservations(userId);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Get all reservations (admin endpoint)
     */
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        logger.info("Fetching all reservations");
        List<ReservationResponse> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    /**
     * Cancel a reservation
     */
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long reservationId,
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        logger.info("Cancelling reservation {} for user: {}", reservationId, userId);
        reservationService.cancelReservation(reservationId, userId);
        return ResponseEntity.noContent().build();
    }
}
