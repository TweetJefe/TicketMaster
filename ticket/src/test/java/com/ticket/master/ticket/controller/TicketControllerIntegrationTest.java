package com.ticket.master.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.common.enums.TicketType;
import com.ticket.master.ticket.TestcontainersConfiguration;
import com.ticket.master.ticket.dto.kafka.BuyTicketRequest;
import com.ticket.master.ticket.enums.Status;
import com.ticket.master.ticket.model.Ticket;
import com.ticket.master.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Nested
    public class BuyTicket {

        @Test
        public void shouldBuyTicketSuccessfully() throws Exception {
            Ticket ticket = new Ticket();
            ticket.setEventId(UUID.randomUUID());
            ticket.setPrice(100.0);
            ticket.setType(TicketType.STANDARD);
            ticket.setSeat("10A");
            ticket.setStatus(Status.AVAILABLE);
            ticket = ticketRepository.save(ticket);

            UUID userId = UUID.randomUUID();
            BuyTicketRequest request = new BuyTicketRequest(ticket.getId(), userId);

            mockMvc.perform(post("/api/tickets/{id}/buy", ticket.getId())
                            .with(user(userId.toString()).roles("CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ticket.getId().toString()))
                    .andExpect(jsonPath("$.status").value(Status.SOLD.name()))
                    .andExpect(jsonPath("$.userId").value(userId.toString()));
        }

        @Test
        public void shouldReturnBadRequestWhenTicketAlreadySold() throws Exception {
            Ticket ticket = new Ticket();
            ticket.setEventId(UUID.randomUUID());
            ticket.setPrice(100.0);
            ticket.setType(TicketType.STANDARD);
            ticket.setSeat("10A");
            ticket.setStatus(Status.SOLD);
            ticket.setUserId(UUID.randomUUID());
            ticket = ticketRepository.save(ticket);

            BuyTicketRequest request = new BuyTicketRequest(ticket.getId(), UUID.randomUUID());

            mockMvc.perform(post("/api/tickets/{id}/buy", ticket.getId())
                            .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnNotFoundWhenTicketDoesntExist() throws Exception {
            UUID ticketId = UUID.randomUUID();
            BuyTicketRequest request = new BuyTicketRequest(ticketId, UUID.randomUUID());

            mockMvc.perform(post("/api/tickets/{id}/buy", ticketId)
                            .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotAuthenticated() throws Exception {
            UUID ticketId = UUID.randomUUID();
            BuyTicketRequest request = new BuyTicketRequest(ticketId, UUID.randomUUID());

            mockMvc.perform(post("/api/tickets/{id}/buy", ticketId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
