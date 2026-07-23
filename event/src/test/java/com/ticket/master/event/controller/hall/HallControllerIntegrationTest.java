package com.ticket.master.event.controller.hall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.event.TestcontainersConfiguration;
import com.ticket.master.event.dto.HallRequest;
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
public class HallControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    public class CreateHall {

        @Test
        public void shouldCreateHallAndReturn201() throws Exception {
            HallRequest request = new HallRequest("O2 Arena", "London", "Peninsula Square", 20000);

            mockMvc.perform(post("/api/halls")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("O2 Arena"))
                    .andExpect(jsonPath("$.capacity").value(20000));
        }

        @Test
        public void shouldReturnBadRequestWhenValidationFails() throws Exception {
            HallRequest invalidRequest = new HallRequest("", "London", "Peninsula Square", -100);

            mockMvc.perform(post("/api/halls")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
            HallRequest request = new HallRequest("O2 Arena", "London", "Peninsula Square", 20000);

            mockMvc.perform(post("/api/halls")
                            .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    public class GetHall {

        @Test
        public void shouldReturnHallJson() throws Exception {
            HallRequest request = new HallRequest("O2 Arena", "London", "Peninsula Square", 20000);

            String response = mockMvc.perform(post("/api/halls")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();

            String hallId = objectMapper.readTree(response).get("id").asText();

            mockMvc.perform(get("/api/halls/{id}", hallId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(hallId))
                    .andExpect(jsonPath("$.name").value("O2 Arena"));
        }

        @Test
        public void shouldReturnNotFoundWhenHallDoesntExist() throws Exception {
            mockMvc.perform(get("/api/halls/{id}", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    public class GetAllHalls {

        @Test
        public void shouldReturnListOfHalls() throws Exception {
            HallRequest request1 = new HallRequest("O2 Arena", "London", "Peninsula Square", 20000);
            HallRequest request2 = new HallRequest("Wembley", "London", "Wembley Way", 90000);

            mockMvc.perform(post("/api/halls")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/halls")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/halls")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("O2 Arena"))
                    .andExpect(jsonPath("$[1].name").value("Wembley"));
        }
    }

    @Nested
    public class UpdateHall {

        @Test
        public void shouldUpdateHallAndReturn200() throws Exception {
            HallRequest createRequest = new HallRequest("O2 Arena", "London", "Peninsula Square", 20000);

            String createResponse = mockMvc.perform(post("/api/halls")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn().getResponse().getContentAsString();

            String hallId = objectMapper.readTree(createResponse).get("id").asText();

            HallRequest updateRequest = new HallRequest("Wembley Updated", "London", "Wembley Way", 90000);

            mockMvc.perform(put("/api/halls/{id}", hallId)
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Wembley Updated"))
                    .andExpect(jsonPath("$.capacity").value(90000));
        }

        @Test
        public void shouldReturnBadRequestWhenValidationFails() throws Exception {
            HallRequest createRequest = new HallRequest("O2 Arena", "London", "Peninsula Square", 20000);

            String createResponse = mockMvc.perform(post("/api/halls")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn().getResponse().getContentAsString();

            String hallId = objectMapper.readTree(createResponse).get("id").asText();

            HallRequest invalidRequest = new HallRequest("Wembley", "", "", -1);

            mockMvc.perform(put("/api/halls/{id}", hallId)
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnNotFoundWhenHallDoesntExist() throws Exception {
            HallRequest updateRequest = new HallRequest("Wembley Updated", "London", "Wembley Way", 90000);

            mockMvc.perform(put("/api/halls/{id}", UUID.randomUUID())
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
            HallRequest updateRequest = new HallRequest("Wembley Updated", "London", "Wembley Way", 90000);

            mockMvc.perform(put("/api/halls/{id}", UUID.randomUUID())
                            .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    public class DeleteHall {

        @Test
        public void shouldDeleteHallAndReturn204() throws Exception {
            HallRequest request = new HallRequest("O2 Arena", "London", "Peninsula Square", 20000);

            String createResponse = mockMvc.perform(post("/api/halls")
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();

            String hallId = objectMapper.readTree(createResponse).get("id").asText();

            mockMvc.perform(delete("/api/halls/{id}", hallId)
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN")))
                    .andExpect(status().isNoContent());
        }

        @Test
        public void shouldReturnNotFoundWhenHallDoesntExist() throws Exception {
            mockMvc.perform(delete("/api/halls/{id}", UUID.randomUUID())
                            .with(user(UUID.randomUUID().toString()).roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
            mockMvc.perform(delete("/api/halls/{id}", UUID.randomUUID())
                            .with(user(UUID.randomUUID().toString()).roles("CUSTOMER")))
                    .andExpect(status().isForbidden());
        }
    }
}
