package exhibitflow.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationSummary {
    private Long id;
    private String userName;
    private String businessName;
    private String stallCode;
    private String status;
    private String createdAt;
}
