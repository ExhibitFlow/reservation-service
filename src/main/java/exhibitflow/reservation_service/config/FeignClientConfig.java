package exhibitflow.reservation_service.config;

import exhibitflow.reservation_service.constants.HeaderConstants;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Feign Clients
 * Configures connection timeout, read timeout, retry mechanism, and logging
 */
@Configuration
public class FeignClientConfig {

    private static final int CONNECT_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds

    /**
     * Configure Feign request options with timeouts
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            CONNECT_TIMEOUT, TimeUnit.MILLISECONDS,
            READ_TIMEOUT, TimeUnit.MILLISECONDS,
            true
        );
    }

    /**
     * Configure retry mechanism for failed requests
     * Retries up to 3 times with exponential backoff
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            100L,  // Initial interval (ms)
            1000L, // Max interval (ms)
            3      // Max attempts
        );
    }

    /**
     * Configure Feign logging level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Custom error decoder for Feign clients
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignClientErrorDecoder();
    }

    /**
     * Request interceptor to forward Authorization header to external services
     * This allows the Stall Service and User Service to perform role-based authorization
     */
    @Bean
    public RequestInterceptor authorizationHeaderInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader(HeaderConstants.AUTHORIZATION);

                if (authHeader != null && !authHeader.isEmpty()) {
                    requestTemplate.header(HeaderConstants.AUTHORIZATION, authHeader);
                }
            }
        };
    }
}
