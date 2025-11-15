package exhibitflow.reservation_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for communicating with QR Code Service
 */
@FeignClient(
    name = "qrcode-service",
    url = "${services.qrcode-service.url:http://localhost:8083}"
)
public interface QRCodeServiceClient {

    /**
     * Generate QR code for reservation
     */
    @PostMapping("/api/qrcode/generate")
    Map<String, Object> generateQRCode(@RequestBody Map<String, Object> request);
}
