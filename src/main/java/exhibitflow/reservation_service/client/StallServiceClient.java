package exhibitflow.reservation_service.client;

import exhibitflow.reservation_service.dto.StallDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for communicating with Stall Service
 */
@Component
public class StallServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(StallServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String stallServiceUrl;

    public StallServiceClient(
            RestTemplate restTemplate,
            @Value("${services.stall-service.url:http://localhost:8082}") String stallServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.stallServiceUrl = stallServiceUrl;
    }

    /**
     * Get stall by ID from Stall Service
     */
    public StallDto getStallById(Long stallId) {
        try {
            String url = stallServiceUrl + "/api/stalls/" + stallId;
            logger.info("Fetching stall from Stall Service: {}", url);
            StallDto stall = restTemplate.getForObject(url, StallDto.class);
            logger.info("Successfully fetched stall: {}", stall != null ? stall.getStallCode() : "null");
            return stall;
        } catch (Exception e) {
            logger.error("Failed to fetch stall with ID {} from URL {}: {} - {}", 
                stallId, stallServiceUrl, e.getClass().getSimpleName(), e.getMessage());
            logger.error("Full exception: ", e);
            throw new RuntimeException("Failed to fetch stall from Stall Service: " + e.getMessage(), e);
        }
    }

    /**
     * Reserve a stall (mark as reserved)
     */
    public boolean reserveStall(Long stallId) {
        try {
            String url = stallServiceUrl + "/api/stalls/" + stallId + "/reserve";
            logger.debug("Reserving stall in Stall Service: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<StallDto> response = restTemplate.exchange(
                url, 
                HttpMethod.PUT, 
                request, 
                StallDto.class
            );
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.error("Failed to reserve stall with ID {}: {}", stallId, e.getMessage());
            return false;
        }
    }

    /**
     * Release a stall reservation (mark as available)
     */
    public boolean releaseStall(Long stallId) {
        try {
            String url = stallServiceUrl + "/api/stalls/" + stallId + "/release";
            logger.debug("Releasing stall in Stall Service: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<StallDto> response = restTemplate.exchange(
                url, 
                HttpMethod.PUT, 
                request, 
                StallDto.class
            );
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.error("Failed to release stall with ID {}: {}", stallId, e.getMessage());
            return false;
        }
    }
}
