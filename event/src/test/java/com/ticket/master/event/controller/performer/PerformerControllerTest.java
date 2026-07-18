package com.ticket.master.event.controller.performer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.event.config.SecurityConfig;
import com.ticket.master.event.controller.PerformerController;
import com.ticket.master.event.dto.PerformerRequest;
import com.ticket.master.event.dto.PerformerResponse;
import com.ticket.master.event.service.PerformerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PerformerController.class)
@Import(SecurityConfig.class)
public class PerformerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PerformerService performerService;

    @BeforeEach
    void setUp() {
        Mockito.reset(performerService);
    }

    private PerformerRequest createValidRequest() {
        return new PerformerRequest("Imagine Dragons", "Pop Rock", "American band");
    }

    private PerformerResponse createMockResponse(UUID id) {
        return new PerformerResponse(id, "Imagine Dragons", "Pop Rock", "American band");
    }

    @Nested
    class CreatePerformer {

        @Test
        void shouldCreatePerformerAndReturn201() throws Exception {
            PerformerRequest request = createValidRequest();
            PerformerResponse response = createMockResponse(UUID.randomUUID());

            Mockito.when(performerService.createPerformer(any(PerformerRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/performers")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Imagine Dragons"))
                    .andExpect(jsonPath("$.genre").value("Pop Rock"));
        }

        @Test
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            PerformerRequest invalidRequest = new PerformerRequest("", "Pop Rock", "American band");

            mockMvc.perform(post("/api/performers")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            Mockito.verifyNoInteractions(performerService);
        }
    }

    @Nested
    class GetPerformer {

        @Test
        void shouldReturnPerformerJson() throws Exception {
            UUID performerId = UUID.randomUUID();
            PerformerResponse response = createMockResponse(performerId);

            Mockito.when(performerService.getPerformerById(performerId)).thenReturn(response);

            mockMvc.perform(get("/api/performers/{id}", performerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(performerId.toString()))
                    .andExpect(jsonPath("$.name").value("Imagine Dragons"));
        }
    }

    @Nested
    class GetAllPerformers {

        @Test
        void shouldReturnListOfPerformers() throws Exception {
            PerformerResponse response1 = createMockResponse(UUID.randomUUID());
            PerformerResponse response2 = new PerformerResponse(UUID.randomUUID(), "Coldplay", "Pop Rock", "British band");

            Mockito.when(performerService.getAllPerformers()).thenReturn(List.of(response1, response2));

            mockMvc.perform(get("/api/performers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Imagine Dragons"))
                    .andExpect(jsonPath("$[1].name").value("Coldplay"));
        }
    }

    @Nested
    class UpdatePerformer {

        @Test
        void shouldUpdatePerformerAndReturn200() throws Exception {
            UUID performerId = UUID.randomUUID();
            PerformerRequest request = new PerformerRequest("Coldplay", "Pop Rock", "British band");
            PerformerResponse response = new PerformerResponse(performerId, "Coldplay", "Pop Rock", "British band");

            Mockito.when(performerService.updatePerformer(eq(performerId), any(PerformerRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/performers/{id}", performerId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Coldplay"));
        }

        @Test
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            UUID performerId = UUID.randomUUID();
            PerformerRequest invalidRequest = new PerformerRequest("", "", "");

            mockMvc.perform(put("/api/performers/{id}", performerId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            Mockito.verifyNoInteractions(performerService);
        }
    }

    @Nested
    class DeletePerformer {

        @Test
        void shouldDeletePerformerAndReturn204() throws Exception {
            UUID performerId = UUID.randomUUID();

            mockMvc.perform(delete("/api/performers/{id}", performerId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNoContent());

            Mockito.verify(performerService).deletePerformer(performerId);
        }
    }
}
