package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.client.QRCodeServiceClient;
import exhibitflow.reservation_service.client.StallServiceClient;
import exhibitflow.reservation_service.client.UserServiceClient;
import exhibitflow.reservation_service.dto.CreateReservationRequest;
import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.dto.StallDto;
import exhibitflow.reservation_service.dto.UserDto;
import exhibitflow.reservation_service.entity.Reservation;
import exhibitflow.reservation_service.exception.ReservationLimitExceededException;
import exhibitflow.reservation_service.exception.ResourceNotFoundException;
import exhibitflow.reservation_service.exception.StallNotAvailableException;
import exhibitflow.reservation_service.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Reservation Service for microservices architecture
 * Communicates with User, Stall, and QRCode services via REST
 */
@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final int MAX_RESERVATIONS_PER_USER = 3;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private StallServiceClient stallServiceClient;

    @Autowired
    private QRCodeServiceClient qrCodeServiceClient;

    /**
     * Creates a new reservation with transactional safety
     * Communicates with User Service, Stall Service, and QRCode Service
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, Long userId) {
        logger.info("Creating reservation for user: {} and stall: {}", userId, request.getStallId());

        // Validate user exists via User Service
        UserDto user = userServiceClient.getUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        // Check if user has reached reservation limit
        long userReservationCount = reservationRepository.countByUserIdAndStatus(
            userId, 
            Reservation.ReservationStatus.CONFIRMED
        );
        if (userReservationCount >= MAX_RESERVATIONS_PER_USER) {
            logger.error("User {} has reached maximum reservation limit of {}", userId, MAX_RESERVATIONS_PER_USER);
            throw new ReservationLimitExceededException(
                String.format("Maximum reservation limit of %d stalls per business has been reached", MAX_RESERVATIONS_PER_USER)
            );
        }

        // Get stall details from Stall Service
        StallDto stall = stallServiceClient.getStallById(request.getStallId());
        if (stall == null) {
            throw new ResourceNotFoundException("Stall not found with ID: " + request.getStallId());
        }

        // Check if stall is already reserved
        if (stall.getIsReserved()) {
            logger.error("Stall {} is already reserved", stall.getStallCode());
            throw new StallNotAvailableException(
                String.format("Stall %s is already reserved", stall.getStallCode())
            );
        }

        // Reserve the stall via Stall Service
        boolean reserved = stallServiceClient.reserveStall(request.getStallId());
        if (!reserved) {
            throw new StallNotAvailableException("Failed to reserve stall. Please try again.");
        }

        try {
            // Create reservation
            Reservation reservation = Reservation.builder()
                .userId(userId)
                .stallId(request.getStallId())
                .status(Reservation.ReservationStatus.CONFIRMED)
                .build();

            // Save reservation to get ID
            reservation = reservationRepository.save(reservation);

            // Generate QR code via QRCode Service
            String qrCodeBase64 = qrCodeServiceClient.generateQRCode(
                reservation.getId(),
                user.getName(),
                stall.getStallCode()
            );

            // Update reservation with QR code
            if (qrCodeBase64 != null) {
                reservation.setQrCodeBase64(qrCodeBase64);
                reservation = reservationRepository.save(reservation);
            }

            logger.info("Reservation created successfully with ID: {}", reservation.getId());

            return mapToReservationResponse(reservation, user, stall);
        } catch (Exception e) {
            // Rollback: Release the stall if reservation creation fails
            logger.error("Failed to create reservation, releasing stall: {}", e.getMessage());
            stallServiceClient.releaseStall(request.getStallId());
            throw e;
        }
    }

    /**
     * Get all reservations for a specific user
     */
    public List<ReservationResponse> getUserReservations(Long userId) {
        logger.debug("Fetching reservations for user: {}", userId);
        
        // Validate user exists
        UserDto user = userServiceClient.getUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        return reservations.stream()
            .map(this::mapToReservationResponseWithExternalData)
            .collect(Collectors.toList());
    }

    /**
     * Get all reservations (admin function)
     */
    public List<ReservationResponse> getAllReservations() {
        logger.debug("Fetching all reservations");
        List<Reservation> reservations = reservationRepository.findAll();
        return reservations.stream()
            .map(this::mapToReservationResponseWithExternalData)
            .collect(Collectors.toList());
    }

    /**
     * Cancel a reservation
     */
    @Transactional
    public void cancelReservation(Long reservationId, Long userId) {
        logger.info("Cancelling reservation: {} for user: {}", reservationId, userId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        // Verify the reservation belongs to the user
        if (!reservation.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This reservation does not belong to you");
        }

        // Update status
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // Release the stall via Stall Service
        stallServiceClient.releaseStall(reservation.getStallId());
        
        logger.info("Reservation {} cancelled successfully", reservationId);
    }

    /**
     * Map reservation with provided user and stall data
     */
    private ReservationResponse mapToReservationResponse(Reservation reservation, UserDto user, StallDto stall) {
        return ReservationResponse.builder()
            .id(reservation.getId())
            .userId(user.getId())
            .userName(user.getName())
            .userEmail(user.getEmail())
            .businessName(user.getBusinessName())
            .stallId(stall.getId())
            .stallCode(stall.getStallCode())
            .stallSize(stall.getSize())
            .createdAt(reservation.getCreatedAt())
            .status(reservation.getStatus().name())
            .qrCodeBase64(reservation.getQrCodeBase64())
            .build();
    }

    /**
     * Map reservation with external service calls for user and stall data
     */
    private ReservationResponse mapToReservationResponseWithExternalData(Reservation reservation) {
        UserDto user = userServiceClient.getUserById(reservation.getUserId());
        StallDto stall = stallServiceClient.getStallById(reservation.getStallId());
        return mapToReservationResponse(reservation, user, stall);
    }
}
