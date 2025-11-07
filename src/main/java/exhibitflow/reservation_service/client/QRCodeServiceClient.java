package exhibitflow.reservation_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for communicating with QR Code Service
 */
@Component
public class QRCodeServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String qrCodeServiceUrl;

    public QRCodeServiceClient(
            RestTemplate restTemplate,
            @Value("${services.qrcode-service.url:http://localhost:8083}") String qrCodeServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.qrCodeServiceUrl = qrCodeServiceUrl;
    }

    /**
     * Generate QR code for reservation
     */
    public String generateQRCode(Long reservationId, String userName, String stallCode) {
        try {
            String url = qrCodeServiceUrl + "/api/qrcode/generate";
            logger.debug("Generating QR code: {}", url);
            
            Map<String, Object> request = new HashMap<>();
            request.put("reservationId", reservationId);
            request.put("userName", userName);
            request.put("stallCode", stallCode);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                (Class<Map<String, Object>>)(Class<?>)Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("qrCodeBase64");
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to generate QR code: {}", e.getMessage());
            return null;
        }
    }
}
