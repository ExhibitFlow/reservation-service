package exhibitflow.reservation_service.integration;

import exhibitflow.reservation_service.client.StallServiceClient;
import exhibitflow.reservation_service.client.UserServiceClient;
import exhibitflow.reservation_service.dto.CreateReservationRequest;
import exhibitflow.reservation_service.dto.ReservationResponse;
import exhibitflow.reservation_service.dto.StallDto;
import exhibitflow.reservation_service.dto.UserDto;
import exhibitflow.reservation_service.entity.Reservation;
import exhibitflow.reservation_service.exception.*;
import exhibitflow.reservation_service.repository.ReservationRepository;
import exhibitflow.reservation_service.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for ReservationService with mocked external services
 * Tests the complete flow as if connected to User and Stall microservices
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private StallServiceClient stallServiceClient;

    private UserDto testUser;
    private StallDto testStall;
    private CreateReservationRequest testRequest;

    @BeforeEach
    void setUp() {
        // Clear all previous mocks
        reset(userServiceClient, stallServiceClient);
        
        // Clean database
        reservationRepository.deleteAll();

        // Setup test data
        testUser = UserDto.builder()
                .id("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .email("test@example.com")
                .name("Test User")
                .businessName("Test Business")
                .build();

        testStall = StallDto.builder()
                .id(1L)
                .code("A-001")
                .size("10x10")
                .price(500.00)
                .isReserved(false)
                .build();

        testRequest = new CreateReservationRequest();
        testRequest.setStallId(1L);
    }

    @Test
    void testCreateReservation_Success() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Mock Stall Service responses
        when(stallServiceClient.getStallById(1L)).thenReturn(testStall);
        when(stallServiceClient.reserveStall(1L)).thenReturn(
                StallDto.builder()
                        .id(1L)
                        .code("A-001")
                        .size("10x10")
                        .price(500.00)
                        .status("HELD")
                        .isReserved(false)
                        .build()
        );

        // Create reservation
        ReservationResponse response = reservationService.createReservation(testRequest, "60ab5b68-6f41-49b7-a461-f2cf89e6c099");

        // Verify response
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getUserId()).isEqualTo("60ab5b68-6f41-49b7-a461-f2cf89e6c099");
        assertThat(response.getUserName()).isEqualTo("Test User");
        assertThat(response.getStallId()).isEqualTo(1L);
        assertThat(response.getStallCode()).isEqualTo("A-001");
        assertThat(response.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(response.getPaymentExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(response.getQrCodeBase64()).isNull(); // QR code not generated until payment

        // Verify external service calls
        verify(userServiceClient, times(1)).getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099");
        verify(stallServiceClient, times(1)).getStallById(1L);
        verify(stallServiceClient, times(1)).holdStall(1L);

        // Verify database
        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getStatus()).isEqualTo(Reservation.ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    void testCreateReservation_UserNotFound() {
        // Mock User Service to return null
        when(userServiceClient.getUserById("999")).thenReturn(null);

        // Attempt to create reservation
        assertThatThrownBy(() -> reservationService.createReservation(testRequest, "999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify no stall service calls were made
        verify(stallServiceClient, never()).getStallById(anyLong());
        verify(stallServiceClient, never()).holdStall(anyLong());
    }

    @Test
    void testCreateReservation_StallNotFound() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Mock Stall Service to return null
        when(stallServiceClient.getStallById(999L)).thenReturn(null);

        testRequest.setStallId(999L);

        // Attempt to create reservation
        assertThatThrownBy(() -> reservationService.createReservation(testRequest, "60ab5b68-6f41-49b7-a461-f2cf89e6c099"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stall not found");

        // Verify no stall was held
        verify(stallServiceClient, never()).holdStall(anyLong());
    }

    @Test
    void testCreateReservation_StallAlreadyReserved() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Mock Stall Service to return already reserved stall
        StallDto reservedStall = StallDto.builder()
                .id(1L)
                .code("A-001")
                .size("10x10")
                .price(500.00)
                .isReserved(true)
                .build();
        when(stallServiceClient.getStallById(1L)).thenReturn(reservedStall);

        // Attempt to create reservation
        assertThatThrownBy(() -> reservationService.createReservation(testRequest, "60ab5b68-6f41-49b7-a461-f2cf89e6c099"))
                .isInstanceOf(StallNotAvailableException.class)
                .hasMessageContaining("already reserved");

        // Verify stall was not held
        verify(stallServiceClient, never()).holdStall(anyLong());
    }

    @Test
    void testCreateReservation_MaxReservationsExceeded() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Create 3 existing confirmed reservations for the user
        for (int i = 1; i <= 3; i++) {
            Reservation existing = Reservation.builder()
                    .userId("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                    .stallId((long) i)
                    .status(Reservation.ReservationStatus.CONFIRMED)
                    .paymentCompletedAt(LocalDateTime.now())
                    .build();
            reservationRepository.save(existing);
        }

        // Mock new stall as available
        StallDto newStall = StallDto.builder()
                .id(4L)
                .code("A-004")
                .size("10x10")
                .price(500.00)
                .isReserved(false)
                .build();
        when(stallServiceClient.getStallById(4L)).thenReturn(newStall);

        testRequest.setStallId(4L);

        // Attempt to create 4th reservation
        assertThatThrownBy(() -> reservationService.createReservation(testRequest, "60ab5b68-6f41-49b7-a461-f2cf89e6c099"))
                .isInstanceOf(ReservationLimitExceededException.class)
                .hasMessageContaining("Maximum reservation limit");

        // Verify no stall service calls for hold
        verify(stallServiceClient, never()).holdStall(anyLong());
    }

    @Test
    void testCompletePayment_Success() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Mock Stall Service responses
        when(stallServiceClient.getStallById(1L)).thenReturn(testStall);
        when(stallServiceClient.holdStall(1L)).thenReturn(
                StallDto.builder()
                        .id(1L)
                        .code("A-001")
                        .size("10x10")
                        .price(500.00)
                        .status("HELD")
                        .isReserved(false)
                        .build()
        );

        // Create reservation
        ReservationResponse created = reservationService.createReservation(testRequest, "60ab5b68-6f41-49b7-a461-f2cf89e6c099");
        
        // Clear mock interactions
        clearInvocations(userServiceClient, stallServiceClient);
        
        // Mock services for payment completion
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);
        when(stallServiceClient.getStallById(1L)).thenReturn(testStall);
        when(stallServiceClient.reserveStall(1L)).thenReturn(
                StallDto.builder()
                        .id(1L)
                        .code("A-001")
                        .size("10x10")
                        .price(500.00)
                        .status("RESERVED")
                        .isReserved(true)
                        .build()
        );

        // Complete payment
        ReservationResponse completed = reservationService.completePayment(created.getId(), "60ab5b68-6f41-49b7-a461-f2cf89e6c099");

        // Verify response
        assertThat(completed).isNotNull();
        assertThat(completed.getId()).isEqualTo(created.getId());
        assertThat(completed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(completed.getPaymentCompletedAt()).isNotNull();
        assertThat(completed.getQrCodeBase64()).isNotNull(); // QR code should be generated

        // Verify database
        Reservation reservation = reservationRepository.findById(created.getId()).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
        assertThat(reservation.getQrCodeBase64()).isNotNull();
    }

    @Test
    void testCompletePayment_Unauthorized() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Mock Stall Service responses
        when(stallServiceClient.getStallById(1L)).thenReturn(testStall);
        when(stallServiceClient.holdStall(1L)).thenReturn(
                StallDto.builder()
                        .id(1L)
                        .code("A-001")
                        .size("10x10")
                        .price(500.00)
                        .status("HELD")
                        .isReserved(false)
                        .build()
        );

        // Create reservation
        ReservationResponse created = reservationService.createReservation(testRequest, "60ab5b68-6f41-49b7-a461-f2cf89e6c099");

        // Attempt to complete payment with different user
        assertThatThrownBy(() -> reservationService.completePayment(created.getId(), "999"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    void testCompletePayment_AlreadyConfirmed() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Create already confirmed reservation
        Reservation reservation = Reservation.builder()
                .userId("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .stallId(1L)
                .status(Reservation.ReservationStatus.CONFIRMED)
                .paymentCompletedAt(LocalDateTime.now())
                .qrCodeBase64("existing-qr-code")
                .build();
        reservation = reservationRepository.save(reservation);

        // Attempt to complete payment again
        Long reservationId = reservation.getId();
        assertThatThrownBy(() -> reservationService.completePayment(reservationId, "60ab5b68-6f41-49b7-a461-f2cf89e6c099"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not pending payment");
    }

    @Test
    void testGetUserReservations_Success() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Create multiple reservations for user
        Reservation res1 = Reservation.builder()
                .userId("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .stallId(1L)
                .status(Reservation.ReservationStatus.CONFIRMED)
                .paymentCompletedAt(LocalDateTime.now())
                .build();
        
        Reservation res2 = Reservation.builder()
                .userId("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .stallId(2L)
                .status(Reservation.ReservationStatus.PENDING_PAYMENT)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        
        reservationRepository.save(res1);
        reservationRepository.save(res2);

        // Mock stall service responses
        StallDto stall1 = StallDto.builder()
                .id(1L)
                .code("A-001")
                .size("10x10")
                .price(500.00)
                .isReserved(true)
                .build();
        
        StallDto stall2 = StallDto.builder()
                .id(2L)
                .code("A-002")
                .size("15x15")
                .price(750.00)
                .isReserved(true)
                .build();

        when(stallServiceClient.getStallById(1L)).thenReturn(stall1);
        when(stallServiceClient.getStallById(2L)).thenReturn(stall2);

        // Get user reservations
        List<ReservationResponse> reservations = reservationService.getUserReservations("60ab5b68-6f41-49b7-a461-f2cf89e6c099");

        // Verify
        assertThat(reservations).hasSize(2);
        assertThat(reservations).extracting(ReservationResponse::getStallCode)
                .containsExactlyInAnyOrder("A-001", "A-002");
        
        // Verify service calls
        verify(userServiceClient, atLeastOnce()).getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099");
        verify(stallServiceClient, times(1)).getStallById(1L);
        verify(stallServiceClient, times(1)).getStallById(2L);
    }

    @Test
    void testCancelReservation_Success() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Mock Stall Service responses
        when(stallServiceClient.getStallById(1L)).thenReturn(testStall);
        when(stallServiceClient.holdStall(1L)).thenReturn(
                StallDto.builder()
                        .id(1L)
                        .code("A-001")
                        .size("10x10")
                        .price(500.00)
                        .status("HELD")
                        .isReserved(false)
                        .build()
        );

        // Create reservation
        ReservationResponse created = reservationService.createReservation(testRequest, "60ab5b68-6f41-49b7-a461-f2cf89e6c099");
        
        // Mock release stall
        when(stallServiceClient.releaseStall(1L)).thenReturn(testStall);

        // Cancel reservation
        reservationService.cancelReservation(created.getId(), "60ab5b68-6f41-49b7-a461-f2cf89e6c099");

        // Verify database
        Reservation reservation = reservationRepository.findById(created.getId()).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.CANCELLED);

        // Verify stall was released
        verify(stallServiceClient, times(1)).releaseStall(1L);
    }

    @Test
    void testCancelReservation_Unauthorized() {
        // Create reservation for user 1
        Reservation reservation = Reservation.builder()
                .userId("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .stallId(1L)
                .status(Reservation.ReservationStatus.CONFIRMED)
                .paymentCompletedAt(LocalDateTime.now())
                .build();
        reservation = reservationRepository.save(reservation);

        // Attempt to cancel with different user
        Long reservationId = reservation.getId();
        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId, "999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");

        // Verify stall was not released
        verify(stallServiceClient, never()).releaseStall(anyLong());
    }

    @Test
    void testGetAllReservations_Success() {
        // Mock User Service responses for different users
        UserDto user1 = UserDto.builder()
                .id("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .email("user1@example.com")
                .name("User One")
                .businessName("Business One")
                .build();
        
        UserDto user2 = UserDto.builder()
                .id("70ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .email("user2@example.com")
                .name("User Two")
                .businessName("Business Two")
                .build();

        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(user1);
        when(userServiceClient.getUserById("70ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(user2);

        // Mock Stall Service responses
        StallDto stall1 = StallDto.builder()
                .id(1L)
                .code("A-001")
                .size("10x10")
                .price(500.00)
                .isReserved(true)
                .build();
        
        StallDto stall2 = StallDto.builder()
                .id(2L)
                .code("A-002")
                .size("15x15")
                .price(750.00)
                .isReserved(true)
                .build();

        when(stallServiceClient.getStallById(1L)).thenReturn(stall1);
        when(stallServiceClient.getStallById(2L)).thenReturn(stall2);

        // Create reservations for different users
        Reservation res1 = Reservation.builder()
                .userId("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .stallId(1L)
                .status(Reservation.ReservationStatus.CONFIRMED)
                .paymentCompletedAt(LocalDateTime.now())
                .build();
        
        Reservation res2 = Reservation.builder()
                .userId("70ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .stallId(2L)
                .status(Reservation.ReservationStatus.PENDING_PAYMENT)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        
        reservationRepository.save(res1);
        reservationRepository.save(res2);

        // Get all reservations
        List<ReservationResponse> reservations = reservationService.getAllReservations();

        // Verify
        assertThat(reservations).hasSize(2);
        assertThat(reservations).extracting(ReservationResponse::getUserName)
                .containsExactlyInAnyOrder("User One", "User Two");
        assertThat(reservations).extracting(ReservationResponse::getStallCode)
                .containsExactlyInAnyOrder("A-001", "A-002");
    }

    @Test
    void testPaymentExpiration_AutoExpire() {
        // Mock User Service response
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Mock Stall Service responses
        when(stallServiceClient.getStallById(1L)).thenReturn(testStall);
        
        // Create expired reservation manually
        Reservation expiredReservation = Reservation.builder()
                .userId("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .stallId(1L)
                .status(Reservation.ReservationStatus.PENDING_PAYMENT)
                .paymentExpiresAt(LocalDateTime.now().minusMinutes(1)) // Already expired
                .build();
        expiredReservation = reservationRepository.save(expiredReservation);

        // Mock release stall
        when(stallServiceClient.releaseStall(1L)).thenReturn(testStall);

        // Attempt to complete payment after expiration
        Long reservationId = expiredReservation.getId();
        assertThatThrownBy(() -> reservationService.completePayment(reservationId, "60ab5b68-6f41-49b7-a461-f2cf89e6c099"))
                .isInstanceOf(PaymentExpiredException.class)
                .hasMessageContaining("Payment window has expired");

        // Verify reservation status changed to EXPIRED
        Reservation updated = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Reservation.ReservationStatus.EXPIRED);

        // Verify stall was released
        verify(stallServiceClient, times(1)).releaseStall(1L);
    }
}
