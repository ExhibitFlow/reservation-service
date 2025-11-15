package exhibitflow.reservation_service.controller;

import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.service.QRCodeGeneratorService;
import exhibitflow.reservation_service.service.ReservationService;
import jakarta.persistence.PostRemove;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for QR code operations
 */
@RestController
@RequestMapping("/api/qrcode")
public class QRCodeController {
    
    @Autowired
    private QRCodeGeneratorService qrCodeGeneratorService;
    
    @Autowired
    private ReservationService reservationService;

    
    /**
     * Regenerate QR code for a reservation
     * GET /api/qrcode/regenerate/{reservationId}
     */
    @GetMapping("/regenerate/{reservationId}")
    public ResponseEntity<Map<String, String>> regenerateQRCode(
            @PathVariable Long reservationId,
            @RequestHeader("X-User-Id") Long userId) {
        
        // Get reservation details
        ReservationResponse reservation = reservationService.getUserReservations(userId)
            .stream()
            .filter(r -> r.getId().equals(reservationId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Reservation not found or unauthorized"));
        
        // Generate new QR code
        String qrCodeBase64 = qrCodeGeneratorService.generateQRCode(
            reservation.getId(),
            reservation.getUserName(),
            reservation.getStallCode()
        );
        
        Map<String, String> response = new HashMap<>();
        response.put("qrCodeBase64", qrCodeBase64);
        response.put("reservationId", reservationId.toString());
        
        return ResponseEntity.ok(response);
    }
}
