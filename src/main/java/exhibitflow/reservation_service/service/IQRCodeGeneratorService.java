package exhibitflow.reservation_service.service;

import exhibitflow.reservation_service.dto.QRCodeRequest;

/**
 * Service interface for QR code operations
 */
public interface IQRCodeGeneratorService {

    /**
     * Generates a QR code with default settings
     */
    String generateQRCode(Long reservationId, String userName, String stallCode);

    /**
     * Generates a QR code with custom settings
     */
    String generateQRCode(Long reservationId, String userName, String stallCode, QRCodeRequest settings);

    /**
     * Validates a QR code
     */
    boolean validateQRCode(String qrCodeContent, Long reservationId);
}
