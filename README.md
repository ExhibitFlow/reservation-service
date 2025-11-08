# Colombo International Bookfair - Reservation Service

A complete backend microservice for managing stall reservations at the Colombo International Bookfair. This Spring Boot application handles user authentication, stall management, reservations, QR code generation, and email notifications - all in a single, comprehensive service.

## 🎯 Features

- **User Authentication & Authorization**
  - JWT-based authentication
  - Role-based access control (USER and ADMIN)
  - Secure password hashing with BCrypt
  
- **Stall Management**
  - 26 pre-configured stalls (A-Z)
  - Three sizes: SMALL, MEDIUM, LARGE
  - Real-time availability tracking
  
- **Reservation System**
  - Maximum 3 stalls per business
  - Atomic reservation operations
  - Race condition prevention with pessimistic locking
  - Automatic QR code generation
  - Email confirmation with QR attachment
  
- **Admin Portal Support**
  - View all stalls and their status
  - Monitor all reservations
  - Complete oversight of the system

## 🏗️ Architecture

### Technology Stack

- **Backend Framework:** Spring Boot 3.5.7
- **Java Version:** 21
- **Security:** Spring Security + JWT (JJWT 0.12.3)
- **Database:** MySQL with Spring Data JPA
- **Email:** Spring Mail
- **QR Generation:** ZXing (Google) 3.5.3
- **Build Tool:** Maven

### Database Schema

```
users
├── id (PK)
├── name
├── email (Unique)
├── password (Hashed)
├── business_name
├── role (USER/ADMIN)
└── created_at

stalls
├── id (PK)
├── stall_code (Unique, A-Z)
├── size (SMALL/MEDIUM/LARGE)
├── is_reserved
├── metadata
└── version (Optimistic locking)

reservations
├── id (PK)
├── user_id (FK)
├── stall_id (FK)
├── created_at
├── qr_code_base64
└── status (CONFIRMED/CANCELLED)
```

## 🚀 Getting Started

### Prerequisites

- Java 21 or higher
- MySQL 8.0 or higher
- Maven 3.6+
- SMTP server credentials (e.g., Gmail)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/ExhibitFlow/Reservation-service.git
cd Reservation-service
```

2. **Create MySQL database**
```sql
CREATE DATABASE bookfair_reservation;
```

3. **Configure application properties**

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/bookfair_reservation?createDatabaseIfNotExist=true
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password

# JWT (Generate a secure key for production)
jwt.secret=your-secret-key-should-be-at-least-256-bits-long-for-HS256-algorithm-security-purposes
jwt.expiration=86400000

# Email (Gmail example)
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

4. **Build the project**
```bash
./mvnw clean install
```

5. **Run the application**
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### First Run

On the first run, the application automatically:
- Creates 26 stalls (A-Z) with varying sizes
- Creates a default admin user:
  - Email: `admin@bookfair.com`
  - Password: `admin123`

## 📚 API Documentation

### Quick Start Examples

#### 1. Register a User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "businessName": "Johns Books Store"
  }'
```

#### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```
Save the `token` from the response!

#### 3. View Available Stalls
```bash
curl http://localhost:8080/api/stalls?available=true
```

#### 4. Create Reservation
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"stallId": 1}'
```

#### 5. View My Reservations
```bash
curl http://localhost:8080/api/reservations/my \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Complete API Reference

See [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) for detailed API documentation including:
- All endpoints with request/response examples
- Error handling
- Authentication details
- Postman collection

### Postman Collection

Import `Bookfair_Reservation_API.postman_collection.json` into Postman for easy testing.

## 🔒 Security Features

### JWT Authentication
- Stateless authentication
- Token expiration (24 hours default)
- Secure password storage with BCrypt

### Role-Based Access Control
- **USER Role:** Can create and view their own reservations
- **ADMIN Role:** Can view all stalls and reservations

### Race Condition Prevention

The system uses **pessimistic locking** to prevent double-booking:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Stall s WHERE s.id = :id")
Optional<Stall> findByIdWithLock(@Param("id") Long id);
```

**How it works:**
1. When a reservation request arrives, the stall is locked at the database level
2. Availability is checked while the lock is held
3. If available, the stall is marked as reserved
4. Transaction commits and releases the lock
5. Concurrent requests wait until the lock is released

This ensures that two users cannot reserve the same stall simultaneously.

### Additional Security Measures
- **@Transactional** annotations for atomic operations
- **Version field** for optimistic locking
- Input validation on all DTOs
- Global exception handling
- CORS configuration for frontend integration

## 📧 Email Notifications

Upon successful reservation, users receive an HTML email containing:
- Reservation confirmation details
- Business information
- Stall details (code, size)
- Embedded QR code image
- Instructions for check-in

### Gmail Configuration

1. Enable 2-factor authentication on your Google account
2. Generate an app-specific password
3. Use these credentials in `application.properties`

## 📱 QR Code Generation

QR codes are generated using the ZXing library and contain:
- Reservation ID
- User Name
- Stall Code
- Event Name

The QR code is:
- Generated as a PNG image
- Encoded to Base64 string
- Stored in the database
- Embedded in confirmation email
- Returned in API responses

## 🎯 Business Rules

1. **Maximum Reservations:** Each user can reserve up to 3 stalls
2. **Stall Availability:** Reserved stalls are immediately unavailable to others
3. **Concurrent Safety:** Atomic operations prevent race conditions
4. **Email Notifications:** Sent automatically on successful reservation
5. **Role Separation:** Users and admins have different access levels

## 📊 System Flow

### Reservation Process

```
1. User registers/logs in → Receives JWT token
2. User views available stalls → GET /api/stalls?available=true
3. User selects a stall → POST /api/reservations with stallId
4. System checks:
   - User hasn't exceeded 3 reservations
   - Stall is available (with pessimistic lock)
5. If valid:
   - Mark stall as reserved
   - Create reservation record
   - Generate QR code
   - Store QR with reservation
   - Send confirmation email
6. Return reservation details with QR code
```

### Race Condition Scenario

**Without Locking (BAD):**
```
Time  User A              User B
t1    Check stall (free)  
t2                        Check stall (free)
t3    Reserve stall       
t4                        Reserve stall (DOUBLE BOOKING!)
```

**With Pessimistic Locking (GOOD):**
```
Time  User A              User B
t1    Lock & check        
t2                        Wait for lock...
t3    Reserve & commit    
t4    Release lock        
t5                        Lock & check (already reserved)
t6                        Return error (stall unavailable)
```

## 🧪 Testing

### Manual Testing

1. **Start the application**
2. **Test user registration**
   - Register multiple users
   - Verify JWT tokens are returned
3. **Test stall retrieval**
   - Get all stalls
   - Filter by availability
   - Filter by size
4. **Test reservations**
   - Create reservations for different stalls
   - Try to exceed 3-stall limit
   - Try to book already reserved stall
5. **Test admin functions**
   - Login as admin
   - View all reservations
   - Check all stalls

### Concurrent Testing

To test race condition prevention:

```bash
# In terminal 1
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer USER1_TOKEN" \
  -d '{"stallId": 1}'

# In terminal 2 (simultaneously)
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer USER2_TOKEN" \
  -d '{"stallId": 1}'
```

Expected: One succeeds, one receives "Stall already reserved" error.

## 📁 Project Structure

```
src/
├── main/
│   ├── java/exhibitflow/reservation_service/
│   │   ├── config/              # Database initialization
│   │   ├── controller/          # REST endpoints
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── entity/              # JPA entities
│   │   ├── exception/           # Custom exceptions & handlers
│   │   ├── repository/          # Data access layer
│   │   ├── security/            # JWT & Spring Security config
│   │   ├── service/             # Business logic
│   │   └── ReservationServiceApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/exhibitflow/reservation_service/
        └── ReservationServiceApplicationTests.java
```

## 🔧 Configuration Options

### Database Configuration
```properties
spring.jpa.hibernate.ddl-auto=update    # Use 'validate' in production
spring.jpa.show-sql=true                # Disable in production
```

### JWT Configuration
```properties
jwt.secret=your-secure-secret-key
jwt.expiration=86400000                 # 24 hours in milliseconds
```

### Email Configuration
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

## 🐛 Troubleshooting

### Common Issues

**1. Email not sending**
- Check SMTP credentials
- For Gmail, use app-specific password
- Verify firewall allows SMTP port 587

**2. JWT token expired**
- Tokens expire after 24 hours by default
- Re-login to get a new token

**3. Database connection failed**
- Verify MySQL is running
- Check database credentials
- Ensure database exists

**4. Stall already reserved**
- This is expected behavior
- Choose a different stall
- Use `?available=true` to see only free stalls

## 📝 Future Enhancements

- [ ] Async email sending with @Async
- [ ] Reservation cancellation endpoint
- [ ] Payment integration
- [ ] SMS notifications
- [ ] Event scheduling (multiple bookfairs)
- [ ] Stall pricing based on size
- [ ] PDF invoice generation
- [ ] Admin dashboard metrics
- [ ] WebSocket for real-time updates
- [ ] Docker containerization

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👥 Authors

- **ExhibitFlow Team** - *Initial work*

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- ZXing team for QR code generation
- JJWT team for JWT implementation
- All contributors and testers
