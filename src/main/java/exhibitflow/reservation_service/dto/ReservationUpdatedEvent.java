package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdatedEvent {
    private Long reservationId;
    private String userId;
    private Long stallId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime updatedAt;
}
