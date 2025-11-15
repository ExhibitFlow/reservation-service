# Integration Testing Documentation

## Overview
This document describes the comprehensive integration tests created for the Reservation Service that simulate interactions with external microservices (User Service and Stall Service).

## Test Architecture

### Mock Configuration
- **TestConfig.java**: Provides mock beans for `UserServiceClient` and `StallServiceClient`
- Uses Mockito to simulate responses from external services
- Tests run with an in-memory H2 database instead of PostgreSQL

### Test Suites

#### 1. ReservationServiceIntegrationTest
**Purpose**: Tests business logic layer with mocked external services

**Test Cases**:

1. **testCreateReservation_Success**
   - Simulates successful reservation creation
   - Mocks: User exists, Stall is available
   - Verifies: PENDING_PAYMENT status, payment expiration time, no QR code yet
   - Validates: External service calls are made correctly

2. **testCreateReservation_UserNotFound**
   - Simulates user not found scenario
   - Mocks: UserService returns null
   - Verifies: ResourceNotFoundException is thrown
   - Validates: No stall service calls are made

3. **testCreateReservation_StallNotFound**
   - Simulates stall not found scenario
   - Mocks: StallService returns null
   - Verifies: ResourceNotFoundException is thrown
   - Validates: Stall is not reserved

4. **testCreateReservation_StallAlreadyReserved**
   - Simulates stall already reserved scenario
   - Mocks: StallService returns reserved stall
   - Verifies: StallNotAvailableException is thrown
   - Validates: Reservation is not created

5. **testCreateReservation_MaxReservationsExceeded**
   - Tests the 3-reservation limit per user
   - Setup: Creates 3 confirmed reservations
   - Verifies: ReservationLimitExceededException is thrown
   - Validates: 4th reservation is rejected

6. **testCompletePayment_Success**
   - Tests successful payment completion flow
   - Simulates: Creating reservation then completing payment
   - Verifies: Status changes to CONFIRMED, QR code is generated
   - Validates: Payment completion timestamp is set

7. **testCompletePayment_Unauthorized**
   - Tests payment completion by different user
   - Simulates: User 2 trying to complete User 1's payment
   - Verifies: UnauthorizedException is thrown
   - Validates: Reservation status unchanged

8. **testCompletePayment_AlreadyConfirmed**
   - Tests duplicate payment completion
   - Setup: Reservation already CONFIRMED
   - Verifies: InvalidOperationException is thrown

9. **testGetUserReservations_Success**
   - Tests retrieving all user reservations
   - Setup: Multiple reservations for one user
   - Mocks: User and Stall service responses for each reservation
   - Verifies: All reservations returned with complete data

10. **testCancelReservation_Success**
    - Tests successful reservation cancellation
    - Simulates: Complete flow from creation to cancellation
    - Verifies: Status changes to CANCELLED, stall is released
    - Validates: releaseStall is called on StallService

11. **testCancelReservation_Unauthorized**
    - Tests cancellation by different user
    - Verifies: Exception is thrown, stall not released

12. **testGetAllReservations_Success**
    - Tests admin function to get all reservations
    - Setup: Multiple users with multiple reservations
    - Mocks: Different users and stalls
    - Verifies: All reservations returned with correct user/stall data

13. **testPaymentExpiration_AutoExpire**
    - Tests automatic expiration of payment window
    - Setup: Reservation with expired payment window
    - Verifies: PaymentExpiredException thrown, status changes to EXPIRED
    - Validates: Stall is automatically released

#### 2. ReservationControllerIntegrationTest
**Purpose**: Tests REST API endpoints with mocked external services

**Test Cases**:

1. **testCreateReservation_Success**
   - Tests POST /api/reservations endpoint
   - Validates: HTTP 201 Created, response contains all expected fields
   - Verifies: External services called correctly

2. **testCreateReservation_MissingUserIdHeader**
   - Tests missing X-User-Id header
   - Validates: HTTP 400 Bad Request
   - Verifies: No external service calls made

3. **testCreateReservation_UserNotFound**
   - Tests user not found via REST API
   - Validates: HTTP 404 Not Found, error message

4. **testCreateReservation_StallNotAvailable**
   - Tests stall already reserved via REST API
   - Validates: HTTP 409 Conflict, error message

5. **testCompletePayment_Success**
   - Tests POST /api/reservations/{id}/complete-payment
   - Validates: HTTP 200 OK, QR code in response
   - Verifies: Status changed to CONFIRMED

6. **testCompletePayment_Unauthorized**
   - Tests unauthorized payment completion
   - Validates: HTTP 401 Unauthorized, error message

7. **testGetMyReservations_Success**
   - Tests GET /api/reservations/my
   - Validates: HTTP 200 OK, array of reservations
   - Verifies: All user reservations returned

8. **testGetAllReservations_Success**
   - Tests GET /api/reservations
   - Validates: HTTP 200 OK, all reservations returned
   - Verifies: Multiple users' reservations included

9. **testCancelReservation_Success**
   - Tests DELETE /api/reservations/{id}
   - Validates: HTTP 204 No Content
   - Verifies: releaseStall called

10. **testCancelReservation_Unauthorized**
    - Tests unauthorized cancellation
    - Validates: HTTP 500 Internal Server Error, error message

11. **testCreateReservation_InvalidRequest**
    - Tests validation of request body
    - Validates: HTTP 400 Bad Request
    - Verifies: No external service calls

## Mock Data

### UserDto Mock
```json
{
  "id": 1,
  "email": "test@example.com",
  "name": "Test User",
  "businessName": "Test Business"
}
```

### StallDto Mock (Available)
```json
{
  "id": 1,
  "stallCode": "A-001",
  "size": "10x10",
  "price": 500.00,
  "isReserved": false
}
```

### StallDto Mock (Reserved)
```json
{
  "id": 1,
  "stallCode": "A-001",
  "size": "10x10",
  "price": 500.00,
  "isReserved": true
}
```

## Running the Tests

### Run All Tests
```bash
./mvnw test
```

### Run Specific Test Suite
```bash
# Service layer tests
./mvnw test -Dtest=ReservationServiceIntegrationTest

# Controller tests
./mvnw test -Dtest=ReservationControllerIntegrationTest
```

### Run Specific Test Case
```bash
./mvnw test -Dtest=ReservationServiceIntegrationTest#testCreateReservation_Success
```

## Test Coverage

### Business Scenarios Covered
- ✅ Successful reservation creation with payment lock
- ✅ User validation via User Service
- ✅ Stall availability check via Stall Service
- ✅ Stall reservation via Stall Service
- ✅ Payment completion with QR code generation
- ✅ Payment window expiration (5 minutes)
- ✅ Maximum reservation limit (3 per user)
- ✅ Reservation cancellation with stall release
- ✅ User authorization checks
- ✅ Retrieving user-specific reservations
- ✅ Admin retrieval of all reservations
- ✅ Error handling for missing resources
- ✅ Conflict handling for unavailable stalls

### External Service Interactions Tested
- ✅ UserServiceClient.getUserById()
- ✅ StallServiceClient.getStallById()
- ✅ StallServiceClient.reserveStall()
- ✅ StallServiceClient.releaseStall()

### Edge Cases Covered
- User not found
- Stall not found
- Stall already reserved
- Unauthorized access attempts
- Payment window expiration
- Maximum reservation limit
- Invalid request payloads
- Missing required headers

## Configuration

### Test Database
- Uses H2 in-memory database
- Database recreated for each test
- No PostgreSQL connection required

### Application Properties
Location: `src/test/resources/application-test.properties`

Key settings:
- H2 database configuration
- Hibernate auto-DDL set to create-drop
- Debug logging enabled
- Mock service URLs configured

## Mock Verification

Tests verify that external services are called:
- **Correct number of times**
- **With correct parameters**
- **In correct order**
- **Only when expected**

Example:
```java
verify(userServiceClient, times(1)).getUserById(1L);
verify(stallServiceClient, times(1)).reserveStall(1L);
verify(stallServiceClient, never()).releaseStall(anyLong());
```

## Best Practices Demonstrated

1. **Isolation**: Each test is independent and isolated
2. **Mock Reset**: Mocks reset before each test
3. **Database Cleanup**: Database cleared before each test
4. **Comprehensive Coverage**: Tests both happy and error paths
5. **External Call Verification**: Confirms correct microservice interactions
6. **Transaction Management**: Tests run in transactions that rollback
7. **Clear Assertions**: Multiple specific assertions per test
8. **Realistic Scenarios**: Simulates actual microservice communication

## Benefits of This Testing Approach

1. **No External Dependencies**: Tests run without User/Stall services running
2. **Fast Execution**: In-memory database, no network calls
3. **Deterministic**: Controlled mock responses ensure consistent results
4. **Comprehensive**: Tests complete integration from controller to database
5. **Debuggable**: Easy to debug with controlled inputs/outputs
6. **CI/CD Ready**: Can run in any environment without external services

## Future Enhancements

Potential additions:
- Performance tests for concurrent reservations
- Chaos engineering tests for service failures
- Contract testing with Pact
- Load testing with JMeter
- Security testing for authentication/authorization
