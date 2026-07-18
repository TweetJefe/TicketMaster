package com.ticket.master.event.controller.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.common.enums.TicketType;
import com.ticket.master.event.config.SecurityConfig;
import com.ticket.master.event.controller.EventController;
import com.ticket.master.event.dto.CategoryRequestDTO;
import com.ticket.master.event.dto.CreateEventDTO;
import com.ticket.master.event.dto.EventDTO;
import com.ticket.master.event.dto.UpdateEventDTO;
import com.ticket.master.event.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @BeforeEach
    void setUp() {
        Mockito.reset(eventService);
    }

    private CreateEventDTO createValidRequest() {
        return new CreateEventDTO(
                "Epic Concert", "123 Main St", "London",
                Instant.now().plus(30, ChronoUnit.DAYS),
                UUID.randomUUID(), List.of(UUID.randomUUID()),
                List.of(new CategoryRequestDTO(TicketType.VIP, 100, 150.0))
        );
    }

    @Nested
    class CreateEvent {

        @Test
        void shouldCreateEventAndReturn201() throws Exception {
            CreateEventDTO request = createValidRequest();

            mockMvc.perform(post("/api/events")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ORGANIZATION")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            Mockito.verify(eventService).createEvent(any(CreateEventDTO.class));
        }

        @Test
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            CreateEventDTO invalidRequest = new CreateEventDTO(
                    "", "123 Main St", "London",
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    UUID.randomUUID(), List.of(UUID.randomUUID()),
                    List.of(new CategoryRequestDTO(TicketType.VIP, 100, 150.0))
            );

            mockMvc.perform(post("/api/events")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ORGANIZATION")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            Mockito.verifyNoInteractions(eventService);
        }

        @Test
        void shouldReturnForbiddenWhenUserIsCustomer() throws Exception {
            CreateEventDTO request = createValidRequest();

            mockMvc.perform(post("/api/events")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "CUSTOMER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            Mockito.verifyNoInteractions(eventService);
        }
    }

    @Nested
    class UpdateEvent {

        @Test
        void shouldUpdateEventAndReturn200() throws Exception {
            UUID eventId = UUID.randomUUID();
            UpdateEventDTO updateRequest = new UpdateEventDTO(
                    "Updated Concert", "456 New St", "Paris",
                    Instant.now().plus(40, ChronoUnit.DAYS),
                    UUID.randomUUID(), null, null
            );

            mockMvc.perform(patch("/api/events/{id}", eventId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ORGANIZATION")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());

            Mockito.verify(eventService).updateEvent(eq(eventId), any(UpdateEventDTO.class));
        }

        @Test
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            UUID eventId = UUID.randomUUID();
            UpdateEventDTO invalidRequest = new UpdateEventDTO(
                    "Updated Concert", "456 New St", "Paris",
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    UUID.randomUUID(), null, null
            );

            mockMvc.perform(patch("/api/events/{id}", eventId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ORGANIZATION")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            Mockito.verifyNoInteractions(eventService);
        }

        @Test
        void shouldReturnForbiddenWhenUserIsCustomer() throws Exception {
            UUID eventId = UUID.randomUUID();
            UpdateEventDTO updateRequest = new UpdateEventDTO(
                    "Updated Concert", "456 New St", "Paris",
                    Instant.now().plus(40, ChronoUnit.DAYS),
                    UUID.randomUUID(), null, null
            );

            mockMvc.perform(patch("/api/events/{id}", eventId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "CUSTOMER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());

            Mockito.verifyNoInteractions(eventService);
        }
    }

    @Nested
    class DeleteEvent {

        @Test
        void shouldDeleteEventAndReturn204() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(delete("/api/events/{id}", eventId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ORGANIZATION"))
                    .andExpect(status().isNoContent());

            Mockito.verify(eventService).deleteEvent(eventId);
        }

        @Test
        void shouldReturnForbiddenWhenUserIsCustomer() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(delete("/api/events/{id}", eventId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "CUSTOMER"))
                    .andExpect(status().isForbidden());

            Mockito.verifyNoInteractions(eventService);
        }
    }

    @Nested
    class GetAllEventsInTheCity {

        @Test
        void shouldReturnPagedEvents() throws Exception {
            String city = "London";
            EventDTO event1 = new EventDTO(UUID.randomUUID(), "Concert 1", "Addr 1", city, Instant.now().plus(10, ChronoUnit.DAYS), null, null, null);
            EventDTO event2 = new EventDTO(UUID.randomUUID(), "Concert 2", "Addr 2", city, Instant.now().plus(20, ChronoUnit.DAYS), null, null, null);
            Page<EventDTO> page = new PageImpl<>(List.of(event1, event2));

            Mockito.when(eventService.getAllEventsInTheCity(eq(city), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/events")
                            .param("city", city)
                            .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].name").value("Concert 1"))
                    .andExpect(jsonPath("$.content[1].name").value("Concert 2"));
        }

        @Test
        void shouldReturnEmptyPageWhenNoEvents() throws Exception {
            String city = "Moscow";
            Page<EventDTO> emptyPage = new PageImpl<>(List.of());

            Mockito.when(eventService.getAllEventsInTheCity(eq(city), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/events")
                            .param("city", city))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    class GetEventById {

        @Test
        void shouldReturnEventJson() throws Exception {
            UUID eventId = UUID.randomUUID();
            EventDTO mockResponse = new EventDTO(
                    eventId, "Epic Concert", "123 Main St", "London",
                    Instant.now().plus(10, ChronoUnit.DAYS),
                    null, null, null
            );

            Mockito.when(eventService.getEvent(eventId)).thenReturn(mockResponse);

            mockMvc.perform(get("/api/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Epic Concert"))
                    .andExpect(jsonPath("$.city").value("London"))
                    .andExpect(jsonPath("$.id").value(eventId.toString()));
        }

        @Test
        void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
            UUID randomId = UUID.randomUUID();
            Mockito.when(eventService.getEvent(randomId))
                    .thenThrow(new EntityNotFoundException("Event not found"));
            mockMvc.perform(get("/api/events/{id}", randomId))
                    .andExpect(status().isNotFound());
        }
    }
}
