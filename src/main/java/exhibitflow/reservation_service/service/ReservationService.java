package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.client.QRCodeServiceClient;
import exhibitflow.reservation_service.client.StallServiceClient;
import exhibitflow.reservation_service.client.UserServiceClient;
import exhibitflow.reservation_service.dto.CreateReservationRequest;
import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.dto.StallDto;
import exhibitflow.reservation_service.dto.UserDto;
import exhibitflow.reservation_service.entity.Reservation;
import exhibitflow.reservation_service.exception.*;
import exhibitflow.reservation_service.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reservation Service for microservices architecture
 * Communicates with User, Stall, and QRCode services via REST
 * Implements 5-minute payment lock mechanism
 */
@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final int MAX_RESERVATIONS_PER_USER = 3;
    private static final int PAYMENT_LOCK_MINUTES = 5;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private StallServiceClient stallServiceClient;

    @Autowired
    private QRCodeServiceClient qrCodeServiceClient;

    /**
     * Creates a new reservation with payment lock (5 minutes)
     * Stall is temporarily locked until payment is completed
     * Communicates with User Service, Stall Service (QR code generated after payment)
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, Long userId) {
        logger.info("Creating reservation with payment lock for user: {} and stall: {}", userId, request.getStallId());

        // Validate user exists via User Service
        UserDto user = userServiceClient.getUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        // Check if user has reached reservation limit (only count CONFIRMED reservations)
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

        // Check if there's an existing PENDING_PAYMENT reservation for this stall
        Optional<Reservation> existingPending = reservationRepository
            .findByStallIdAndStatus(request.getStallId(), Reservation.ReservationStatus.PENDING_PAYMENT);
        
        if (existingPending.isPresent()) {
            Reservation pending = existingPending.get();
            if (pending.getPaymentExpiresAt().isAfter(LocalDateTime.now())) {
                logger.error("Stall {} is temporarily locked for payment until {}", 
                    stall.getStallCode(), pending.getPaymentExpiresAt());
                throw new StallNotAvailableException(
                    String.format("Stall %s is temporarily locked for payment. Please try again later.", stall.getStallCode())
                );
            } else {
                // Expired - auto-cancel it
                logger.info("Auto-expiring reservation {} for stall {}", pending.getId(), request.getStallId());
                pending.setStatus(Reservation.ReservationStatus.EXPIRED);
                reservationRepository.save(pending);
                stallServiceClient.releaseStall(request.getStallId());
            }
        }

        // Temporarily reserve the stall via Stall Service
        boolean reserved = stallServiceClient.reserveStall(request.getStallId());
        if (!reserved) {
            throw new StallNotAvailableException("Failed to reserve stall. Please try again.");
        }

        try {
            // Create reservation with PENDING_PAYMENT status
            Reservation reservation = Reservation.builder()
                .userId(userId)
                .stallId(request.getStallId())
                .status(Reservation.ReservationStatus.PENDING_PAYMENT)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(PAYMENT_LOCK_MINUTES))
                .build();

            // Save reservation to get ID
            reservation = reservationRepository.save(reservation);

            logger.info("Reservation created with payment lock. ID: {}, Expires at: {}", 
                reservation.getId(), reservation.getPaymentExpiresAt());

            // Return response WITHOUT QR code (generated after payment)
            return mapToReservationResponse(reservation, user, stall);
        } catch (Exception e) {
            // Rollback: Release the stall if reservation creation fails
            logger.error("Failed to create reservation, releasing stall: {}", e.getMessage());
            stallServiceClient.releaseStall(request.getStallId());
            throw e;
        }
    }

    /**
     * Complete payment for a pending reservation
     * Generates QR code and confirms the reservation
     */
    @Transactional
    public ReservationResponse completePayment(Long reservationId, Long userId) {
        logger.info("Completing payment for reservation: {} by user: {}", reservationId, userId);
        
        // Find reservation
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));
        
        // Verify ownership
        if (!reservation.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to complete payment for this reservation");
        }
        
        // Check status
        if (reservation.getStatus() != Reservation.ReservationStatus.PENDING_PAYMENT) {
            throw new InvalidOperationException("Reservation is not pending payment");
        }
        
        // Check if payment window expired
        if (reservation.getPaymentExpiresAt().isBefore(LocalDateTime.now())) {
            // Auto-cancel and release stall
            logger.error("Payment window expired for reservation {}", reservationId);
            reservation.setStatus(Reservation.ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            stallServiceClient.releaseStall(reservation.getStallId());
            throw new PaymentExpiredException("Payment window has expired. Please create a new reservation.");
        }
        
        // Update to CONFIRMED
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        reservation.setPaymentCompletedAt(LocalDateTime.now());
        
        // Get user and stall data
        UserDto user = userServiceClient.getUserById(userId);
        StallDto stall = stallServiceClient.getStallById(reservation.getStallId());
        
        // Generate QR code NOW
        String qrCodeBase64 = qrCodeServiceClient.generateQRCode(
            reservation.getId(),
            user.getName(),
            stall.getStallCode()
        );
        
        if (qrCodeBase64 != null) {
            reservation.setQrCodeBase64(qrCodeBase64);
        }
        
        Reservation updated = reservationRepository.save(reservation);
        
        logger.info("Payment completed successfully for reservation: {}", reservationId);
        
        return mapToReservationResponse(updated, user, stall);
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
            .paymentExpiresAt(reservation.getPaymentExpiresAt())
            .paymentCompletedAt(reservation.getPaymentCompletedAt())
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
