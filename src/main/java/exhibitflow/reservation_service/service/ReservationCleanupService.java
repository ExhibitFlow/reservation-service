package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.client.StallServiceClient;
import exhibitflow.reservation_service.entity.Reservation;
import exhibitflow.reservation_service.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cleanup service for expired payment locks
 * Runs every minute to release stalls from expired pending payments
 */
@Service
public class ReservationCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationCleanupService.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private StallServiceClient stallServiceClient;

    /**
     * Runs every minute to clean up expired pending payments
     * Releases stalls and marks reservations as EXPIRED
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Reservation> expiredReservations = reservationRepository
            .findByStatusAndPaymentExpiresAtBefore(
                Reservation.ReservationStatus.PENDING_PAYMENT, 
                now
            );
        
        if (expiredReservations.isEmpty()) {
            return;
        }
        
        logger.info("Found {} expired pending reservations to clean up", expiredReservations.size());
        
        for (Reservation reservation : expiredReservations) {
            try {
                logger.info("Expiring reservation {} for stall {} (expired at {})", 
                    reservation.getId(), 
                    reservation.getStallId(),
                    reservation.getPaymentExpiresAt()
                );
                
                // Update status to EXPIRED
                reservation.setStatus(Reservation.ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);
                
                // Release the stall
                stallServiceClient.releaseStall(reservation.getStallId());
                
                logger.info("Successfully expired reservation {} and released stall {}", 
                    reservation.getId(), 
                    reservation.getStallId()
                );
                
            } catch (Exception e) {
                logger.error("Failed to expire reservation {} for stall {}: {}", 
                    reservation.getId(), 
                    reservation.getStallId(), 
                    e.getMessage(),
                    e
                );
            }
        }
        
        logger.info("Cleanup completed: {} reservations expired", expiredReservations.size());
    }
}
