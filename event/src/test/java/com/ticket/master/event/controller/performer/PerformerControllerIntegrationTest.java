package com.ticket.master.event.controller.performer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.event.TestcontainersConfiguration;
import com.ticket.master.event.dto.PerformerRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public class PerformerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    public class CreatePerformer {

        @Test
        public void shouldCreatePerformerAndReturn201() throws Exception {
            PerformerRequest request = new PerformerRequest("Kanye West", "Hip-Hop/Rap", "Artist");

            mockMvc.perform(post("/api/performers")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("Kanye West"))
                    .andExpect(jsonPath("$.genre").value("Hip-Hop/Rap"));
        }

        @Test
        public void shouldReturnBadRequestWhenValidationFails() throws Exception {
            PerformerRequest invalidRequest = new PerformerRequest("", "Hip-Hop/Rap", "Artist");

            mockMvc.perform(post("/api/performers")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
            PerformerRequest request = new PerformerRequest("Kanye West", "Hip-Hop/Rap", "Artist");

            mockMvc.perform(post("/api/performers")
                            .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    public class GetPerformer {

        @Test
        public void shouldReturnPerformerJson() throws Exception {
            PerformerRequest request = new PerformerRequest("Kanye West", "Hip-Hop/Rap", "Artist");

            String response = mockMvc.perform(post("/api/performers")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();

            String performerId = objectMapper.readTree(response).get("id").asText();

            mockMvc.perform(get("/api/performers/{id}", performerId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(performerId))
                    .andExpect(jsonPath("$.name").value("Kanye West"));
        }

        @Test
        public void shouldReturnNotFoundWhenPerformerDoesntExist() throws Exception {
            mockMvc.perform(get("/api/performers/{id}", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    public class GetAllPerformers {

        @Test
        public void shouldReturnListOfPerformers() throws Exception {
            PerformerRequest request1 = new PerformerRequest("Kanye West", "Hip-Hop/Rap", "Artist");
            PerformerRequest request2 = new PerformerRequest("Playboi Carti", "Hip-Hop/Rap", "Artist");

            mockMvc.perform(post("/api/performers")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/performers")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/performers")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Kanye West"))
                    .andExpect(jsonPath("$[1].name").value("Playboi Carti"));
        }
    }

    @Nested
    public class UpdatePerformer {

        @Test
        public void shouldUpdatePerformerAndReturn200() throws Exception {
            PerformerRequest createRequest = new PerformerRequest("Kanye West", "Hip-Hop/Rap", "Artist");

            String createResponse = mockMvc.perform(post("/api/performers")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn().getResponse().getContentAsString();

            String performerId = objectMapper.readTree(createResponse).get("id").asText();

            PerformerRequest updateRequest = new PerformerRequest("Ye", "Hip-Hop/Rap", "Artist");

            mockMvc.perform(put("/api/performers/{id}", performerId)
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Ye"));
        }

        @Test
        public void shouldReturnBadRequestWhenValidationFails() throws Exception {
            PerformerRequest createRequest = new PerformerRequest("Kanye West", "Hip-Hop/Rap", "Artist");

            String createResponse = mockMvc.perform(post("/api/performers")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn().getResponse().getContentAsString();

            String performerId = objectMapper.readTree(createResponse).get("id").asText();

            PerformerRequest invalidRequest = new PerformerRequest("", "Hip-Hop/Rap", "Artist");

            mockMvc.perform(put("/api/performers/{id}", performerId)
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnNotFoundWhenPerformerDoesntExist() throws Exception {
            PerformerRequest updateRequest = new PerformerRequest("Ye", "Hip-Hop/Rap", "Artist");

            mockMvc.perform(put("/api/performers/{id}", UUID.randomUUID())
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
            PerformerRequest updateRequest = new PerformerRequest("Ye", "Hip-Hop/Rap", "Artist");

            mockMvc.perform(put("/api/performers/{id}", UUID.randomUUID())
                            .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    public class DeletePerformer {

        @Test
        public void shouldDeletePerformerAndReturn204() throws Exception {
            PerformerRequest request = new PerformerRequest("Kanye West", "Hip-Hop/Rap", "Artist");

            String createResponse = mockMvc.perform(post("/api/performers")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();

            String performerId = objectMapper.readTree(createResponse).get("id").asText();

            mockMvc.perform(delete("/api/performers/{id}", performerId)
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN")))
                    .andExpect(status().isNoContent());
        }

        @Test
        public void shouldReturnNotFoundWhenPerformerDoesntExist() throws Exception {
            mockMvc.perform(delete("/api/performers/{id}", UUID.randomUUID())
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
            mockMvc.perform(delete("/api/performers/{id}", UUID.randomUUID())
                            .with(user(UUID.randomUUID().toString()).roles("CUSTOMER")))
                    .andExpect(status().isForbidden());
        }
    }
}
