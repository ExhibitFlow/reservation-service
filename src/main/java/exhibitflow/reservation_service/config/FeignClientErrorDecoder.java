package exhibitflow.reservation_service.config;

import exhibitflow.reservation_service.exception.ExternalServiceException;
import exhibitflow.reservation_service.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FeignClientErrorDecoder implements ErrorDecoder {

    private static final Logger logger = LoggerFactory.getLogger(FeignClientErrorDecoder.class);
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        logger.error("Feign client error: {} - Status: {}", methodKey, response.status());

        switch (response.status()) {
            case 404:
                return new ResourceNotFoundException(
                    String.format("Resource not found in external service: %s", methodKey)
                );
            case 503:
            case 504:
                return new ExternalServiceException(
                    String.format("External service unavailable: %s", methodKey)
                );
            case 500:
                return new ExternalServiceException(
                    String.format("Internal error in external service: %s", methodKey)
                );
            default:
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}
