package exhibitflow.reservation_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reservation Service API")
                        .version("1.0.0")
                        .description("""
                                REST API for managing exhibition stall reservations
                                
                                ## Authentication
                                This API uses JWT Bearer tokens issued by the Identity Service.
                                
                                ### Get Access Token
                                ```
                                POST http://localhost:8080/api/v1/auth/login
                                Content-Type: application/json
                                
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                ```
                                
                                ### Use Token
                                Add the token to the Authorization header:
                                ```
                                Authorization: Bearer {your-access-token}
                                ```
                                
                                ## Authorization Roles
                                - **USER/VIEWER**: Create and manage own reservations
                                - **MANAGER**: View all reservations, manage venue maps
                                - **ADMIN**: Full access including viewing all reservations with pagination
                                
                                ## Key Features
                                - Create reservations (up to 3 stalls per user)
                                - Complete payment for reservations
                                - View and cancel own reservations
                                - Generate and regenerate QR codes
                                - View venue maps with stall availability
                                - Admin: View all reservations with pagination and filtering
                                
                                ## Identity Service
                                Base URL: http://localhost:8080/api/v1
                                
                                The API Gateway extracts user information from JWT and passes it via headers:
                                - `X-User-Id`: User's unique identifier
                                - `Authorization`: JWT Bearer token for role-based authorization
                                """))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                JWT Bearer token from Identity Service
                                                
                                                To obtain a token:
                                                1. Login at: POST http://localhost:8080/api/v1/auth/login
                                                2. Copy the 'accessToken' from response
                                                3. Click 'Authorize' button above
                                                4. Enter token (without 'Bearer' prefix)
                                                5. Click 'Authorize' to save
                                                
                                                Token includes:
                                                - User identity (username, email, userId)
                                                - Roles (VIEWER, MANAGER, ADMIN)
                                                - Permissions (resource:action format)
                                                - Expiration (typically 24 hours)
                                                
                                                Required roles by endpoint:
                                                - Create/manage own reservations: USER/VIEWER role
                                                - View all reservations: MANAGER or ADMIN role
                                                - Paginated admin view: ADMIN role
                                                """)));
    }
}
