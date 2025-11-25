package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.client.StallServiceClient;
import exhibitflow.reservation_service.client.UserServiceClient;
import exhibitflow.reservation_service.config.ApplicationProperties;
import exhibitflow.reservation_service.dto.CreateReservationRequest;
import exhibitflow.reservation_service.dto.PagedResponse;
import exhibitflow.reservation_service.dto.PaymentCompletedEvent;
import exhibitflow.reservation_service.dto.PaymentExpiredEvent;
import exhibitflow.reservation_service.dto.ReservationCancelledEvent;
import exhibitflow.reservation_service.dto.ReservationCreatedEvent;
import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.dto.ReservationSummary;
import exhibitflow.reservation_service.dto.StallDto;
import exhibitflow.reservation_service.dto.StallReservedEvent;
import exhibitflow.reservation_service.dto.UserDto;
import exhibitflow.reservation_service.entity.Reservation;
import exhibitflow.reservation_service.exception.*;
import exhibitflow.reservation_service.repository.ReservationRepository;
import exhibitflow.reservation_service.util.MDCUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reservation Service for microservices architecture
 * Communicates with User and Stall services via REST
 * Generates QR codes using ZXing library
 * Implements 5-minute payment lock mechanism
 */
@Service
public class ReservationService implements IReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReservationRepository reservationRepository;
    private final UserServiceClient userServiceClient;
    private final StallServiceClient stallServiceClient;
    private final QRCodeGeneratorService qrCodeGeneratorService;
    private final ApplicationProperties applicationProperties;
    private final IKafkaProducerService kafkaProducerService;

    @Autowired
    public ReservationService(
            ReservationRepository reservationRepository,
            UserServiceClient userServiceClient,
            StallServiceClient stallServiceClient,
            QRCodeGeneratorService qrCodeGeneratorService,
            ApplicationProperties applicationProperties,
            IKafkaProducerService kafkaProducerService) {
        this.reservationRepository = reservationRepository;
        this.userServiceClient = userServiceClient;
        this.stallServiceClient = stallServiceClient;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.applicationProperties = applicationProperties;
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * Creates a new reservation with payment lock (5 minutes)
     * Stall is temporarily locked until payment is completed
     * Communicates with User Service, Stall Service (QR code generated after payment)
     */
    @Override
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, String userId) {
        MDCUtil.setUserId(userId);
        logger.info("Creating reservation with payment lock for user: {} and stall: {}", userId, request.getStallId());

        // Validate user exists via User Service
        UserDto user = validateUser(userId);

        // Check if user has reached reservation limit (count CONFIRMED and PENDING_PAYMENT reservations)
        int maxReservations = applicationProperties.getReservation().getMaxReservationsPerUser();
        long userReservationCount = reservationRepository.countByUserIdAndStatusIn(
            userId, 
            List.of(Reservation.ReservationStatus.CONFIRMED, Reservation.ReservationStatus.PENDING_PAYMENT)
        );
        if (userReservationCount >= maxReservations) {
            logger.error("User {} has reached maximum reservation limit of {}", userId, maxReservations);
            throw new ReservationLimitExceededException(
                String.format("Maximum reservation limit of %d stalls per business has been reached", maxReservations)
            );
        }

        // Get stall details and check availability
        StallDto stall = validateStall(request.getStallId());
        checkStallAvailability(stall, request.getStallId());

        // Hold the stall temporarily via Stall Service (status: HELD)
        // This allows the user time to complete payment
        try {
            StallDto heldStall = stallServiceClient.holdStall(request.getStallId());
            if (heldStall == null) {
                throw new StallNotAvailableException("Failed to hold stall. Please try again.");
            }
            logger.info("Stall {} held successfully, waiting for payment completion", request.getStallId());
        } catch (Exception e) {
            logger.error("Failed to hold stall {}: {}", request.getStallId(), e.getMessage());
            throw new StallNotAvailableException("Failed to hold stall. Please try again.");
        }

        try {
            // Create reservation with PENDING_PAYMENT status
            int paymentLockMinutes = applicationProperties.getReservation().getPaymentLockMinutes();
            Reservation reservation = Reservation.builder()
                .userId(userId)
                .stallId(request.getStallId())
                .status(Reservation.ReservationStatus.PENDING_PAYMENT)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(paymentLockMinutes))
                .build();

            // Save reservation to get ID
            Reservation savedReservation = reservationRepository.save(reservation);

            logger.info("Reservation created with payment lock. ID: {}, Expires at: {}", 
                savedReservation.getId(), savedReservation.getPaymentExpiresAt());

            // Publish Kafka event
            publishEvent(() -> {
                ReservationCreatedEvent event = ReservationCreatedEvent.builder()
                    .reservationId(savedReservation.getId())
                    .userId(userId)
                    .stallId(request.getStallId())
                    .totalPrice(stall.getPrice() != null ? stall.getPrice().doubleValue() : 0.0)
                    .createdAt(savedReservation.getCreatedAt())
                    .paymentDeadline(savedReservation.getPaymentExpiresAt())
                    .build();
                kafkaProducerService.publishReservationCreated(event);
            }, "reservation created");

            // Return response WITHOUT QR code (generated after payment)
            return mapToReservationResponse(savedReservation, user, stall);
        } catch (Exception e) {
            // Rollback: Release the stall if reservation creation fails
            logger.error("Failed to create reservation, releasing stall: {}", e.getMessage());
            releaseStall(request.getStallId());
            throw e;
        }
    }

    /**
     * Complete payment for a pending reservation
     * Generates QR code and confirms the reservation
     */
    @Override
    @Transactional
    public ReservationResponse completePayment(Long reservationId, String userId) {
        MDCUtil.setUserId(userId);
        MDCUtil.setReservationId(reservationId);
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
            releaseStall(reservation.getStallId());
            throw new PaymentExpiredException("Payment window has expired. Please create a new reservation.");
        }
        
        // Reserve the stall (change status from HELD to RESERVED)
        try {
            StallDto reservedStall = stallServiceClient.reserveStall(reservation.getStallId());
            if (reservedStall == null) {
                throw new ExternalServiceException("Failed to confirm stall reservation. Please contact support.");
            }
            logger.info("Stall {} reserved successfully after payment", reservation.getStallId());
        } catch (Exception e) {
            logger.error("Failed to reserve stall {} after payment: {}", reservation.getStallId(), e.getMessage());
            throw new ExternalServiceException("Failed to confirm stall reservation. Please contact support.");
        }

        // Update to CONFIRMED
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        reservation.setPaymentCompletedAt(LocalDateTime.now());
        
        // Get user and stall data
        UserDto user = userServiceClient.getUserById(userId);
        StallDto stall = stallServiceClient.getStallById(reservation.getStallId());
        
        // Generate QR code using ZXing - REQUIRED
        String qrCodeBase64 = qrCodeGeneratorService.generateQRCode(
            reservation.getId(),
            user.getName(),
            stall.getCode()
        );
        reservation.setQrCodeBase64(qrCodeBase64);
        logger.info("QR code generated successfully for reservation: {}", reservationId);
        
        Reservation updated = reservationRepository.save(reservation);
        
        // Publish Kafka events
        publishEvent(() -> {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .reservationId(reservationId)
                .userId(userId)
                .stallId(reservation.getStallId())
                .amount(stall.getPrice() != null ? stall.getPrice().doubleValue() : 0.0)
                .paymentMethod("ONLINE")
                .paidAt(reservation.getPaymentCompletedAt())
                .build();
            kafkaProducerService.publishPaymentCompleted(event);
        }, "payment completed");
        
        // Publish StallReservedEvent with complete reservation details
        publishEvent(() -> {
            StallReservedEvent stallEvent = StallReservedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("STALL_RESERVED")
                .eventVersion("1.0")
                .occurredAt(java.time.LocalDateTime.now().toString())
                .payload(StallReservedEvent.Payload.builder()
                    .reservationId(String.valueOf(reservationId))
                    .stallId(String.valueOf(reservation.getStallId()))
                    .userId(String.valueOf(userId))
                    .size(mapToEventSize(stall.getSize()))
                    .reservationDetails(StallReservedEvent.ReservationDetails.builder()
                        .reservedAt(reservation.getPaymentCompletedAt().toString())
                        .expiresAt(reservation.getPaymentExpiresAt() != null ? reservation.getPaymentExpiresAt().toString() : null)
                        .status(reservation.getStatus().name())
                        .notes("Payment completed successfully")
                        .build())
                    .qrCode(qrCodeBase64)
                    .paidAmount(stall.getPrice() != null ? stall.getPrice().doubleValue() : 0.0)
                    .build())
                .build();
            kafkaProducerService.publishStallReserved(stallEvent);
        }, "stall reserved");
        
        logger.info("Payment completed successfully for reservation: {}", reservationId);
        
        return mapToReservationResponse(updated, user, stall);
    }

    /**
     * Get all reservations for a specific user
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getUserReservations(String userId) {
        MDCUtil.setUserId(userId);
        logger.debug("Fetching reservations for user: {}", userId);
        
        // Validate user exists
        validateUser(userId);

        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        return reservations.stream()
            .map(this::mapToReservationResponseWithExternalData)
            .collect(Collectors.toList());
    }

    /**
     * Get all reservations (admin function)
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        logger.debug("Fetching all reservations");
        List<Reservation> reservations = reservationRepository.findAll();
        return reservations.stream()
            .map(this::mapToReservationResponseWithExternalData)
            .collect(Collectors.toList());
    }

    /**
     * Get all reservations with pagination
     */
    @Override
    public PagedResponse<ReservationSummary> getAllReservations(Pageable pageable) {
        logger.debug("Fetching all reservations with pagination: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Reservation> page = reservationRepository.findAll(pageable);
        
        List<ReservationSummary> summaries = page.getContent().stream()
            .map(this::mapToReservationSummary)
            .collect(Collectors.toList());
        
        return PagedResponse.<ReservationSummary>builder()
            .content(summaries)
            .pageNumber(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .build();
    }

    /**
     * Get a specific reservation by ID
     */
    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long reservationId, String userId) {
        MDCUtil.setUserId(userId);
        MDCUtil.setReservationId(reservationId);
        logger.debug("Fetching reservation: {} for user: {}", reservationId, userId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));
        
        // Verify ownership
        if (!reservation.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to view this reservation");
        }
        
        return mapToReservationResponseWithExternalData(reservation);
    }

    /**
     * Cancel a reservation
     */
    @Override
    @Transactional
    public void cancelReservation(Long reservationId, String userId) {
        MDCUtil.setUserId(userId);
        MDCUtil.setReservationId(reservationId);
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
        releaseStall(reservation.getStallId());

        // Publish Kafka event
        publishEvent(() -> {
            ReservationCancelledEvent event = ReservationCancelledEvent.builder()
                .reservationId(reservationId)
                .userId(userId)
                .stallId(reservation.getStallId())
                .cancellationReason("User requested cancellation")
                .cancelledAt(LocalDateTime.now())
                .build();
            kafkaProducerService.publishReservationCancelled(event);
        }, "reservation cancelled");

        logger.info("Reservation {} cancelled successfully", reservationId);
    }

    /**
     * Check if a stall has active reservations
     */
    @Override
    public boolean hasActiveReservation(Long stallId) {
        logger.debug("Checking active reservations for stall: {}", stallId);
        return reservationRepository.findByStallIdAndStatus(
            stallId, 
            Reservation.ReservationStatus.CONFIRMED
        ).isPresent();
    }

    /**
     * Count active reservations for a user
     */
    @Override
    public long countActiveReservationsForUser(String userId) {
        MDCUtil.setUserId(userId);
        logger.debug("Counting active reservations for user: {}", userId);
        return reservationRepository.countByUserIdAndStatusIn(
            userId,
            List.of(Reservation.ReservationStatus.CONFIRMED, Reservation.ReservationStatus.PENDING_PAYMENT)
        );
    }

    /**
     * Temporarily holds a stall for a user
     * This is a lightweight operation that marks the stall as held in the Stall Service
     */
    @Override
    @Transactional
    public void holdStall(Long stallId, String userId) {
        MDCUtil.setUserId(userId);
        logger.info("Holding stall {} for user: {}", stallId, userId);

        // Validate user exists and stall is available
        validateUser(userId);
        StallDto stall = validateStall(stallId);
        checkStallAvailability(stall, stallId);

        // Hold the stall via Stall Service
        try {
            StallDto heldStall = stallServiceClient.holdStall(stallId);
            if (heldStall == null) {
                throw new StallNotAvailableException("Failed to hold stall. Please try again.");
            }
            logger.info("Stall {} held successfully for user: {}", stallId, userId);
        } catch (Exception e) {
            logger.error("Failed to hold stall {}: {}", stallId, e.getMessage());
            throw new StallNotAvailableException("Failed to hold stall. Please try again.");
        }
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
            .stallCode(stall.getCode())
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
        try {
            UserDto user = userServiceClient.getUserById(reservation.getUserId());
            StallDto stall = stallServiceClient.getStallById(reservation.getStallId());
            return mapToReservationResponse(reservation, user, stall);
        } catch (Exception e) {
            logger.error("Error fetching external data for reservation {}: {}", 
                reservation.getId(), e.getMessage());
            throw new ExternalServiceException(
                "Failed to fetch complete reservation details", e);
        }
    }

    /**
     * Map reservation to summary
     */
    private ReservationSummary mapToReservationSummary(Reservation reservation) {
        try {
            UserDto user = userServiceClient.getUserById(reservation.getUserId());
            StallDto stall = stallServiceClient.getStallById(reservation.getStallId());
            
            return ReservationSummary.builder()
                .id(reservation.getId())
                .userName(user.getName())
                .businessName(user.getBusinessName())
                .stallCode(stall.getCode())
                .status(reservation.getStatus().name())
                .createdAt(reservation.getCreatedAt().format(DATE_TIME_FORMATTER))
                .build();
        } catch (Exception e) {
            logger.warn("Error creating summary for reservation {}: {}", 
                reservation.getId(), e.getMessage());
            // Return partial data
            return ReservationSummary.builder()
                .id(reservation.getId())
                .userName("N/A")
                .businessName("N/A")
                .stallCode("N/A")
                .status(reservation.getStatus().name())
                .createdAt(reservation.getCreatedAt().format(DATE_TIME_FORMATTER))
                .build();
        }
    }

    /**
     * Helper method to publish Kafka events with error handling
     */
    private void publishEvent(Runnable eventPublisher, String eventType) {
        try {
            eventPublisher.run();
        } catch (Exception e) {
            logger.warn("Failed to publish {} event: {}", eventType, e.getMessage());
        }
    }

    /**
     * Helper method to release a stall with error handling
     */
    private void releaseStall(Long stallId) {
        try {
            stallServiceClient.releaseStall(stallId);
        } catch (Exception e) {
            logger.error("Failed to release stall {}: {}", stallId, e.getMessage());
        }
    }

    /**
     * Helper method to validate user exists
     */
    private UserDto validateUser(String userId) {
        UserDto user = userServiceClient.getUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        return user;
    }

    /**
     * Helper method to validate and get stall details
     */
    private StallDto validateStall(Long stallId) {
        StallDto stall = stallServiceClient.getStallById(stallId);
        if (stall == null) {
            throw new ResourceNotFoundException("Stall not found with ID: " + stallId);
        }
        return stall;
    }

    /**
     * Helper method to check stall availability
     */
    private void checkStallAvailability(StallDto stall, Long stallId) {
        // Check if stall is already reserved or held
        if ("RESERVED".equals(stall.getStatus()) || "HELD".equals(stall.getStatus())) {
            logger.error("Stall {} is not available (status: {})", stall.getCode(), stall.getStatus());
            throw new StallNotAvailableException(
                String.format("Stall %s is not available (status: %s)", stall.getCode(), stall.getStatus())
            );
        }

        // Check if there's an existing PENDING_PAYMENT reservation for this stall
        Optional<Reservation> existingPending = reservationRepository
            .findByStallIdAndStatus(stallId, Reservation.ReservationStatus.PENDING_PAYMENT);
        
        if (existingPending.isPresent()) {
            Reservation pending = existingPending.get();
            if (pending.getPaymentExpiresAt().isAfter(LocalDateTime.now())) {
                logger.error("Stall {} is temporarily locked for payment until {}", 
                    stall.getCode(), pending.getPaymentExpiresAt());
                throw new StallNotAvailableException(
                    String.format("Stall %s is temporarily locked for payment. Please try again later.", stall.getCode())
                );
            } else {
                // Expired - auto-cancel it
                logger.info("Auto-expiring reservation {} for stall {}", pending.getId(), stallId);
                pending.setStatus(Reservation.ReservationStatus.EXPIRED);
                reservationRepository.save(pending);
                releaseStall(stallId);
            }
        }
    }

    /**
     * Helper method to map stall size to event size enum
     */
    private StallReservedEvent.Size mapToEventSize(String stallSize) {
        if (stallSize == null) {
            return StallReservedEvent.Size.MEDIUM; // default
        }
        try {
            return StallReservedEvent.Size.valueOf(stallSize.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown stall size: {}, defaulting to MEDIUM", stallSize);
            return StallReservedEvent.Size.MEDIUM;
        }
    }
}
