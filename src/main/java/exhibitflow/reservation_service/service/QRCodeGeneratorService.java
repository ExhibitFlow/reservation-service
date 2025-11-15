package exhibitflow.reservation_service.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class QRCodeGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeGeneratorService.class);
    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;

    /**
     * Generate QR code as Base64 encoded string
     * 
     * @param reservationId The reservation ID
     * @param userName The user's name
     * @param stallCode The stall code
     * @return Base64 encoded QR code image
     */
    public String generateQRCode(Long reservationId, String userName, String stallCode) {
        try {
            // Create QR code content with reservation details
            String qrContent = String.format(
                "Reservation ID: %d\nUser: %s\nStall: %s",
                reservationId,
                userName,
                stallCode
            );

            logger.info("Generating QR code for reservation: {}", reservationId);

            // Configure QR code settings
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                qrContent,
                BarcodeFormat.QR_CODE,
                QR_CODE_WIDTH,
                QR_CODE_HEIGHT,
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
}
