package exhibitflow.reservation_service.controller;

import exhibitflow.reservation_service.constants.HeaderConstants;
import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.service.QRCodeGeneratorService;
import exhibitflow.reservation_service.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for QR code operations
 */
@RestController
@RequestMapping("/api/v1/qrcode")
public class QRCodeController {
    
    @Autowired
    private QRCodeGeneratorService qrCodeGeneratorService;
    
    @Autowired
    private ReservationService reservationService;

    
    /**
     * Regenerate QR code for a reservation
     * GET /api/v1/qrcode/regenerate/{reservationId}
     */
    @GetMapping("/regenerate/{reservationId}")
    public ResponseEntity<Map<String, String>> regenerateQRCode(
            @PathVariable Long reservationId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId) {
        
        // Get reservation details
        ReservationResponse reservation = reservationService.getUserReservations(userId)
            .stream()
            .filter(r -> r.getId().equals(reservationId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
        "Reservation not found or unauthorized"));
        
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
