package exhibitflow.reservation_service.integration;

import exhibitflow.reservation_service.client.StallServiceClient;
import exhibitflow.reservation_service.client.UserServiceClient;
import exhibitflow.reservation_service.dto.CreateReservationRequest;
import exhibitflow.reservation_service.dto.StallDto;
import exhibitflow.reservation_service.dto.UserDto;
import exhibitflow.reservation_service.entity.Reservation;
import exhibitflow.reservation_service.repository.ReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ReservationController with mocked external services
 * Tests the complete REST API flow as if connected to other microservices
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReservationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void testCreateReservation_Success() throws Exception {
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

        // Make request
        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", "60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value("60ab5b68-6f41-49b7-a461-f2cf89e6c099"))
                .andExpect(jsonPath("$.userName").value("Test User"))
                .andExpect(jsonPath("$.userEmail").value("test@example.com"))
                .andExpect(jsonPath("$.businessName").value("Test Business"))
                .andExpect(jsonPath("$.stallId").value(1))
                .andExpect(jsonPath("$.stallCode").value("A-001"))
                .andExpect(jsonPath("$.stallSize").value("10x10"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.paymentExpiresAt").exists())
                .andExpect(jsonPath("$.qrCodeBase64").doesNotExist());

        // Verify external service calls
        verify(userServiceClient, times(1)).getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099");
        verify(stallServiceClient, times(1)).getStallById(1L);
        verify(stallServiceClient, times(1)).holdStall(1L);
    }

    @Test
    void testCreateReservation_MissingUserIdHeader() throws Exception {
        // Make request without X-User-Id header
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isInternalServerError()); // Returns 500 for missing required header

        // Verify no external service calls
        verify(userServiceClient, never()).getUserById(anyString());
        verify(stallServiceClient, never()).getStallById(anyLong());
    }

    @Test
    void testCreateReservation_UserNotFound() throws Exception {
        // Mock User Service to return null
        when(userServiceClient.getUserById("999")).thenReturn(null);

        // Make request
        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("User not found")));
    }

    @Test
    void testCreateReservation_StallNotAvailable() throws Exception {
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

        // Make request
        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", "60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest()) // StallNotAvailableException returns 400
                .andExpect(jsonPath("$.message").value(containsString("already reserved")));
    }

    @Test
    void testCompletePayment_Success() throws Exception {
        // Mock services for reservation creation
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);
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
        String createResponse = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", "60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract reservation ID
        Long reservationId = objectMapper.readTree(createResponse).get("id").asLong();

        // Mock reserveStall for payment completion
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
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/complete-payment")
                        .header("X-User-Id", "60ab5b68-6f41-49b7-a461-f2cf89e6c099"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentCompletedAt").exists())
                .andExpect(jsonPath("$.qrCodeBase64").exists());
    }

    @Test
    void testCompletePayment_Unauthorized() throws Exception {
        // Create reservation for user 1
        Reservation reservation = Reservation.builder()
                .userId("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .stallId(1L)
                .status(Reservation.ReservationStatus.PENDING_PAYMENT)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        reservation = reservationRepository.save(reservation);

        // Attempt to complete payment with different user
        mockMvc.perform(post("/api/v1/reservations/" + reservation.getId() + "/complete-payment")
                        .header("X-User-Id", "999"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Not authorized")));
    }

    @Test
    void testGetMyReservations_Success() throws Exception {
        // Mock User Service
        when(userServiceClient.getUserById("60ab5b68-6f41-49b7-a461-f2cf89e6c099")).thenReturn(testUser);

        // Create reservations
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

        // Get reservations
        mockMvc.perform(get("/api/v1/reservations/my")
                        .header("X-User-Id", "60ab5b68-6f41-49b7-a461-f2cf89e6c099"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].stallCode", containsInAnyOrder("A-001", "A-002")))
                .andExpect(jsonPath("$[*].userName", everyItem(is("Test User"))));
    }

    @Test
    void testGetAllReservations_Success() throws Exception {
        // Mock User Service responses
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

        // Create reservations
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
        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].userName", containsInAnyOrder("User One", "User Two")))
                .andExpect(jsonPath("$.content[*].stallCode", containsInAnyOrder("A-001", "A-002")))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void testCancelReservation_Success() throws Exception {
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
        String createResponse = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", "60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract reservation ID
        Long reservationId = objectMapper.readTree(createResponse).get("id").asLong();

        // Mock release stall
        when(stallServiceClient.releaseStall(1L)).thenReturn(testStall);

        // Cancel reservation
        mockMvc.perform(delete("/api/v1/reservations/" + reservationId)
                        .header("X-User-Id", "60ab5b68-6f41-49b7-a461-f2cf89e6c099"))
                .andExpect(status().isNoContent());

        // Verify stall was released
        verify(stallServiceClient, times(1)).releaseStall(1L);
    }

    @Test
    void testCancelReservation_Unauthorized() throws Exception {
        // Create reservation for user 1
        Reservation reservation = Reservation.builder()
                .userId("60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                .stallId(1L)
                .status(Reservation.ReservationStatus.CONFIRMED)
                .paymentCompletedAt(LocalDateTime.now())
                .build();
        reservation = reservationRepository.save(reservation);

        // Attempt to cancel with different user
        mockMvc.perform(delete("/api/v1/reservations/" + reservation.getId())
                        .header("X-User-Id", "999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("Unauthorized")));

        // Verify stall was not released
        verify(stallServiceClient, never()).releaseStall(anyLong());
    }

    @Test
    void testCreateReservation_InvalidRequest() throws Exception {
        // Create invalid request (missing stallId)
        CreateReservationRequest invalidRequest = new CreateReservationRequest();

        // Make request
        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", "60ab5b68-6f41-49b7-a461-f2cf89e6c099")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Verify no external service calls
        verify(userServiceClient, never()).getUserById(anyString());
        verify(stallServiceClient, never()).getStallById(anyLong());
    }
}
