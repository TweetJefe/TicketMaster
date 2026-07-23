package com.ticket.master.event.controller.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.common.enums.TicketType;
import com.ticket.master.event.TestcontainersConfiguration;
import com.ticket.master.event.dto.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID createHallViaApi() throws Exception {
        HallRequest request = new HallRequest("O2 Arena", "London", "Peninsula Square", 20000);
        
        String response = mockMvc.perform(post("/api/halls")
                        .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private UUID createPerformerViaApi() throws Exception {
        PerformerRequest request = new PerformerRequest("Kanye West", "Hip-Hop/Rap", "Artist");

        String response = mockMvc.perform(post("/api/performers")
                        .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    @Nested
    public class CreateEvent{
        @Test
        public void ShouldCreateEventAndReturn201() throws Exception {
            UUID hall = createHallViaApi();
            UUID performer = createPerformerViaApi();

            CreateEventDTO createEventDTO = new CreateEventDTO(
                    "Kanye West: Donda Listening Party",
                    "Peninsula Square",
                    "London",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    hall,
                    List.of(performer),
                    List.of(
                            new CategoryRequestDTO(TicketType.VIP, 500, 299.99),
                            new CategoryRequestDTO(TicketType.STANDARD, 15000, 99.99)
                    )
            );
            mockMvc.perform(post("/api/events")
                        .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").isNotEmpty())
                    .andExpect(jsonPath("$.city").isNotEmpty())
                    .andExpect(jsonPath("$.hall.id").value(hall.toString()));
        }

        @Test
        public void shouldReturnNotFoundWhenHallDoesntExist() throws Exception{
            UUID fakeHallId = UUID.randomUUID();
            UUID performer = createPerformerViaApi();

            CreateEventDTO createEventDTO = new CreateEventDTO(
                    "Fake Concert", "Address", "City",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    fakeHallId,
                    List.of(performer),
                    List.of(new CategoryRequestDTO(TicketType.STANDARD, 100, 50.0))
            );

            mockMvc.perform(post("/api/events")
                    .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createEventDTO)))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnNotFoundWhenPerformerDoesntExist() throws Exception{
            UUID hall = createHallViaApi();
            UUID fakePerformerId = UUID.randomUUID();

            CreateEventDTO createEventDTO = new CreateEventDTO(
                    "Kanye West: Donda Listening Party",
                    "Peninsula Square",
                    "London",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    hall,
                    List.of(fakePerformerId),
                    List.of(
                            new CategoryRequestDTO(TicketType.VIP, 500, 299.99),
                            new CategoryRequestDTO(TicketType.STANDARD, 15000, 99.99)
                    )
            );

            mockMvc.perform(post("/api/events")
                    .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createEventDTO)))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotOrgOrAdmin() throws Exception{
            CreateEventDTO createEventDTO = new CreateEventDTO(
                    "Kanye West: Donda Listening Party",
                    "Peninsula Square",
                    "London",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    UUID.randomUUID(),
                    List.of(UUID.randomUUID()),
                    List.of(
                            new CategoryRequestDTO(TicketType.VIP, 500, 299.99),
                            new CategoryRequestDTO(TicketType.STANDARD, 15000, 99.99)
                    )
            );

            mockMvc.perform(post("/api/events")
                    .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createEventDTO)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    public class UpdateEvent{
        @Test
        public void shouldUpdateEventAndReturn200() throws Exception {
            UUID hall = createHallViaApi();
            UUID performer = createPerformerViaApi();

            CreateEventDTO createDto = new CreateEventDTO(
                    "Old Name", "Old Address", "London",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    hall, List.of(performer),
                    List.of(new CategoryRequestDTO(TicketType.STANDARD, 100, 50.0))
            );

            String createResponse = mockMvc.perform(post("/api/events")
                            .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andReturn().getResponse().getContentAsString();

            String eventId = objectMapper.readTree(createResponse).get("id").asText();

            UpdateEventDTO updateEventDTO = new UpdateEventDTO(
                    "Kanye West: Vultures World Tour",
                    "Wembley Stadium",
                    null,
                    Instant.now().plus(60, ChronoUnit.DAYS),
                    null, null, null
            );

            mockMvc.perform(patch("/api/events/{id}", eventId)
                    .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateEventDTO)))
                    .andExpect(status().isOk())

                    .andExpect(jsonPath("$.name").value("Kanye West: Vultures World Tour"))
                    .andExpect(jsonPath("$.address").value("Wembley Stadium"))
                    .andExpect(jsonPath("$.city").value("London"))
                    .andExpect(jsonPath("$.hall.id").value(hall.toString()));
        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotOrgOrAdmin() throws Exception{
            UpdateEventDTO updateEventDTO = new UpdateEventDTO(
                    "Kanye West: Vultures World Tour", "Wembley Stadium", null,
                    Instant.now().plus(60, ChronoUnit.DAYS), null, null, null
            );

            mockMvc.perform(patch("/api/events/{id}", UUID.randomUUID())
                    .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateEventDTO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void shouldReturnNotFoundWhenHallDoesntExist() throws Exception{
            UUID hall = createHallViaApi();
            UUID performer = createPerformerViaApi();

            CreateEventDTO createDto = new CreateEventDTO(
                    "Old Name", "Old Address", "London",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    hall, List.of(performer),
                    List.of(new CategoryRequestDTO(TicketType.STANDARD, 100, 50.0))
            );

            String createResponse = mockMvc.perform(post("/api/events")
                            .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andReturn().getResponse().getContentAsString();

            String eventId = objectMapper.readTree(createResponse).get("id").asText();

            UUID fakeHallId = UUID.randomUUID();

            UpdateEventDTO updateEventDTO = new UpdateEventDTO(
                    null, null, null, null,
                    fakeHallId,
                    null, null
            );

            mockMvc.perform(patch("/api/events/{id}", eventId)
                    .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateEventDTO)))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnNotFoundWhenPerformerDoesntExist() throws Exception{
            UUID hall = createHallViaApi();
            UUID performer = createPerformerViaApi();

            CreateEventDTO createDto = new CreateEventDTO(
                    "Old Name", "Old Address", "London",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    hall, List.of(performer),
                    List.of(new CategoryRequestDTO(TicketType.STANDARD, 100, 50.0))
            );

            String createResponse = mockMvc.perform(post("/api/events")
                            .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andReturn().getResponse().getContentAsString();

            String eventId = objectMapper.readTree(createResponse).get("id").asText();

            UUID fakePerformerId = UUID.randomUUID();

            UpdateEventDTO updateEventDTO = new UpdateEventDTO(
                    null, null, null, null,
                    null,
                    List.of(fakePerformerId), null
            );

            mockMvc.perform(patch("/api/events/{id}", eventId)
                            .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    public class deleteEvent{
        @Test
        public void shouldDeleteEvent() throws Exception{
            UUID hall = createHallViaApi();
            UUID performer = createPerformerViaApi();

            CreateEventDTO createDto = new CreateEventDTO(
                    "Old Name", "Old Address", "London",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    hall, List.of(performer),
                    List.of(new CategoryRequestDTO(TicketType.STANDARD, 100, 50.0))
            );

            String createResponse = mockMvc.perform(post("/api/events")
                            .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andReturn().getResponse().getContentAsString();

            String eventId = objectMapper.readTree(createResponse).get("id").asText();

            mockMvc.perform(delete("/api/events/{id}", eventId)
                    .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotOrgOrAdmin() throws Exception{
            mockMvc.perform(delete("/api/events/{id}", UUID.randomUUID())
                    .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
        @Test
        public void shouldReturnNotFoundWhenEventDoesntExist() throws Exception{
            mockMvc.perform(delete("/api/events/{id}", UUID.randomUUID())
                    .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    public class GetEvent{
        @Test
        public void shouldReturnEventById() throws Exception{
            UUID hall = createHallViaApi();
            UUID performer = createPerformerViaApi();

            CreateEventDTO createDto = new CreateEventDTO(
                    "Concert", "Address", "London",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    hall, List.of(performer),
                    List.of(new CategoryRequestDTO(TicketType.STANDARD, 100, 50.0))
            );

            String createResponse = mockMvc.perform(post("/api/events")
                            .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andReturn().getResponse().getContentAsString();

            String eventId = objectMapper.readTree(createResponse).get("id").asText();

            mockMvc.perform(get("/api/events/{id}", eventId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(eventId))
                    .andExpect(jsonPath("$.name").value("Concert"));
        }

        @Test
        public void shouldReturnNotFoundWhenEventDoesntExist() throws Exception{
            mockMvc.perform(get("/api/events/{id}", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    public class GetAllEventsInTheCity{
        @Test
        public void shouldReturnEventsByCity() throws Exception{
            UUID hall = createHallViaApi();
            UUID performer = createPerformerViaApi();

            CreateEventDTO createDto = new CreateEventDTO(
                    "Concert in Paris", "Address", "Paris",
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    hall, List.of(performer),
                    List.of(new CategoryRequestDTO(TicketType.STANDARD, 100, 50.0))
            );

            mockMvc.perform(post("/api/events")
                            .with(user(UUID.randomUUID().toString()).roles("ORGANIZATION"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/events")
                    .param("city", "Paris")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].city").value("Paris"))
                    .andExpect(jsonPath("$.content[0].name").value("Concert in Paris"));
        }
    }
}
