# Reservation Service API Testing Guide

## Prerequisites
- Spring Boot application running on `http://localhost:8080`
- MySQL database `reservation_db` created and accessible
- The following microservices should be running for full functionality:
  - User Service: `http://localhost:8081`
  - Stall Service: `http://localhost:8082`
  - QR Code Service: `http://localhost:8083`

**Note:** This service depends on other microservices. Without them running, create/update operations will fail when trying to fetch user/stall details.

## API Endpoints

### 1. Get All Reservations (Admin)
**Endpoint:** `GET /api/reservations`

**Description:** Retrieves all reservations in the system

**Request:**
```bash
curl -X GET http://localhost:8080/api/reservations \
  -H "Content-Type: application/json"
```

**Expected Response (Empty initially):**
```json
[]
```

---

### 2. Get My Reservations (User)
**Endpoint:** `GET /api/reservations/my`

**Description:** Retrieves all reservations for a specific user

**Headers Required:**
- `X-User-Id`: The ID of the user

**Request:**
```bash
curl -X GET http://localhost:8080/api/reservations/my \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1"
```

**Expected Response:**
```json
[]
```

---

### 3. Create a Reservation
**Endpoint:** `POST /api/reservations`

**Description:** Creates a new reservation for a user

**Headers Required:**
- `X-User-Id`: The ID of the user creating the reservation

**Request Body:**
```json
{
  "stallId": 5
}
```

**Request:**
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "stallId": 5
  }'
```

**Expected Response (Success - 201 Created):**
```json
{
  "id": 1,
  "userId": 1,
  "userName": "John Doe",
  "userEmail": "john@example.com",
  "businessName": "Tech Corp",
  "stallId": 5,
  "stallCode": "ST-005",
  "stallSize": "Medium",
  "createdAt": "2025-11-09T17:30:00",
  "status": "CONFIRMED",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**Expected Response (Error - Dependencies Not Running):**
```json
{
  "timestamp": "2025-11-09T17:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to fetch user from User Service",
  "path": "/api/reservations"
}
```

---

### 4. Cancel a Reservation
**Endpoint:** `DELETE /api/reservations/{reservationId}`

**Description:** Cancels a specific reservation

**Headers Required:**
- `X-User-Id`: The ID of the user canceling the reservation

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/reservations/1 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1"
```

**Expected Response (Success - 204 No Content):**
```
(Empty response body)
```

**Expected Response (Error - Not Found):**
```json
{
  "timestamp": "2025-11-09T17:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Reservation not found with id: 1",
  "path": "/api/reservations/1"
}
```

---

## Testing Scenarios

### Scenario 1: Basic Health Check
Test if the service is running:

```bash
# This should return the default Spring Boot error page
curl http://localhost:8080
```

### Scenario 2: Test Database Connection
Check if the application can connect to the database:

```bash
# Get all reservations - should return empty array if database is connected
curl -X GET http://localhost:8080/api/reservations
```

### Scenario 3: Test Direct Database Insert (Manual)
You can manually insert data into the database to test the GET endpoints:

```sql
USE reservation_db;

INSERT INTO reservations (user_id, stall_id, created_at, status) 
VALUES (1, 5, NOW(), 'CONFIRMED');

INSERT INTO reservations (user_id, stall_id, created_at, status) 
VALUES (2, 3, NOW(), 'CONFIRMED');
```

Then test:
```bash
# Get all reservations
curl -X GET http://localhost:8080/api/reservations

# Get reservations for user 1
curl -X GET http://localhost:8080/api/reservations/my \
  -H "X-User-Id: 1"
```

### Scenario 4: Test Validation
Test request validation by sending invalid data:

```bash
# Missing stallId - should return 400 Bad Request
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{}'

# Missing X-User-Id header - should return 400 Bad Request
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "stallId": 5
  }'
```

---

## Testing with Postman

### Import Collection
You can create a Postman collection with the following structure:

1. **Get All Reservations**
   - Method: GET
   - URL: `http://localhost:8080/api/reservations`

2. **Get My Reservations**
   - Method: GET
   - URL: `http://localhost:8080/api/reservations/my`
   - Headers: `X-User-Id: 1`

3. **Create Reservation**
   - Method: POST
   - URL: `http://localhost:8080/api/reservations`
   - Headers: `X-User-Id: 1`, `Content-Type: application/json`
   - Body (raw JSON):
     ```json
     {
       "stallId": 5
     }
     ```

4. **Cancel Reservation**
   - Method: DELETE
   - URL: `http://localhost:8080/api/reservations/1`
   - Headers: `X-User-Id: 1`

---

## Common Issues & Troubleshooting

### Issue 1: Connection Refused Errors
**Error:** `Failed to fetch user from User Service`

**Cause:** User Service, Stall Service, or QR Code Service is not running

**Solution:** 
- Either start all dependent microservices
- Or mock the services for testing
- Or directly insert test data into the database

### Issue 2: Database Connection Error
**Error:** `Access denied for user 'root'@'localhost'`

**Solution:** 
- Check MySQL credentials in `application.properties`
- Ensure MySQL server is running
- Verify database `reservation_db` exists

### Issue 3: Port Already in Use
**Error:** `Port 8080 is already in use`

**Solution:**
- Stop other applications using port 8080
- Or change the port in `application.properties`:
  ```properties
  server.port=8081
  ```

---

## Expected Database Schema

The `reservations` table should have the following structure:

```sql
CREATE TABLE reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stall_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    qr_code_base64 LONGTEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    
    INDEX idx_user_id (user_id),
    INDEX idx_stall_id (stall_id),
    INDEX idx_status (status)
);
```

---

## Next Steps

1. **Start all microservices** to enable full functionality
2. **Test with real data** by starting User Service and Stall Service
3. **Implement integration tests** for automated testing
4. **Add API Gateway** for centralized authentication and routing
