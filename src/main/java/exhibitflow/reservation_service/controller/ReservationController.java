package exhibitflow.reservation_service.controller;

import exhibitflow.reservation_service.constants.HeaderConstants;
import exhibitflow.reservation_service.dto.CreateReservationRequest;
import exhibitflow.reservation_service.dto.PagedResponse;
import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.dto.ReservationSummary;
import exhibitflow.reservation_service.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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


@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Endpoints for managing stall reservations")
@SecurityRequirement(name = "bearer-jwt")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }


    @PostMapping
    @Operation(
        summary = "Create a new reservation",
        description = "Creates a reservation for a stall. User can reserve up to 3 stalls. Requires USER/VIEWER role or higher."
    )
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @RequestHeader(value = HeaderConstants.USER_ID, required = true) String userId
    ) {
        logger.info("Reservation request received for user: {}", userId);
        ReservationResponse response = reservationService.createReservation(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @PostMapping("/{reservationId}/complete-payment")
    @Operation(
        summary = "Complete payment for reservation",
        description = "Confirms a pending reservation and generates QR code. Only the reservation owner can complete payment. Requires USER/VIEWER role or higher."
    )
    public ResponseEntity<ReservationResponse> completePayment(
            @PathVariable Long reservationId,
            @RequestHeader(value = HeaderConstants.USER_ID, required = true) String userId
    ) {
        logger.info("Payment completion request for reservation: {} by user: {}", reservationId, userId);
        ReservationResponse response = reservationService.completePayment(reservationId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all reservations for the authenticated user
     */
    @GetMapping("/my")
    @Operation(
        summary = "Get my reservations",
        description = "Retrieves all reservations for the authenticated user. Requires USER/VIEWER role or higher."
    )
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @RequestHeader(value = HeaderConstants.USER_ID, required = true) String userId
    ) {
        logger.info("Fetching reservations for user: {}", userId);
        List<ReservationResponse> reservations = reservationService.getUserReservations(userId);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Get a specific reservation by ID
     */
    @GetMapping("/{reservationId}")
    @Operation(
        summary = "Get reservation by ID",
        description = "Retrieves a specific reservation by ID. Only the reservation owner or ADMIN can view. Requires USER/VIEWER role or higher."
    )
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long reservationId,
            @RequestHeader(value = HeaderConstants.USER_ID, required = true) String userId
    ) {
        logger.info("Fetching reservation {} for user: {}", reservationId, userId);
        ReservationResponse reservation = reservationService.getReservationById(reservationId, userId);
        return ResponseEntity.ok(reservation);
    }

    /**
     * Get all reservations (admin endpoint) with pagination
     */
    @GetMapping
    @Operation(
        summary = "Get all reservations (Admin/Manager)",
        description = "Retrieves all reservations with pagination and sorting. This endpoint is intended for ADMIN and MANAGER roles only."
    )
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
    @Operation(
        summary = "Cancel a reservation",
        description = "Cancels a reservation and releases the stall. Only the reservation owner can cancel. Requires USER/VIEWER role or higher."
    )
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long reservationId,
            @RequestHeader(value = HeaderConstants.USER_ID, required = true) String userId
    ) {
        logger.info("Cancelling reservation {} for user: {}", reservationId, userId);
        reservationService.cancelReservation(reservationId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get active reservation count for user
     */
    @GetMapping("/my/count")
    @Operation(
        summary = "Get active reservation count",
        description = "Returns the number of active reservations for the authenticated user. Requires USER/VIEWER role or higher."
    )
    public ResponseEntity<Long> getActiveReservationCount(
            @RequestHeader(value = HeaderConstants.USER_ID, required = true) String userId
    ) {
        logger.info("Fetching active reservation count for user: {}", userId);
        long count = reservationService.countActiveReservationsForUser(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Hold a stall temporarily
     */
    @PostMapping("/stalls/{stallId}/hold")
    @Operation(
        summary = "Hold a stall temporarily",
        description = "Temporarily holds a stall for the user before creating a reservation. Requires USER/VIEWER role or higher."
    )
    public ResponseEntity<Void> holdStall(
            @PathVariable Long stallId,
            @RequestHeader(value = HeaderConstants.USER_ID, required = true) String userId
    ) {
        logger.info("Hold stall request for stall {} by user: {}", stallId, userId);
        reservationService.holdStall(stallId, userId);
        return ResponseEntity.ok().build();
    }
}
