# Reservation Service API Documentation

## Overview

The Reservation Service manages stall reservations for the ExhibitFlow platform. It handles the complete reservation lifecycle including creating temporary payment locks, completing payments, and generating QR codes for confirmed reservations.

**Base URL:** `http://localhost:8080/api/reservations`

**Authentication:** All endpoints require the `X-User-Id` header (extracted from JWT by API Gateway)

---

## Table of Contents

1. [Create Reservation](#11-create-reservation)
2. [Complete Payment](#12-complete-payment)
3. [Get My Reservations](#13-get-my-reservations)
4. [Get All Reservations (Admin)](#14-get-all-reservations-admin)
5. [Cancel Reservation](#15-cancel-reservation)
6. [Error Responses](#error-responses)

---

## 1. Reservation Endpoints

### 1.1 Create Reservation

**Endpoint:** `POST /api/reservations`

**Description:** Create a new stall reservation with a temporary 5-minute payment lock

**Headers:**
```
X-User-Id: <userId> (required)
Content-Type: application/json
```

**Request Body:**

```json
{
  "stallId": 1
}
```

**Request Fields:**
- `stallId` (Long, required): The ID of the stall to reserve

**Response:** `201 Created`

```json
{
  "id": 1,
  "userId": 123,
  "userName": "John Doe",
  "userEmail": "vendor@example.com",
  "businessName": "ABC Publishers",
  "stallId": 1,
  "stallCode": "A-001",
  "stallSize": "MEDIUM",
  "createdAt": "2025-11-11T10:00:00",
  "status": "PENDING_PAYMENT",
  "qrCodeBase64": null,
  "paymentExpiresAt": "2025-11-11T10:05:00",
  "paymentCompletedAt": null
}
```

**Response Fields:**
- `id` (Long): Unique reservation identifier
- `userId` (Long): User who made the reservation
- `userName` (String): Name of the user
- `userEmail` (String): Email of the user
- `businessName` (String): Business name of the vendor
- `stallId` (Long): Reserved stall ID
- `stallCode` (String): Stall identification code
- `stallSize` (String): Size of the stall (SMALL, MEDIUM, LARGE)
- `createdAt` (DateTime): Timestamp when reservation was created
- `status` (String): Reservation status (PENDING_PAYMENT, CONFIRMED, CANCELLED, EXPIRED)
- `qrCodeBase64` (String): Base64 encoded QR code (null until payment completed)
- `paymentExpiresAt` (DateTime): Expiration time for payment (5 minutes from creation)
- `paymentCompletedAt` (DateTime): Timestamp when payment was completed (null if pending)

**Error Responses:**

```json
// 400 Bad Request - Validation Error
{
  "stallId": "Stall ID is required"
}

// 400 Bad Request - Stall Not Available
{
  "status": 400,
  "message": "Stall is not available for reservation",
  "error": "Stall Not Available",
  "timestamp": 1699689600000
}

// 400 Bad Request - Reservation Limit Exceeded
{
  "status": 400,
  "message": "User has reached maximum reservation limit",
  "error": "Reservation Limit Exceeded",
  "timestamp": 1699689600000
}

// 404 Not Found - Stall Not Found
{
  "status": 404,
  "message": "Stall not found with ID: 1",
  "error": "Resource Not Found",
  "timestamp": 1699689600000
}

// 409 Conflict - Duplicate Reservation
{
  "status": 409,
  "message": "User already has a reservation for this stall",
  "error": "Duplicate Resource",
  "timestamp": 1699689600000
}
```

---

### 1.2 Complete Payment

**Endpoint:** `POST /api/reservations/{reservationId}/complete-payment`

**Description:** Complete payment for a pending reservation and generate QR code

**Headers:**
```
X-User-Id: <userId> (required)
```

**Path Parameters:**
- `reservationId` (Long): The ID of the reservation to complete payment for

**Request Body:** None

**Response:** `200 OK`

```json
{
  "id": 1,
  "userId": 123,
  "userName": "John Doe",
  "userEmail": "vendor@example.com",
  "businessName": "ABC Publishers",
  "stallId": 1,
  "stallCode": "A-001",
  "stallSize": "MEDIUM",
  "createdAt": "2025-11-11T10:00:00",
  "status": "CONFIRMED",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51...",
  "paymentExpiresAt": "2025-11-11T10:05:00",
  "paymentCompletedAt": "2025-11-11T10:03:00"
}
```

**Error Responses:**

```json
// 400 Bad Request - Payment Expired
{
  "status": 400,
  "message": "Payment lock has expired. Please create a new reservation",
  "error": "Payment Expired",
  "timestamp": 1699689600000
}

// 400 Bad Request - Invalid Operation
{
  "status": 400,
  "message": "Cannot complete payment for reservation with status: CONFIRMED",
  "error": "Invalid Operation",
  "timestamp": 1699689600000
}

// 401 Unauthorized
{
  "status": 401,
  "message": "User is not authorized to complete payment for this reservation",
  "error": "Unauthorized",
  "timestamp": 1699689600000
}

// 404 Not Found
{
  "status": 404,
  "message": "Reservation not found with ID: 1",
  "error": "Resource Not Found",
  "timestamp": 1699689600000
}
```

---

### 1.3 Get My Reservations

**Endpoint:** `GET /api/reservations/my`

**Description:** Get all reservations for the authenticated user

**Headers:**
```
X-User-Id: <userId> (required)
```

**Request Body:** None

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "userId": 123,
    "userName": "John Doe",
    "userEmail": "vendor@example.com",
    "businessName": "ABC Publishers",
    "stallId": 1,
    "stallCode": "A-001",
    "stallSize": "MEDIUM",
    "createdAt": "2025-11-11T10:00:00",
    "status": "CONFIRMED",
    "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51...",
    "paymentExpiresAt": "2025-11-11T10:05:00",
    "paymentCompletedAt": "2025-11-11T10:03:00"
  },
  {
    "id": 2,
    "userId": 123,
    "userName": "John Doe",
    "userEmail": "vendor@example.com",
    "businessName": "ABC Publishers",
    "stallId": 5,
    "stallCode": "B-003",
    "stallSize": "LARGE",
    "createdAt": "2025-11-11T11:00:00",
    "status": "PENDING_PAYMENT",
    "qrCodeBase64": null,
    "paymentExpiresAt": "2025-11-11T11:05:00",
    "paymentCompletedAt": null
  }
]
```

**Error Responses:**

```json
// 500 Internal Server Error
{
  "status": 500,
  "message": "Error fetching user reservations",
  "error": "Internal Server Error",
  "timestamp": 1699689600000
}
```

---

### 1.4 Get All Reservations (Admin)

**Endpoint:** `GET /api/reservations`

**Description:** Get all reservations in the system (admin access)

**Headers:**
```
X-User-Id: <userId> (required)
```

**Request Body:** None

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "userId": 123,
    "userName": "John Doe",
    "userEmail": "vendor1@example.com",
    "businessName": "ABC Publishers",
    "stallId": 1,
    "stallCode": "A-001",
    "stallSize": "MEDIUM",
    "createdAt": "2025-11-11T10:00:00",
    "status": "CONFIRMED",
    "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51...",
    "paymentExpiresAt": "2025-11-11T10:05:00",
    "paymentCompletedAt": "2025-11-11T10:03:00"
  },
  {
    "id": 2,
    "userId": 456,
    "userName": "Jane Smith",
    "userEmail": "vendor2@example.com",
    "businessName": "XYZ Books",
    "stallId": 5,
    "stallCode": "B-003",
    "stallSize": "LARGE",
    "createdAt": "2025-11-11T11:00:00",
    "status": "PENDING_PAYMENT",
    "qrCodeBase64": null,
    "paymentExpiresAt": "2025-11-11T11:05:00",
    "paymentCompletedAt": null
  }
]
```

**Error Responses:**

```json
// 500 Internal Server Error
{
  "status": 500,
  "message": "Error fetching all reservations",
  "error": "Internal Server Error",
  "timestamp": 1699689600000
}
```

---

### 1.5 Cancel Reservation

**Endpoint:** `DELETE /api/reservations/{reservationId}`

**Description:** Cancel a reservation

**Headers:**
```
X-User-Id: <userId> (required)
```

**Path Parameters:**
- `reservationId` (Long): The ID of the reservation to cancel

**Request Body:** None

**Response:** `204 No Content`

**Error Responses:**

```json
// 401 Unauthorized
{
  "status": 401,
  "message": "User is not authorized to cancel this reservation",
  "error": "Unauthorized",
  "timestamp": 1699689600000
}

// 404 Not Found
{
  "status": 404,
  "message": "Reservation not found with ID: 1",
  "error": "Resource Not Found",
  "timestamp": 1699689600000
}

// 400 Bad Request - Invalid Operation
{
  "status": 400,
  "message": "Cannot cancel reservation in EXPIRED status",
  "error": "Invalid Operation",
  "timestamp": 1699689600000
}
```

---

## Error Responses

All error responses follow a consistent format:

### Standard Error Response Format

```json
{
  "status": 400,
  "message": "Detailed error message",
  "error": "Error Type",
  "timestamp": 1699689600000
}
```

### HTTP Status Codes

- `200 OK` - Request succeeded
- `201 Created` - Resource created successfully
- `204 No Content` - Request succeeded with no response body
- `400 Bad Request` - Invalid request data or business rule violation
- `401 Unauthorized` - User not authorized to perform action
- `404 Not Found` - Requested resource not found
- `409 Conflict` - Resource already exists
- `500 Internal Server Error` - Server-side error

### Error Types

- **Validation Error** - Request data validation failed
- **Resource Not Found** - Requested resource does not exist
- **Duplicate Resource** - Resource already exists
- **Stall Not Available** - Stall is not available for reservation
- **Reservation Limit Exceeded** - User has reached maximum reservation limit
- **Payment Expired** - Payment lock has expired (5-minute window)
- **Invalid Operation** - Operation not allowed in current state
- **Unauthorized** - User not authorized for the operation
- **Internal Server Error** - Unexpected server error

---

## Business Rules

### Reservation Lifecycle

1. **PENDING_PAYMENT**: Initial state when reservation is created
   - 5-minute payment window
   - Stall is temporarily locked
   - No QR code generated yet

2. **CONFIRMED**: Payment completed successfully
   - Stall permanently reserved
   - QR code generated and attached
   - Can be cancelled by user

3. **EXPIRED**: Payment window expired
   - Stall becomes available again
   - Automatic cleanup by background service

4. **CANCELLED**: User cancelled the reservation
   - Stall becomes available again
   - Cannot be reactivated

### Payment Lock Feature

- When a reservation is created, the stall is locked for 5 minutes
- User must complete payment within this window
- After expiration, the reservation is automatically marked as EXPIRED
- Background cleanup service runs periodically to release expired locks

### Reservation Limits

- Users may have a maximum number of active reservations
- Limit is enforced during reservation creation
- Only CONFIRMED and PENDING_PAYMENT reservations count toward limit

---

## Integration Notes

### Required External Services

1. **User Service** (`http://localhost:8081`)
   - Fetches user details (name, email, business name)
   - Endpoint: `GET /api/users/{userId}`

2. **Stall Service** (`http://localhost:8082`)
   - Checks stall availability
   - Marks stall as reserved/available
   - Endpoints:
     - `GET /api/stalls/{stallId}`
     - `POST /api/stalls/{stallId}/reserve`
     - `POST /api/stalls/{stallId}/release`

3. **QR Code Service** (`http://localhost:8083`)
   - Generates QR codes for confirmed reservations
   - Endpoint: `POST /api/qrcode/generate`

### Authentication Flow

```
Client Request
    ↓
API Gateway (validates JWT, extracts userId)
    ↓
Reservation Service (receives X-User-Id header)
    ↓
Process Request
```

---

## Example Usage Flows

### Flow 1: Successful Reservation with Payment

```bash
# Step 1: Create reservation (5-minute payment lock starts)
curl -X POST http://localhost:8080/api/reservations \
  -H "X-User-Id: 123" \
  -H "Content-Type: application/json" \
  -d '{"stallId": 1}'

# Response: reservation with status "PENDING_PAYMENT"

# Step 2: Complete payment (within 5 minutes)
curl -X POST http://localhost:8080/api/reservations/1/complete-payment \
  -H "X-User-Id: 123"

# Response: reservation with status "CONFIRMED" and QR code
```

### Flow 2: Payment Timeout

```bash
# Step 1: Create reservation
curl -X POST http://localhost:8080/api/reservations \
  -H "X-User-Id: 123" \
  -H "Content-Type: application/json" \
  -d '{"stallId": 1}'

# Wait more than 5 minutes...

# Step 2: Attempt to complete payment (will fail)
curl -X POST http://localhost:8080/api/reservations/1/complete-payment \
  -H "X-User-Id: 123"

# Response: 400 Bad Request - "Payment lock has expired"
```

### Flow 3: View and Cancel Reservation

```bash
# Step 1: Get my reservations
curl -X GET http://localhost:8080/api/reservations/my \
  -H "X-User-Id: 123"

# Response: Array of user's reservations

# Step 2: Cancel a reservation
curl -X DELETE http://localhost:8080/api/reservations/1 \
  -H "X-User-Id: 123"

# Response: 204 No Content
```

---

## Testing Notes

### Test Data Setup

Ensure the following services are running:
- User Service on port 8081
- Stall Service on port 8082
- QR Code Service on port 8083
- Reservation Service on port 8080
- MySQL database on port 3306

### Database Schema

The service automatically creates the `reservation_db` database and `reservations` table on startup.

### Background Services

The cleanup service runs every minute to:
- Mark expired reservations (payment window > 5 minutes)
- Release stall locks for expired reservations

---

## Changelog

### Version 1.0.0 (Current)

- Initial release
- Create reservation with payment lock
- Complete payment
- Get user reservations
- Get all reservations (admin)
- Cancel reservations
- Automatic cleanup of expired reservations
- QR code generation integration

---

## Support

For issues or questions, please contact the ExhibitFlow development team.

**Service Repository:** `Reservation-service`  
**Service Port:** `8080`  
**Database:** `reservation_db` (MySQL)
