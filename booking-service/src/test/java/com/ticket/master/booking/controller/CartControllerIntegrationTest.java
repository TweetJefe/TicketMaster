package com.ticket.master.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.booking.TestcontainersConfiguration;
import com.ticket.master.booking.model.CartItem;
import com.ticket.master.booking.repository.CartRepository;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartRepository cartRepository;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
    }

    @Nested
    class addItemsToCartTests {
        @Test
        @WithMockUser(roles = "CUSTOMER")
        void addItemsToCartSuccess() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            CartItem item = CartItem.builder()
                    .eventId(eventId)
                    .ticketId(ticketId)
                    .price(BigDecimal.TEN)
                    .build();

            mockMvc.perform(post("/api/carts/{userId}/add", userId)
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "CUSTOMER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(item))))
                    .andExpect(status().isOk());
        }

        @Test
        void addItemsToCartUnauthorized() throws Exception {
            UUID userId = UUID.randomUUID();
            CartItem item = CartItem.builder()
                    .eventId(UUID.randomUUID())
                    .ticketId(UUID.randomUUID())
                    .price(BigDecimal.TEN)
                    .build();

            mockMvc.perform(post("/api/carts/{userId}/add", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(item))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class removeItemFromCartTests {
        @Test
        @WithMockUser(roles = "CUSTOMER")
        void removeItemFromCartSuccess() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            CartItem item = CartItem.builder()
                    .eventId(eventId)
                    .ticketId(ticketId)
                    .price(BigDecimal.TEN)
                    .build();

            mockMvc.perform(post("/api/carts/{userId}/add", userId)
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "CUSTOMER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(item))))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/carts/{userId}/events/{eventId}/tickets/{ticketId}", userId, eventId, ticketId)
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "CUSTOMER"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void removeItemFromCartWhenCartDoesNotExist() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            mockMvc.perform(delete("/api/carts/{userId}/events/{eventId}/tickets/{ticketId}", userId, eventId, ticketId)
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "CUSTOMER"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void removeItemFromCartUnauthorized() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            mockMvc.perform(delete("/api/carts/{userId}/events/{eventId}/tickets/{ticketId}", userId, eventId, ticketId))
                    .andExpect(status().isForbidden());
        }
    }
}
