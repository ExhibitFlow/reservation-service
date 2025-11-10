package exhibitflow.reservation_service.repository;

import exhibitflow.reservation_service.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Reservation entity in microservices architecture
 * Uses userId instead of User entity references
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    List<Reservation> findByUserId(Long userId);
    
    List<Reservation> findByUserIdAndStatus(Long userId, Reservation.ReservationStatus status);
    
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.userId = :userId AND r.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Reservation.ReservationStatus status);
    
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.userId = :userId AND r.status IN :statuses")
    long countByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<Reservation.ReservationStatus> statuses);
    
    List<Reservation> findByStallId(Long stallId);
    
    Optional<Reservation> findByStallIdAndStatus(Long stallId, Reservation.ReservationStatus status);
    
    List<Reservation> findByStatusAndPaymentExpiresAtBefore(Reservation.ReservationStatus status, LocalDateTime expiryTime);
}
