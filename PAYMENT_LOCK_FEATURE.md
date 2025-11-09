# Payment Lock Feature - Documentation

## Overview

The Reservation Service now implements a **5-minute temporary lock mechanism** for stalls during the payment process. This prevents race conditions and ensures fair access to stalls while users complete their payment.

---

## How It Works

### Phase 1: Create Reservation (Temporary Lock)

When a user initiates a reservation:

1. **Stall is locked** for 5 minutes
2. **Status**: `PENDING_PAYMENT`
3. **QR code**: NOT generated yet
4. **User has 5 minutes** to complete payment

**Endpoint:**
```
POST /api/reservations
Headers: X-User-Id: {userId}
Body: {
  "stallId": 5
}
```

**Response:**
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
  "status": "PENDING_PAYMENT",
  "createdAt": "2025-11-09T10:30:00",
  "paymentExpiresAt": "2025-11-09T10:35:00",
  "paymentCompletedAt": null,
  "qrCodeBase64": null
}
```

---

### Phase 2: Complete Payment

After successful payment:

1. **Status**: Changes to `CONFIRMED`
2. **QR code**: Generated
3. **Lock**: Becomes permanent reservation
4. **Stall**: Confirmed as reserved

**Endpoint:**
```
POST /api/reservations/{reservationId}/complete-payment
Headers: X-User-Id: {userId}
```

**Response:**
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
  "status": "CONFIRMED",
  "createdAt": "2025-11-09T10:30:00",
  "paymentExpiresAt": "2025-11-09T10:35:00",
  "paymentCompletedAt": "2025-11-09T10:32:15",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

---

## Reservation Status Flow

```
PENDING_PAYMENT (5-minute lock)
    │
    ├──> Payment Completed ──> CONFIRMED (with QR code)
    │
    ├──> User Cancels ──────> CANCELLED (stall released)
    │
    └──> Timeout (5 min) ───> EXPIRED (stall auto-released)
```

---

## Status Types

| Status | Description | Stall State | QR Code |
|--------|-------------|-------------|---------|
| `PENDING_PAYMENT` | Temporary lock during payment | Locked (5 min) | No |
| `CONFIRMED` | Payment completed successfully | Permanently reserved | Yes |
| `CANCELLED` | User cancelled reservation | Released | No |
| `EXPIRED` | Payment timeout (auto-cancelled) | Released | No |

---

## Automatic Cleanup

A background service runs **every 60 seconds** to:

1. Find all `PENDING_PAYMENT` reservations past their `paymentExpiresAt` time
2. Change status to `EXPIRED`
3. Release the stall back to availability
4. Log cleanup actions

**Service:** `ReservationCleanupService`

---

## Business Rules

### During Reservation Creation

1. ✅ User must exist (validated via User Service)
2. ✅ User cannot have more than 3 **CONFIRMED** reservations
3. ✅ Stall must be available (not reserved)
4. ✅ Stall cannot have an active `PENDING_PAYMENT` lock
5. ✅ If pending lock exists but expired, auto-expire it first

### During Payment Completion

1. ✅ Reservation must exist
2. ✅ User must own the reservation
3. ✅ Reservation must be in `PENDING_PAYMENT` status
4. ✅ Payment must be completed within 5-minute window
5. ✅ QR code generated only after payment

---

## Race Condition Prevention

### Scenario: Two users try to book the same stall

**User A** (10:30:00):
```
1. Creates reservation → Status: PENDING_PAYMENT
2. Stall locked until 10:35:00
```

**User B** (10:31:00):
```
1. Tries to create reservation
2. ❌ ERROR: "Stall is temporarily locked for payment"
3. User B must wait until 10:35:00 or User A completes payment
```

**User A** (10:32:00):
```
1. Completes payment → Status: CONFIRMED
2. Stall permanently reserved
```

**User B** (10:35:01) - If User A didn't complete payment:
```
1. Auto-cleanup runs
2. User A's reservation → Status: EXPIRED
3. Stall released
4. User B can now create reservation
```

---

## API Testing Examples

### Test 1: Create Reservation with Lock

```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"stallId": 5}'
```

**Expected:**
- Status: 201 Created
- Response includes `paymentExpiresAt` (5 minutes from now)
- `qrCodeBase64` is null
- `status` is "PENDING_PAYMENT"

---

### Test 2: Complete Payment

```bash
curl -X POST http://localhost:8080/api/reservations/1/complete-payment \
  -H "X-User-Id: 1"
```

**Expected:**
- Status: 200 OK
- Response includes `qrCodeBase64` (generated)
- `paymentCompletedAt` populated
- `status` is "CONFIRMED"

---

### Test 3: Payment Timeout

```bash
# Create reservation
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"stallId": 5}'

# Wait 6 minutes (payment expires)

# Try to complete payment
curl -X POST http://localhost:8080/api/reservations/1/complete-payment \
  -H "X-User-Id: 1"
```

**Expected:**
- Status: 400 Bad Request
- Error: "Payment window has expired. Please create a new reservation."

---

### Test 4: Duplicate Lock Prevention

```bash
# User A creates reservation
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"stallId": 5}'

# User B tries to book same stall
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{"stallId": 5}'
```

**Expected:**
- User A: 201 Created
- User B: 400 Bad Request
- Error: "Stall ST-005 is temporarily locked for payment. Please try again later."

---

## Database Schema Changes

### New Columns in `reservations` table:

```sql
ALTER TABLE reservations 
  ADD COLUMN payment_expires_at DATETIME,
  ADD COLUMN payment_completed_at DATETIME,
  MODIFY COLUMN status ENUM('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED') 
    NOT NULL DEFAULT 'PENDING_PAYMENT';
```

---

## Configuration

**Payment Lock Duration:** 5 minutes (configurable via `PAYMENT_LOCK_MINUTES` constant)

**Cleanup Interval:** 60 seconds (every minute)

**Location:** `ReservationService.java`
```java
private static final int PAYMENT_LOCK_MINUTES = 5;
```

**Location:** `ReservationCleanupService.java`
```java
@Scheduled(fixedRate = 60000) // Every 60 seconds
```

---

## Error Handling

### New Exception Types:

1. **`PaymentExpiredException`**
   - Thrown when payment window expires
   - Status: 400 Bad Request

2. **`InvalidOperationException`**
   - Thrown when trying to complete payment on wrong status
   - Status: 400 Bad Request

3. **`UnauthorizedException`**
   - Thrown when user doesn't own the reservation
   - Status: 401 Unauthorized

---

## Benefits

✅ **Prevents Double Booking** - Stall locked during payment window

✅ **Automatic Cleanup** - Expired locks released every minute

✅ **Fair Access** - Other users can book after timeout

✅ **Better UX** - Clear payment deadline (`paymentExpiresAt`)

✅ **QR Code Security** - Only generated after confirmed payment

✅ **No Wasted Reservations** - Unpaid reservations don't count toward 3-stall limit

---

## Monitoring & Logging

The system logs key events:

```
INFO  - Creating reservation with payment lock for user: 1 and stall: 5
INFO  - Reservation created with payment lock. ID: 1, Expires at: 2025-11-09T10:35:00
INFO  - Completing payment for reservation: 1 by user: 1
INFO  - Payment completed successfully for reservation: 1
INFO  - Found 3 expired pending reservations to clean up
INFO  - Expiring reservation 2 for stall 7 (expired at 2025-11-09T10:25:00)
INFO  - Successfully expired reservation 2 and released stall 7
INFO  - Cleanup completed: 3 reservations expired
```

---

## Frontend Integration Guide

### Step 1: Create Reservation
```javascript
const response = await fetch('/api/reservations', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-User-Id': currentUser.id
  },
  body: JSON.stringify({ stallId: selectedStall.id })
});

const reservation = await response.json();

// Show payment countdown timer
const expiresAt = new Date(reservation.paymentExpiresAt);
startCountdownTimer(expiresAt);
```

### Step 2: Process Payment
```javascript
// After payment gateway confirms payment
const response = await fetch(`/api/reservations/${reservation.id}/complete-payment`, {
  method: 'POST',
  headers: {
    'X-User-Id': currentUser.id
  }
});

const confirmed = await response.json();

// Display QR code
displayQRCode(confirmed.qrCodeBase64);
```

### Step 3: Handle Timeout
```javascript
// If countdown reaches zero
if (Date.now() >= expiresAt) {
  alert('Payment time expired. Please create a new reservation.');
  redirectToStallSelection();
}
```

---

## Summary

The payment lock feature transforms the reservation system from **immediate confirmation** to a **two-phase process**:

1. **Phase 1**: Temporary lock (5 minutes) - `PENDING_PAYMENT`
2. **Phase 2**: Payment completion - `CONFIRMED` with QR code

This ensures fairness, prevents race conditions, and provides a better user experience with clear payment deadlines.
