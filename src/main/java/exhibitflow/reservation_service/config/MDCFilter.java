package exhibitflow.reservation_service.config;

import exhibitflow.reservation_service.util.MDCUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to add MDC context to all requests
 * Ensures request tracking across the application
 */
@Component
public class MDCFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Generate and set request ID
            String requestId = MDCUtil.generateRequestId();
            
            // Extract user ID from header if present
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null) {
                try {
                    MDCUtil.setUserId(Long.parseLong(userIdHeader));
                } catch (NumberFormatException e) {
                    // Invalid user ID format, skip
                }
            }
            
            // Add request ID to response header
            response.setHeader("X-Request-Id", requestId);
            
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC after request
            MDCUtil.clear();
        }
    }
}
