# External API Quick Reference

Quick reference guide for APIs required by Reservation Service.

---

## 🔗 Service URLs

```
User Service:    http://localhost:8081
Stall Service:   http://localhost:8082
QR Code Service: http://localhost:8083  (optional)
```

---

## 📋 Required Endpoints Summary

### 1️⃣ User Service (Port 8081)

| Method | Endpoint | Purpose | Critical |
|--------|----------|---------|----------|
| GET | `/api/users/{userId}` | Get user details | ✅ Yes |

**Response:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "businessName": "Tech Corp"
}
```

---

### 2️⃣ Stall Service (Port 8082)

| Method | Endpoint | Purpose | Critical |
|--------|----------|---------|----------|
| GET | `/api/stalls/{stallId}` | Get stall details | ✅ Yes |
| PUT | `/api/stalls/{stallId}/reserve` | Mark stall as reserved | ✅ Yes |
| PUT | `/api/stalls/{stallId}/release` | Mark stall as available | ✅ Yes |

**GET Response:**
```json
{
  "id": 5,
  "stallCode": "ST-005",
  "size": "Medium",
  "price": 5000.00,
  "isReserved": false
}
```

**PUT Response (Reserve/Release):**
```json
{
  "id": 5,
  "stallCode": "ST-005",
  "size": "Medium",
  "price": 5000.00,
  "isReserved": true  // or false after release
}
```

---

### 3️⃣ QR Code Service (Port 8083) - Optional

| Method | Endpoint | Purpose | Critical |
|--------|----------|---------|----------|
| POST | `/api/qrcode/generate` | Generate QR code | ⚠️ No |

**Request:**
```json
{
  "reservationId": 1,
  "userName": "John Doe",
  "stallCode": "ST-005"
}
```

**Response:**
```json
{
  "qrCodeBase64": "iVBORw0KGgoAAAANS..."
}
```

---

## 🔄 API Call Flow

### Creating a Reservation
```
1. GET  /api/users/{userId}              → Get user info
2. GET  /api/stalls/{stallId}            → Check stall availability
3. PUT  /api/stalls/{stallId}/reserve    → Reserve the stall
4. POST /api/qrcode/generate             → Generate QR code (optional)
```

### Canceling a Reservation
```
1. PUT  /api/stalls/{stallId}/release    → Release the stall
```

---

## ⚙️ Configuration

**application.properties:**
```properties
services.user-service.url=http://localhost:8081
services.stall-service.url=http://localhost:8082
services.qrcode-service.url=http://localhost:8083
```

---

## ✅ Health Checks

```bash
# User Service
curl http://localhost:8081/actuator/health

# Stall Service  
curl http://localhost:8082/actuator/health

# QR Code Service
curl http://localhost:8083/actuator/health
```

---

## 🧪 Quick Test

```bash
# Test User Service
curl http://localhost:8081/api/users/1

# Test Stall Service
curl http://localhost:8082/api/stalls/5

# Test QR Code Service
curl -X POST http://localhost:8083/api/qrcode/generate \
  -H "Content-Type: application/json" \
  -d '{"reservationId":1,"userName":"Test","stallCode":"ST-001"}'
```

---

## 📊 Service Priority

| Priority | Service | Why |
|----------|---------|-----|
| 🔴 **High** | User Service | Cannot create reservations without user validation |
| 🔴 **High** | Stall Service | Cannot reserve stalls or check availability |
| 🟡 **Low** | QR Code Service | Reservations work without QR codes |

---

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| "Connection refused" | Check if service is running on correct port |
| "404 Not Found" | Verify endpoint URL matches specification |
| "500 Internal Error" | Check service logs for details |
| "Timeout" | Increase timeout or check service performance |

---

For complete documentation, see [EXTERNAL_API_REQUIREMENTS.md](./EXTERNAL_API_REQUIREMENTS.md)
