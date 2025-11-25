package exhibitflow.reservation_service.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import exhibitflow.reservation_service.config.ApplicationProperties;
import exhibitflow.reservation_service.dto.QRCodeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating QR codes using ZXing library
 */
@Service
public class QRCodeGeneratorService implements IQRCodeGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeGeneratorService.class);
    
    private final ApplicationProperties applicationProperties;

    @Autowired
    public QRCodeGeneratorService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Generate QR code as Base64 encoded string with default settings
     * 
     * @param reservationId The reservation ID
     * @param userName The user's name
     * @param stallCode The stall code
     * @return Base64 encoded QR code image
     */
    @Override
    public String generateQRCode(Long reservationId, String userName, String stallCode) {
        int width = applicationProperties.getQrCode().getWidth();
        int height = applicationProperties.getQrCode().getHeight();
        return generateQRCodeInternal(reservationId, userName, stallCode, width, height, 
            applicationProperties.getQrCode().getErrorCorrectionLevel());
    }

    /**
     * Generate QR code with custom settings
     */
    @Override
    public String generateQRCode(Long reservationId, String userName, String stallCode, QRCodeRequest settings) {
        return generateQRCodeInternal(reservationId, userName, stallCode, 
            settings.getWidth(), settings.getHeight(), settings.getErrorCorrectionLevel());
    }

    /**
     * Validates a QR code content against a reservation ID
     */
    @Override
    public boolean validateQRCode(String qrCodeContent, Long reservationId) {
        if (qrCodeContent == null || reservationId == null) {
            return false;
        }
        return qrCodeContent.contains("Reservation ID: " + reservationId);
    }

    /**
     * Internal method to generate QR code
     */
    private String generateQRCodeInternal(Long reservationId, String userName, String stallCode, 
                                          int width, int height, String errorCorrectionLevelStr) {
        try {
            // Create QR code content with reservation details
            String qrContent = String.format(
                "Reservation ID: %d\nUser: %s\nStall: %s",
                reservationId,
                userName,
                stallCode
            );

            logger.info("Generating QR code for reservation: {}", reservationId);

            // Parse error correction level
            ErrorCorrectionLevel errorCorrectionLevel = parseErrorCorrectionLevel(errorCorrectionLevelStr);

            // Configure QR code settings
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                qrContent,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            );

            // Convert to PNG image
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();

            // Encode to Base64
            String base64QRCode = Base64.getEncoder().encodeToString(qrCodeBytes);
            
            logger.info("QR code generated successfully for reservation: {}", reservationId);
            
            return base64QRCode;

        } catch (WriterException | IOException e) {
            logger.error("Failed to generate QR code for reservation {}: {}", reservationId, e.getMessage());
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }


    private ErrorCorrectionLevel parseErrorCorrectionLevel(String level) {
        return switch (level.toUpperCase()) {
            case "L" -> ErrorCorrectionLevel.L;
            case "M" -> ErrorCorrectionLevel.M;
            case "Q" -> ErrorCorrectionLevel.Q;
            case "H" -> ErrorCorrectionLevel.H;
            default -> ErrorCorrectionLevel.H;
        };
    }
}
