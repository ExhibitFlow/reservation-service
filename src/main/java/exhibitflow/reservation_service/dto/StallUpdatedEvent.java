package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StallUpdatedEvent {
    private Long id;
    private String code;
    private String size;
    private String location;
    private Double price;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
