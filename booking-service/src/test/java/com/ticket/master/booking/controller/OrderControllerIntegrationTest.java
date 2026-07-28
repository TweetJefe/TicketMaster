package com.ticket.master.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.booking.TestcontainersConfiguration;
import com.ticket.master.booking.dto.OrderCreateRequest;
import com.ticket.master.booking.model.Order;
import com.ticket.master.booking.model.OrderStatus;
import com.ticket.master.booking.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Nested
    class createOrderTests {
        @Test
        @WithMockUser(roles = "CUSTOMER")
        void createOrderSuccess() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            OrderCreateRequest request = new OrderCreateRequest(
                    userId,
                    eventId,
                    BigDecimal.TEN,
                    List.of(ticketId)
            );

            mockMvc.perform(post("/api/bookings/create")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "CUSTOMER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        void createOrderUnauthorized() throws Exception {
            OrderCreateRequest request = new OrderCreateRequest(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    BigDecimal.TEN,
                    List.of(UUID.randomUUID())
            );

            mockMvc.perform(post("/api/bookings/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class payOrderTests {
        @Test
        @WithMockUser(roles = "CUSTOMER")
        void payOrderSuccess() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            Order order = Order.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .totalAmount(BigDecimal.TEN)
                    .ticketIds(List.of(ticketId))
                    .status(OrderStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            order = orderRepository.save(order);

            mockMvc.perform(patch("/api/bookings/{id}/pay", order.getId())
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "CUSTOMER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.getId().toString()))
                    .andExpect(jsonPath("$.status").value("PAID"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void payOrderNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(patch("/api/bookings/{id}/pay", nonExistentId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "CUSTOMER"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void payOrderUnauthorized() throws Exception {
            mockMvc.perform(patch("/api/bookings/{id}/pay", UUID.randomUUID()))
                    .andExpect(status().isForbidden());
        }
    }
}
