package exhibitflow.reservation_service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;


@Configuration
@ConfigurationProperties(prefix = "reservation")
@Data
@Validated
public class ApplicationProperties {

    /**
     * Reservation-related settings
     */
    @NotNull
    private ReservationSettings reservation = new ReservationSettings();

    /**
     * QR code generation settings
     */
    @NotNull
    private QRCodeSettings qrCode = new QRCodeSettings();

    /**
     * Cleanup scheduler settings
     */
    @NotNull
    private CleanupSettings cleanup = new CleanupSettings();

    @Data
    public static class ReservationSettings {
        @Min(1)
        private int maxReservationsPerUser;

        @Min(1)
        private int paymentLockMinutes;
    }

    @Data
    public static class QRCodeSettings {
        @Min(100)
        private int width;

        @Min(100)
        private int height;

        private String errorCorrectionLevel;
    }

    @Data
    public static class CleanupSettings {
        @Min(1000)
        private long fixedRateMs;
    }
}
