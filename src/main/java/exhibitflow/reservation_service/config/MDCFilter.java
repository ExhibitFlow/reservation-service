package exhibitflow.reservation_service.config;

import exhibitflow.reservation_service.constants.HeaderConstants;
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
            // Use existing request ID from header if present, otherwise generate new one
            String requestId = request.getHeader(HeaderConstants.REQUEST_ID);
            if (requestId == null || requestId.trim().isEmpty()) {
                requestId = MDCUtil.generateRequestId();
            } else {
                // Propagate existing request ID to MDC
                MDCUtil.setRequestId(requestId);
            }
            
            // Extract user ID from header if present
            String userIdHeader = request.getHeader(HeaderConstants.USER_ID);
            if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
                MDCUtil.setUserId(userIdHeader);
            }
            
            // Add request ID to response header
            response.setHeader(HeaderConstants.REQUEST_ID, requestId);
            
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC after request
            MDCUtil.clear();
        }
    }
}
