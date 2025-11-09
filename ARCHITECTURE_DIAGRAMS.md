# Payment Lock System - Visual Architecture

## System Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                         USER INITIATES BOOKING                       │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
                ┌────────────────────────────┐
                │  POST /api/reservations    │
                │  Body: {stallId: 5}        │
                └────────────┬───────────────┘
                             │
                             ▼
         ┌───────────────────────────────────────────┐
         │    ReservationService.createReservation() │
         └───────────────────┬───────────────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  1. Validate User (User API) │
              │  2. Check 3-stall limit       │
              │  3. Get Stall (Stall API)     │
              │  4. Check availability        │
              │  5. Check pending locks       │
              └──────────────┬────────────────┘
                             │
                             ▼
                    ┌────────────────┐
                    │  Stall.reserve │  ← Temporary Lock
                    └────────┬───────┘
                             │
                             ▼
         ┌───────────────────────────────────────────┐
         │  CREATE RESERVATION                       │
         │  - Status: PENDING_PAYMENT                │
         │  - paymentExpiresAt: NOW + 5 min          │
         │  - qrCodeBase64: NULL                     │
         └───────────────────┬───────────────────────┘
                             │
                             ▼
         ┌───────────────────────────────────────────┐
         │  RESPONSE TO USER                         │
         │  {                                        │
         │    id: 1,                                 │
         │    status: "PENDING_PAYMENT",             │
         │    paymentExpiresAt: "10:35:00",          │
         │    qrCodeBase64: null                     │
         │  }                                        │
         └───────────────────┬───────────────────────┘
                             │
                             │
        ┌────────────────────┴─────────────────────┐
        │                                          │
        ▼                                          ▼
┌───────────────────┐                  ┌──────────────────────┐
│ USER COMPLETES    │                  │  5 MINUTES PASS      │
│ PAYMENT           │                  │  (No payment)        │
└────────┬──────────┘                  └──────────┬───────────┘
         │                                        │
         ▼                                        ▼
┌─────────────────────────┐         ┌──────────────────────────┐
│ POST /api/reservations/ │         │ ReservationCleanupService│
│      1/complete-payment │         │ (runs every 60 seconds)  │
└────────┬────────────────┘         └──────────┬───────────────┘
         │                                      │
         ▼                                      ▼
┌──────────────────────┐            ┌─────────────────────────┐
│ completePayment()    │            │ Find expired locks      │
│                      │            │ WHERE status =          │
│ 1. Verify ownership  │            │   PENDING_PAYMENT       │
│ 2. Check not expired │            │ AND paymentExpiresAt    │
│ 3. Generate QR code  │            │   < NOW                 │
│ 4. Set CONFIRMED     │            └──────────┬──────────────┘
└────────┬─────────────┘                       │
         │                                     ▼
         ▼                          ┌──────────────────────────┐
┌──────────────────────┐            │ For each expired:        │
│ UPDATE RESERVATION   │            │ 1. Set status = EXPIRED  │
│                      │            │ 2. Release stall         │
│ - status: CONFIRMED  │            └──────────────────────────┘
│ - paymentCompletedAt │
│ - qrCodeBase64: "..." │
└────────┬─────────────┘
         │
         ▼
┌──────────────────────────────┐
│ RESPONSE TO USER             │
│ {                            │
│   id: 1,                     │
│   status: "CONFIRMED",       │
│   qrCodeBase64: "iVBORw...", │
│   paymentCompletedAt: "..."  │
│ }                            │
└──────────────────────────────┘
```

---

## Status State Machine

```
                    ┌─────────────────┐
                    │ PENDING_PAYMENT │ ← Created (5-min lock)
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
      ┌──────────┐   ┌──────────┐   ┌──────────┐
      │CONFIRMED │   │CANCELLED │   │ EXPIRED  │
      └──────────┘   └──────────┘   └──────────┘
       (Payment)     (User cancel)   (Timeout)
```

---

## Timeline Example

```
Time      Event                           Status            Stall State
────────  ─────────────────────────────  ────────────────  ─────────────
10:30:00  User creates reservation       PENDING_PAYMENT   Locked
          paymentExpiresAt: 10:35:00
          
10:31:00  Another user tries to book     -                 Locked ❌
          → Error: "Temporarily locked"
          
10:32:00  User completes payment         CONFIRMED         Reserved ✓
          QR code generated
          
10:35:00  [If no payment]                EXPIRED          Released
          Cleanup service auto-expires
```

---

## Component Interaction

```
┌─────────────────┐
│ ReservationCtrl │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────┐
│ReservationSvc   │─────▶│ User Service │ (Validate user)
│                 │      └──────────────┘
│  - create()     │      
│  - complete()   │      ┌──────────────┐
│                 │─────▶│ Stall Service│ (Lock/unlock)
└────────┬────────┘      └──────────────┘
         │               
         │               ┌──────────────┐
         └──────────────▶│ QR Code Svc  │ (Generate QR)
                         └──────────────┘

┌────────────────────┐
│ CleanupService     │
│ @Scheduled(60s)    │
└────────┬───────────┘
         │
         ▼
┌────────────────────┐
│ ReservationRepo    │
│ Find expired locks │
└────────┬───────────┘
         │
         ▼
┌────────────────────┐
│ Stall Service      │
│ Release stalls     │
└────────────────────┘
```

---

## Database State Transitions

```sql
-- Initial creation
INSERT INTO reservations (
  user_id, 
  stall_id, 
  status, 
  payment_expires_at
) VALUES (
  1, 
  5, 
  'PENDING_PAYMENT', 
  '2025-11-09 10:35:00'
);

-- After payment completion
UPDATE reservations 
SET status = 'CONFIRMED',
    payment_completed_at = '2025-11-09 10:32:15',
    qr_code_base64 = 'iVBORw0KGg...'
WHERE id = 1;

-- OR if expired (cleanup service)
UPDATE reservations 
SET status = 'EXPIRED'
WHERE id = 1 
  AND status = 'PENDING_PAYMENT' 
  AND payment_expires_at < NOW();
```

---

## Error Flow Chart

```
User creates reservation
         │
         ▼
    Stall locked?
         │
    Yes──┼──No
    │         │
    ▼         ▼
Pending?   Reserve
    │         │
Yes─┼─No     ▼
    │   │    Create PENDING_PAYMENT
    ▼   ▼    │
 Expired? Confirmed   ▼
    │      │        Response
Yes─┼─No   │
    │   │  │
    ▼   ▼  ▼
Auto-   Error:    User has
expire  "Already  5 minutes
then    reserved" to pay
allow              │
                   ▼
              Complete payment?
                   │
              Yes──┼──No
                   │   │
                   ▼   ▼
              Generate  Timeout
              QR code   auto-expire
                   │
                   ▼
              CONFIRMED
```

---

## Concurrency Control

```
Thread 1 (User A)              Thread 2 (User B)
─────────────────────────────  ─────────────────────────────
Check stall available ✓
                               Check stall available ✓
Reserve stall
                               Reserve stall ❌
                               (Already locked by Thread 1)
Create PENDING_PAYMENT
                               Error: "Temporarily locked"
Response to User A
```

---

## Cleanup Service Operation

```
Every 60 seconds:

SELECT * FROM reservations
WHERE status = 'PENDING_PAYMENT'
  AND payment_expires_at < NOW()

For each expired reservation:
  1. reservation.status = 'EXPIRED'
  2. reservationRepo.save(reservation)
  3. stallService.releaseStall(stallId)
  4. Log: "Expired reservation {id} for stall {stallId}"
```

---

## API Sequence Diagram

```
Client          ReservationCtrl    ReservationSvc    UserSvc   StallSvc   QRSvc
  │                    │                  │            │         │         │
  │ POST /reservations │                  │            │         │         │
  │───────────────────▶│                  │            │         │         │
  │                    │ createReservation│            │         │         │
  │                    │─────────────────▶│            │         │         │
  │                    │                  │ getUserById│         │         │
  │                    │                  │───────────▶│         │         │
  │                    │                  │◀───────────│         │         │
  │                    │                  │ getStallById         │         │
  │                    │                  │─────────────────────▶│         │
  │                    │                  │◀─────────────────────│         │
  │                    │                  │ reserveStall         │         │
  │                    │                  │─────────────────────▶│         │
  │                    │                  │◀─────────────────────│         │
  │                    │                  │ Save to DB           │         │
  │                    │                  │ (PENDING_PAYMENT)    │         │
  │                    │◀─────────────────│                      │         │
  │◀───────────────────│                  │                      │         │
  │ 201 Created        │                  │                      │         │
  │ (No QR code yet)   │                  │                      │         │
  │                    │                  │                      │         │
  │ POST /complete-payment                │                      │         │
  │───────────────────▶│                  │                      │         │
  │                    │ completePayment  │                      │         │
  │                    │─────────────────▶│                      │         │
  │                    │                  │ Check expiry         │         │
  │                    │                  │ Update to CONFIRMED  │         │
  │                    │                  │ generateQRCode       │         │
  │                    │                  │─────────────────────────────▶ │
  │                    │                  │◀───────────────────────────── │
  │                    │                  │ Save QR to DB        │         │
  │                    │◀─────────────────│                      │         │
  │◀───────────────────│                  │                      │         │
  │ 200 OK             │                  │                      │         │
  │ (With QR code)     │                  │                      │         │
```

This visual documentation provides a clear understanding of how the payment lock system works at every level!
