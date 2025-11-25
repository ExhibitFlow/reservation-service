# Colombo International Bookfair - Reservation Service

A **microservices-based** backend service for managing stall reservations at the Colombo International Bookfair. This Spring Boot application is part of a distributed system architecture that orchestrates reservations by coordinating with User Service, Stall Service, and QRCode Service.

> **Note:** This is a microservice, not a standalone application. It requires User Service, Stall Service, and QRCode Service to be running for full functionality.

## 🎯 Features

- **Reservation Management**
  - Create and track reservations with payment workflow
  - Maximum 3 stalls per business
  - Payment lock system (5-minute payment window)
  - Automatic payment expiration handling
  - Reservation status tracking (PENDING_PAYMENT, CONFIRMED, CANCELLED, EXPIRED)
  - Reservation cancellation with automatic stall release
  - Pagination support for admin reservation listing
  
- **QR Code System**
  - Built-in QR code generation (Google ZXing)
  - Automatic QR code creation upon payment completion
  - QR code regeneration capability
  - Base64-encoded QR codes stored with reservations
  
- **Venue Map Integration**
  - Real-time venue map with stall availability
  - Filter available stalls for reservation
  - View map by venue code
  - PostGIS spatial queries for venue visualization
  
- **Microservices Integration**
  - OpenFeign clients for User Service and Stall Service
  - User validation and authentication via User Service
  - Stall availability checks and reservation coordination
  - Custom error handling and retry logic
  
- **Event-Driven Architecture**
  - **Kafka message broker** for asynchronous event publishing
  - Publishes: reservation.created, payment.completed, reservation.cancelled, payment.expired
  - Consumes: stall.created, stall.updated (for cache invalidation)
  - Real-time event streaming for reservation lifecycle
  - Decoupled microservices communication
  
- **Performance & Reliability**
  - Redis-based caching for stall data
  - Automatic cache invalidation via Kafka events
  - Scheduled cleanup of expired reservations
  - Database indexing for optimized queries
  - Health checks and monitoring via Spring Actuator

## 🏗️ Architecture

### Microservices Architecture

This service is part of a distributed microservices system:

```
┌─────────────────────┐
│   Identity Service  │ (JWT Authentication)
│  (Port: 8080)       │
└──────────┬──────────┘
           │
           ├─────────────────────────────────────────┐
           │                                         │
┌──────────▼──────────┐                   ┌─────────▼────────┐
│   User Service      │                   │  Stall Service   │
│  (Port: 8080)       │                   │  (Port: 8081)    │
│  - User management  │                   │  - Stall data    │
│  - User validation  │                   │  - Availability  │
└──────────┬──────────┘                   └─────────┬────────┘
           │                                         │
           │        ┌──────────────────────┐         │
           └────────►  Reservation Service ◄─────────┘
                    │    (Port: 8084)      │
                    │  - Orchestration     │
                    │  - Business logic    │
                    │  - QR generation     │
                    │  - Venue mapping     │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │   Apache Kafka       │
                    │  (Port: 9092)        │
                    │  - Event streaming   │
                    └──────────────────────┘
```

### Technology Stack

- **Backend Framework:** Spring Boot 3.4.0
- **Java Version:** 17
- **Database:** PostgreSQL 16 with PostGIS spatial extension
- **Database Migration:** Flyway
- **Message Broker:** Apache Kafka (via Docker)
- **Inter-service Communication:** Spring Cloud OpenFeign
- **Build Tool:** Maven
- **API Documentation:** SpringDoc OpenAPI (Swagger)
- **Security:** JWT-based authentication
- **Additional Libraries:** Lombok, Spring Validation, Spring Kafka, Hibernate Spatial, Google ZXing (QR codes), Spring Boot Actuator
- **Code Quality Tools:** Checkstyle, SpotBugs with FindSecBugs, PMD

### Database Schema

The Reservation Service uses PostgreSQL with the following schema:

```
reservations
├── id (PK, BIGSERIAL)
├── user_id (VARCHAR, Reference to User Service)
├── stall_id (BIGINT, Reference to Stall Service)
├── created_at (TIMESTAMP, auto-generated)
├── qr_code_base64 (TEXT, generated internally)
├── payment_expires_at (TIMESTAMP, 5-minute lock)
├── payment_completed_at (TIMESTAMP)
└── status (ENUM: PENDING_PAYMENT, CONFIRMED, CANCELLED, EXPIRED)

Indexes:
├── idx_user_id
├── idx_stall_id
├── idx_status
├── idx_created_at
├── idx_payment_expires
├── idx_user_stall (composite)
└── idx_status_stall (composite)
```

**Note:** User and Stall data are NOT stored in this service. They are fetched from their respective services via OpenFeign clients.

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL 16 with PostGIS extension (or use Docker)
- Maven 3.6+
- **Docker & Docker Compose** (for Kafka and PostgreSQL)
- **Running instances of dependent services:**
  - User Service (Port 8080)
  - Stall Service (Port 8081)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/ExhibitFlow/Reservation-service.git
cd Reservation-service
```

2. **Start Infrastructure with Docker Compose**

The `docker-compose.yml` includes PostgreSQL, Kafka, Zookeeper, and Kafka UI:

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (Port 5532) 
- Kafka (Port 9092)
- Zookeeper (Port 2181)
- Kafka UI (Port 8090)

Verify services:
- Kafka UI: `http://localhost:8090`
- PostgreSQL: `localhost:5532`

3. **Configure application properties**

Edit `src/main/resources/application.properties`:

```properties
# Database Configuration (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5532/reservation_db
spring.datasource.username=postgres
spring.datasource.password=asd

# Microservices Configuration
services.user-service.url=http://{server-url}:8080
services.stall-service.url=http://{server-url}:8081

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092

# JWT Configuration (must match identity-service secret)
jwt.secret=your-secret-key
jwt.expiration=86400000

# Reservation Configuration
reservation.reservation.max-reservations-per-user=3
reservation.reservation.payment-lock-minutes=5
```

4. **Database Migration**

Flyway will automatically run migrations on startup from `src/main/resources/db/migration/`

5. **Build the project**
```bash
./mvnw clean install
```

6. **Run the application**
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8084`

**API Documentation:** `http://localhost:8084/swagger-ui.html`

### Service Dependencies

⚠️ **Important:** This service requires the following services to be running:

1. **User Service** - Manages user data and provides user validation
2. **Stall Service** - Manages stall information and availability
3. **PostgreSQL** - Database with PostGIS extension (Docker)
4. **Kafka** - Message broker for event-driven architecture (Docker)
5. **Identity Service** - JWT authentication (not directly called, but provides auth tokens)

Ensure all dependent services are up and running before starting the Reservation Service.

### Kafka Event Topics

**Published Events:**
- `reservation.created` - When a new reservation is created (PENDING_PAYMENT)
- `reservation.updated` - When a reservation status changes
- `reservation.payment.completed` - When payment is completed (→ CONFIRMED)
- `reservation.cancelled` - When a reservation is cancelled
- `reservation.payment.expired` - When payment deadline expires (→ EXPIRED)

**Consumed Events:**
- `stall.created` - Clears stalls cache when new stall is created
- `stall.updated` - Updates cached stall data when stall information changes
- `stall.reserved` - Tracks stall reservation events

## 📚 API Documentation

### Authentication

This service expects the `X-User-Id` header in requests, which should be set by the API Gateway after JWT authentication. The Reservation Service itself does not handle authentication - it delegates that responsibility to the User Service via the API Gateway.

### Quick Start Examples

**Note:** All examples assume the user has been authenticated via the API Gateway, which sets the `X-User-Id` header.

#### 1. Create a Reservation (with 5-minute payment lock)
```bash
curl -X POST http://localhost:8084/api/v1/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-User-Id: 60ab5b68-6f41-49b7-a461-f2cf89e6c099" \
  -d '{
    "stallId": 5
  }'
```

**Response:**
```json
{
  "id": 1,
  "userId": "60ab5b68-6f41-49b7-a461-f2cf89e6c099",
  "userName": "John Doe",
  "userEmail": "john@example.com",
  "businessName": "Johns Books Store",
  "stallId": 5,
  "stallCode": "E",
  "stallSize": "MEDIUM",
  "status": "PENDING_PAYMENT",
  "createdAt": "2025-11-24T10:30:00",
  "paymentExpiresAt": "2025-11-24T10:35:00",
  "qrCodeBase64": null
}
```

#### 2. Complete Payment (generates QR code)
```bash
curl -X POST http://localhost:8084/api/v1/reservations/1/complete-payment \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-User-Id: 60ab5b68-6f41-49b7-a461-f2cf89e6c099"
```

#### 3. View My Reservations
```bash
curl http://localhost:8084/api/v1/reservations/my \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-User-Id: 60ab5b68-6f41-49b7-a461-f2cf89e6c099"
```

#### 4. View All Reservations (Admin) with Pagination
```bash
curl "http://localhost:8084/api/v1/reservations?page=0&size=10&sortBy=createdAt&direction=DESC" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 5. Get Venue Map
```bash
curl http://localhost:8084/api/v1/venue/map \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 6. Cancel a Reservation
```bash
curl -X DELETE http://localhost:8084/api/v1/reservations/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-User-Id: 60ab5b68-6f41-49b7-a461-f2cf89e6c099"
```

### API Endpoints

All endpoints require JWT authentication via `Authorization: Bearer {token}` header.

#### Reservation Endpoints (`/api/v1/reservations`)

- **POST** `/api/v1/reservations` - Create reservation (PENDING_PAYMENT, 5-min lock)
- **POST** `/api/v1/reservations/{id}/complete-payment` - Complete payment (→ CONFIRMED, generates QR)
- **GET** `/api/v1/reservations/my` - Get user's reservations
- **GET** `/api/v1/reservations/my/count` - Get user's active reservation count
- **GET** `/api/v1/reservations/{id}` - Get specific reservation
- **GET** `/api/v1/reservations` - Get all reservations (paginated, Admin/Manager)
- **DELETE** `/api/v1/reservations/{id}` - Cancel reservation
- **POST** `/api/v1/reservations/stalls/{stallId}/hold` - Temporarily hold stall

#### QR Code Endpoints (`/api/v1/qrcode`)

- **GET** `/api/v1/qrcode/regenerate/{reservationId}` - Regenerate QR code

#### Venue Map Endpoints (`/api/v1/venue/map`)

- **GET** `/api/v1/venue/map` - Get complete venue map with all stalls
- **GET** `/api/v1/venue/map/code/{code}` - Get venue map by venue code
- **GET** `/api/v1/venue/map/available` - Get only available stalls

### Service-to-Service Communication

The Reservation Service uses **Spring Cloud OpenFeign** clients:

**UserServiceClient (Port 8080):**
- `GET /api/v1/users/{userId}` - Fetch user details and validate user

**StallServiceClient (Port 8081):**
- `GET /api/v1/stalls/{stallId}` - Fetch stall details
- `GET /api/v1/stalls` - Fetch all stalls (for venue map)
- `PUT /api/v1/stalls/{stallId}/reserve` - Reserve a stall
- `PUT /api/v1/stalls/{stallId}/release` - Release a stall
- `POST /api/v1/stalls/{stallId}/hold` - Temporarily hold a stall

**QR Code Generation:**
- QR codes are generated internally using Google ZXing library
- No external QRCode Service required

## 🔒 Security & Data Consistency

### Distributed Transaction Management

The service implements compensating transactions to maintain consistency across services:

```java
@Transactional
public ReservationResponse createReservation(...) {
    // 1. Validate user via User Service
    // 2. Check reservation limit
    // 3. Validate stall via Stall Service
    // 4. Reserve stall via Stall Service
    
    try {
        // 5. Create reservation record
        // 6. Generate QR code via QRCode Service
        // 7. Update reservation with QR code
    } catch (Exception e) {
        // COMPENSATE: Release stall if reservation fails
        stallServiceClient.releaseStall(stallId);
        throw e;
    }
}
```

### Security Features

- **Header-based Authentication:** `X-User-Id` header set by API Gateway
- **User Validation:** All requests validate user existence via User Service
- **Authorization:** Reservation ownership verified before cancellation
- **Input Validation:** Request DTOs use Jakarta Validation
- **CORS Configuration:** Configured for cross-origin requests

### Race Condition Prevention

While the Reservation Service uses `@Transactional` annotations, the actual race condition prevention for stall availability is handled by the **Stall Service** using pessimistic locking at the database level. This ensures that concurrent reservation attempts for the same stall are properly serialized.

**Flow:**
1. User A and User B both request stall #5
2. Reservation Service → Stall Service (reserve stall #5)
3. Stall Service locks the stall record
4. First request succeeds, second request fails
5. Only one reservation is created

### Error Handling

The service includes comprehensive exception handling:

- `ResourceNotFoundException` - User or Stall not found
- `StallNotAvailableException` - Stall already reserved
- `ReservationLimitExceededException` - User exceeded 3-stall limit
- `GlobalExceptionHandler` - Centralized error responses

## 🎯 Business Rules

1. **Maximum Reservations:** Each user can reserve up to 3 stalls (enforced by Reservation Service)
2. **Payment Lock:** New reservations start as PENDING_PAYMENT with 5-minute payment window
3. **Payment Expiration:** Unpaid reservations automatically expire and release the stall
4. **QR Code Generation:** QR codes generated automatically upon payment completion
5. **Stall Availability:** Stall availability managed by Stall Service with pessimistic locking
6. **User Validation:** All users must exist and be validated via User Service
7. **Reservation Cancellation:** Cancelled reservations automatically release the stall
8. **Authorization:** Users can only view/modify their own reservations (except Admin/Manager)
9. **Distributed Consistency:** Compensating transactions ensure data consistency across services
10. **Event Sourcing:** All reservation state changes publish Kafka events

## 📊 System Flow

### Reservation Creation Process

```
1. User authenticated via Identity Service → JWT + X-User-Id header
2. Request sent to Reservation Service
3. Reservation Service validates:
   ├── User exists (OpenFeign → User Service)
   ├── User hasn't exceeded 3 active reservations
   └── Stall exists and is available (OpenFeign → Stall Service)
4. If valid:
   ├── Reserve stall (OpenFeign → Stall Service with pessimistic lock)
   ├── Create reservation record with PENDING_PAYMENT status
   ├── Set payment_expires_at = now + 5 minutes
   ├── Publish reservation.created event (Kafka)
   └── Return reservation details (no QR code yet)
5. If any step fails after stall reservation:
   └── Release stall (compensating transaction via Feign)

--- User completes payment within 5 minutes ---

6. Complete Payment Request:
   ├── Update status to CONFIRMED
   ├── Set payment_completed_at
   ├── Generate QR code (Google ZXing)
   ├── Store QR code in reservation
   ├── Publish payment.completed event (Kafka)
   └── Return reservation with QR code

--- Or payment times out ---

7. Scheduled Cleanup Task (runs every minute):
   ├── Find reservations where payment_expires_at < now AND status = PENDING_PAYMENT
   ├── Update status to EXPIRED
   ├── Release stall (OpenFeign → Stall Service)
   └── Publish payment.expired event (Kafka)
```

### Service Communication Flow

```
┌──────────┐
│  Client  │
└─────┬────┘
      │ POST /api/v1/reservations
      │ Authorization: Bearer {JWT}
      ▼
┌─────────────────────┐
│ Reservation Service │
│    (Port 8084)      │
└─────────┬───────────┘
          │
          ├──── OpenFeign ────┐
          │                   │
          │                   │
          ▼                   ▼
   ┌──────────────┐    ┌──────────────┐
   │ User Service │    │ Stall Service│
   │ (Port 8080)  │    │ (Port 8081)  │
   └──────────────┘    └──────┬───────┘
                              │
                              ▼
                       ┌──────────────┐
                       │  PostgreSQL  │
                       │  (Stalls DB) │
                       └──────────────┘
          │
          ▼ Publish Events
   ┌──────────────┐
   │ Apache Kafka │
   │ (Port 9092)  │
   └──────┬───────┘
          │
          ▼ Consume Events
   ┌──────────────┐
   │ Event Handler│
   │ (stall.*)    │
   └──────────────┘
```

### Cancellation Flow

```
1. User requests cancellation
2. Reservation Service:
   ├── Validates reservation exists
   ├── Verifies user owns the reservation
   ├── Updates status to CANCELLED
   └── Releases stall (call Stall Service)
3. Return success
```

## 🧪 Testing

### Manual Testing

**Prerequisites:**
- Ensure all dependent services are running (User, Stall, QRCode services)
- Have test user IDs from the User Service

1. **Test reservation creation**
   - Create reservations for different users
   - Verify QR codes are generated
   - Try to exceed 3-stall limit
   - Try to book already reserved stall

2. **Test user reservations retrieval**
   - Get reservations for specific user
   - Verify correct user and stall data is returned

3. **Test reservation cancellation**
   - Cancel a reservation
   - Verify stall is released in Stall Service
   - Try to cancel someone else's reservation (should fail)

4. **Test admin functions**
   - Get all reservations
   - Verify all user and stall data is populated

### Testing with Multiple Services

To test the complete flow:

```bash
# Terminal 1: Start User Service
cd user-service && ./mvnw spring-boot:run

# Terminal 2: Start Stall Service  
cd stall-service && ./mvnw spring-boot:run

# Terminal 3: Start QRCode Service
cd qrcode-service && ./mvnw spring-boot:run

# Terminal 4: Start Reservation Service
cd reservation-service && ./mvnw spring-boot:run

# Terminal 5: Test reservation
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"stallId": 5}'
```

### Concurrent Testing

To test race condition prevention (handled by Stall Service):

```bash
# In terminal 1
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"stallId": 5}'

# In terminal 2 (simultaneously)
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{"stallId": 5}'
```

Expected: One succeeds, one receives "Stall already reserved" error.

## 📁 Project Structure

```
src/
├── main/
│   ├── java/exhibitflow/reservation_service/
│   │   ├── client/              # OpenFeign clients for external APIs
│   │   │   ├── UserServiceClient.java
│   │   │   └── StallServiceClient.java
│   │   ├── config/              # Configuration classes
│   │   │   ├── ApplicationProperties.java
│   │   │   ├── CacheConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   ├── DatabaseConfig.java
│   │   │   ├── FeignClientConfig.java
│   │   │   ├── FeignClientErrorDecoder.java
│   │   │   ├── KafkaConfig.java
│   │   │   ├── MDCFilter.java
│   │   │   └── OpenApiConfig.java
│   │   ├── constants/           # Application constants
│   │   │   └── HeaderConstants.java
│   │   ├── consumer/            # Kafka event consumers
│   │   │   └── StallEventConsumer.java
│   │   ├── controller/          # REST endpoints
│   │   │   ├── ReservationController.java
│   │   │   ├── QRCodeController.java
│   │   │   └── VenueMapController.java
│   │   ├── dto/                 # Data Transfer Objects & Events
│   │   │   ├── CreateReservationRequest.java
│   │   │   ├── CompletePaymentRequest.java
│   │   │   ├── ReservationResponse.java
│   │   │   ├── ReservationSummary.java
│   │   │   ├── PagedResponse.java
│   │   │   ├── UserDto.java
│   │   │   ├── StallDto.java
│   │   │   ├── VenueMapResponse.java
│   │   │   ├── QRCodeRequest.java
│   │   │   ├── ReservationCreatedEvent.java
│   │   │   ├── PaymentCompletedEvent.java
│   │   │   ├── ReservationCancelledEvent.java
│   │   │   ├── PaymentExpiredEvent.java
│   │   │   ├── StallCreatedEvent.java
│   │   │   ├── StallUpdatedEvent.java
│   │   │   └── ErrorResponse.java
│   │   ├── entity/              # JPA entities
│   │   │   └── Reservation.java
│   │   ├── exception/           # Custom exceptions & handlers
│   │   ├── repository/          # Data access layer
│   │   │   └── ReservationRepository.java
│   │   ├── service/             # Business logic & orchestration
│   │   │   ├── ReservationService.java
│   │   │   ├── QRCodeGeneratorService.java
│   │   │   ├── VenueMapService.java
│   │   │   └── ReservationCleanupService.java
│   │   ├── util/                # Utility classes
│   │   └── ReservationServiceApplication.java
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           └── V1__initial_schema.sql
└── test/
    └── java/exhibitflow/reservation_service/
        ├── ReservationServiceApplicationTests.java
        ├── config/
        └── integration/
```

## 🔧 Configuration Options

### Database Configuration (PostgreSQL)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5532/reservation_db
spring.datasource.username=postgres
spring.datasource.password=asd
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=validate    # Always use validate with Flyway
spring.jpa.show-sql=true                  # Disable in production
spring.jpa.properties.hibernate.dialect=org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect
```

### Flyway Configuration
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

### Microservices URLs
```properties
services.user-service.url=http://{server-url}:8080
services.stall-service.url=http://{server-url}:8081
```

### Kafka Configuration
```properties
spring.kafka.bootstrap-servers=localhost:9092
kafka.topics.reservation-created=reservation.created
kafka.topics.payment-completed=reservation.payment.completed
kafka.topics.reservation-cancelled=reservation.cancelled
kafka.topics.payment-expired=reservation.payment.expired
kafka.topics.stall-created=stall.created
kafka.topics.stall-updated=stall.updated
```

### JWT Configuration
```properties
jwt.secret=your-secret-key-here
jwt.expiration=86400000
```

### Business Configuration
```properties
reservation.reservation.max-reservations-per-user=3
reservation.reservation.payment-lock-minutes=5
reservation.cleanup.fixed-rate-ms=60000
```

### QR Code Configuration
```properties
reservation.qr-code.width=300
reservation.qr-code.height=300
reservation.qr-code.error-correction-level=H
```

### Application Configuration
```properties
server.port=8084
spring.application.name=reservation-service
logging.level.exhibitflow.reservation_service=DEBUG
```

## 🐛 Troubleshooting

### Common Issues

**1. Service unavailable errors**
- Verify all dependent services are running:
  - User Service (http://{server-url}:8080)
  - Stall Service (http://{server-url}:8081)
- Check service URLs in `application.properties`
- Review Feign client logs for connection errors
- Verify network connectivity to external services

**2. User not found**
- Ensure the user exists in User Service
- Verify the `X-User-Id` header is set correctly
- Check JWT token is valid
- Check User Service is accessible

**3. Stall not found or already reserved**
- Verify the stall exists in Stall Service
- Check stall availability via Stall Service API
- Use a different stall ID
- Check if stall is in PENDING_PAYMENT by another user

**4. Database connection failed**
- Verify PostgreSQL is running: `docker ps`
- Check database credentials in `application.properties`
- Ensure `reservation_db` database exists
- Verify PostGIS extension is installed
- Check port 5532 is accessible

**5. Kafka connection issues**
- Verify Kafka is running: `docker ps`
- Check Kafka UI at http://localhost:8090
- Verify bootstrap servers: `localhost:9092`
- Check topic creation in Kafka UI

**6. QR code not generated**
- QR codes are only generated after payment completion
- Check Google ZXing library is included in dependencies
- Verify QR code configuration in `application.properties`
- Review application logs for QR generation errors

**7. Payment expiration not working**
- Check scheduled cleanup service is enabled: `@EnableScheduling`
- Verify cleanup rate: `reservation.cleanup.fixed-rate-ms=60000`
- Check application logs for cleanup task execution

**8. Transaction rollback issues**
- Check all services are responding correctly
- Review database transaction logs
- Ensure compensating transactions are working (stall release on failure)
- Verify Feign retry logic in `FeignClientConfig`

**9. Cache issues**
- Clear cache manually via cache manager
- Verify Redis/cache configuration in `CacheConfig`
- Check Kafka consumer for stall events

**10. Flyway migration errors**
- Check migration files in `src/main/resources/db/migration`
- Verify Flyway baseline: `spring.flyway.baseline-on-migrate=true`
- Review Flyway schema history table
- Run migrations manually if needed

## 📝 Future Enhancements

### Service Improvements
- [ ] Implement circuit breakers (Resilience4j) for service calls
- [ ] Add service discovery (Eureka/Consul)
- [ ] Implement API Gateway integration
- [ ] Add distributed tracing (Zipkin/Jaeger)
- [ ] Implement message queue for async operations (RabbitMQ/Kafka)

### Feature Enhancements
- [ ] Reservation modification endpoint
- [ ] Reservation expiration logic
- [ ] Email notifications via dedicated service
- [ ] SMS notifications integration
- [ ] Event scheduling (multiple bookfairs)
- [ ] Payment integration
- [ ] Reservation analytics and reporting

### Technical Improvements
- [ ] Add comprehensive unit and integration tests
- [ ] Implement caching (Redis) for user/stall data
- [ ] Add health checks and metrics (Actuator)
- [ ] Implement rate limiting
- [ ] Add API versioning
- [ ] Docker containerization
- [ ] Kubernetes deployment configuration
- [ ] CI/CD pipeline setup



## 👥 Authors

- **ExhibitFlow Team** - *Initial work*

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- MySQL team for the robust database system
- All contributors and testers
- The ExhibitFlow microservices ecosystem team
