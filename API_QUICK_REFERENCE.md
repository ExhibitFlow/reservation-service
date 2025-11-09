# Payment Lock API - Quick Reference

## 🔑 New API Endpoints

### 1. Create Reservation (With Payment Lock)

**Endpoint:** `POST /api/reservations`

**Headers:**
```
Content-Type: application/json
X-User-Id: {userId}
```

**Request Body:**
```json
{
  "stallId": 5
}
```

**Response:** `201 Created`
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
  "paymentExpiresAt": "2025-11-09T10:35:00",  ← 5 minutes from now
  "paymentCompletedAt": null,
  "qrCodeBase64": null  ← Generated after payment
}
```

**Note:** Stall is locked for 5 minutes. User must complete payment before `paymentExpiresAt`.

---

### 2. Complete Payment (NEW)

**Endpoint:** `POST /api/reservations/{reservationId}/complete-payment`

**Headers:**
```
X-User-Id: {userId}
```

**Request Body:** None

**Response:** `200 OK`
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
  "status": "CONFIRMED",  ← Changed from PENDING_PAYMENT
  "createdAt": "2025-11-09T10:30:00",
  "paymentExpiresAt": "2025-11-09T10:35:00",
  "paymentCompletedAt": "2025-11-09T10:32:15",  ← Timestamp added
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA..."  ← NOW generated
}
```

---

### 3. Get My Reservations (Unchanged)

**Endpoint:** `GET /api/reservations/my`

**Headers:**
```
X-User-Id: {userId}
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "status": "CONFIRMED",
    "paymentCompletedAt": "2025-11-09T10:32:15",
    "qrCodeBase64": "..."
  },
  {
    "id": 2,
    "status": "PENDING_PAYMENT",
    "paymentExpiresAt": "2025-11-09T10:45:00",
    "qrCodeBase64": null
  }
]
```

---

### 4. Cancel Reservation (Unchanged)

**Endpoint:** `DELETE /api/reservations/{reservationId}`

**Headers:**
```
X-User-Id: {userId}
```

**Response:** `204 No Content`

---

## 🚨 Error Responses

### Payment Expired
```json
{
  "status": 400,
  "error": "Payment Expired",
  "message": "Payment window has expired. Please create a new reservation.",
  "timestamp": 1699534320000
}
```

### Stall Temporarily Locked
```json
{
  "status": 400,
  "error": "Stall Not Available",
  "message": "Stall ST-005 is temporarily locked for payment. Please try again later.",
  "timestamp": 1699534320000
}
```

### Invalid Operation
```json
{
  "status": 400,
  "error": "Invalid Operation",
  "message": "Reservation is not pending payment",
  "timestamp": 1699534320000
}
```

### Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Not authorized to complete payment for this reservation",
  "timestamp": 1699534320000
}
```

---

## 📊 Reservation Status Values

| Status | Description | QR Code | Counts Toward Limit |
|--------|-------------|---------|---------------------|
| `PENDING_PAYMENT` | Temporary lock (5 min) | ❌ No | ❌ No |
| `CONFIRMED` | Payment completed | ✅ Yes | ✅ Yes |
| `CANCELLED` | User cancelled | ❌ No | ❌ No |
| `EXPIRED` | Payment timeout | ❌ No | ❌ No |

---

## ⏱️ Timing

- **Payment Lock Duration:** 5 minutes
- **Cleanup Interval:** Every 60 seconds
- **Auto-Expiry:** Happens automatically via background service

---

## 🧪 cURL Examples

### Create Reservation
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"stallId": 5}'
```

### Complete Payment
```bash
curl -X POST http://localhost:8080/api/reservations/1/complete-payment \
  -H "X-User-Id: 1"
```

### Get My Reservations
```bash
curl -X GET http://localhost:8080/api/reservations/my \
  -H "X-User-Id: 1"
```

### Cancel Reservation
```bash
curl -X DELETE http://localhost:8080/api/reservations/1 \
  -H "X-User-Id: 1"
```

---

## 💡 Usage Tips

1. **Always check `paymentExpiresAt`** - Display countdown timer to user
2. **Handle `PENDING_PAYMENT` status** - Show "Payment in progress" UI
3. **Call complete-payment endpoint** - After payment gateway confirmation
4. **Check QR code availability** - Only show QR code if `qrCodeBase64` is not null
5. **Handle expired payments gracefully** - Allow user to retry with new reservation

---

## 🔄 Typical User Flow

```javascript
// Step 1: Create reservation
const createResponse = await fetch('/api/reservations', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-User-Id': '1'
  },
  body: JSON.stringify({ stallId: 5 })
});

const reservation = await createResponse.json();
// reservation.status === "PENDING_PAYMENT"
// reservation.paymentExpiresAt === "2025-11-09T10:35:00"
// reservation.qrCodeBase64 === null

// Step 2: Show countdown timer
const expiresAt = new Date(reservation.paymentExpiresAt);
startCountdown(expiresAt);

// Step 3: Process payment with payment gateway
const paymentResult = await processPayment({
  amount: stallPrice,
  reservationId: reservation.id
});

// Step 4: If payment successful, complete reservation
if (paymentResult.success) {
  const completeResponse = await fetch(
    `/api/reservations/${reservation.id}/complete-payment`,
    {
      method: 'POST',
      headers: { 'X-User-Id': '1' }
    }
  );
  
  const confirmed = await completeResponse.json();
  // confirmed.status === "CONFIRMED"
  // confirmed.qrCodeBase64 === "iVBORw0KG..."
  
  // Step 5: Display QR code
  displayQRCode(confirmed.qrCodeBase64);
}
```

---

## 🎯 Quick Checklist

- [x] Create reservation → Gets `PENDING_PAYMENT` status
- [x] Check `paymentExpiresAt` → 5 minutes from creation
- [x] QR code is `null` initially
- [x] Complete payment within 5 minutes
- [x] After payment → Status changes to `CONFIRMED`
- [x] QR code generated after payment
- [x] If timeout → Auto-expires to `EXPIRED` status
- [x] Expired reservations → Stalls released automatically

---

**Quick Summary:** Create reservation → Process payment → Complete payment → Get QR code!
