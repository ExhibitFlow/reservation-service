package exhibitflow.reservation_service.controller;

import exhibitflow.reservation_service.dto.CreateReservationRequest;
import exhibitflow.reservation_service.dto.PagedResponse;
import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.dto.ReservationSummary;
import exhibitflow.reservation_service.service.ReservationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Reservation Controller for microservices architecture
 * Expects userId to be passed from API Gateway (extracted from JWT)
 * Version 1 API - supports pagination and enhanced features
 */
@RestController
@RequestMapping("/api/v1/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Create a new reservation
     * Creates a temporary lock (5 minutes) for payment
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
     * Complete payment for a pending reservation
     * Confirms reservation and generates QR code
     */
    @PostMapping("/{reservationId}/complete-payment")
    public ResponseEntity<ReservationResponse> completePayment(
            @PathVariable Long reservationId,
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        logger.info("Payment completion request for reservation: {} by user: {}", reservationId, userId);
        ReservationResponse response = reservationService.completePayment(reservationId, userId);
        return ResponseEntity.ok(response);
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
     * Get a specific reservation by ID
     */
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long reservationId,
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        logger.info("Fetching reservation {} for user: {}", reservationId, userId);
        ReservationResponse reservation = reservationService.getReservationById(reservationId, userId);
        return ResponseEntity.ok(reservation);
    }

    /**
     * Get all reservations (admin endpoint) with pagination
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ReservationSummary>> getAllReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        logger.info("Fetching all reservations (page={}, size={}, sortBy={}, direction={})", 
            page, size, sortBy, direction);
        
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        PagedResponse<ReservationSummary> reservations = reservationService.getAllReservations(pageable);
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

    /**
     * Get active reservation count for user
     */
    @GetMapping("/my/count")
    public ResponseEntity<Long> getActiveReservationCount(
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        logger.info("Fetching active reservation count for user: {}", userId);
        long count = reservationService.countActiveReservationsForUser(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Hold a stall temporarily
     */
    @PostMapping("/stalls/{stallId}/hold")
    public ResponseEntity<Void> holdStall(
            @PathVariable Long stallId,
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        logger.info("Hold stall request for stall {} by user: {}", stallId, userId);
        reservationService.holdStall(stallId, userId);
        return ResponseEntity.ok().build();
    }
}
