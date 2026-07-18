package com.ticket.master.event.controller.hall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.event.config.SecurityConfig;
import com.ticket.master.event.controller.HallController;
import com.ticket.master.event.dto.HallRequest;
import com.ticket.master.event.dto.HallResponse;
import com.ticket.master.event.service.HallService;
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

@WebMvcTest(HallController.class)
@Import(SecurityConfig.class)
public class HallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HallService hallService;

    @BeforeEach
    void setUp() {
        Mockito.reset(hallService);
    }

    private HallRequest createValidRequest() {
        return new HallRequest("O2 Arena", "London", "Peninsula Square", 20000);
    }

    private HallResponse createMockResponse(UUID id) {
        return new HallResponse(id, "O2 Arena", "London", "Peninsula Square", 20000);
    }

    @Nested
    class CreateHall {

        @Test
        void shouldCreateHallAndReturn201() throws Exception {
            HallRequest request = createValidRequest();
            HallResponse response = createMockResponse(UUID.randomUUID());

            Mockito.when(hallService.createHall(any(HallRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/halls")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("O2 Arena"))
                    .andExpect(jsonPath("$.capacity").value(20000));
        }

        @Test
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            HallRequest invalidRequest = new HallRequest("", "London", "Peninsula Square", -100);

            mockMvc.perform(post("/api/halls")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            Mockito.verifyNoInteractions(hallService);
        }
    }

    @Nested
    class GetHall {

        @Test
        void shouldReturnHallJson() throws Exception {
            UUID hallId = UUID.randomUUID();
            HallResponse response = createMockResponse(hallId);

            Mockito.when(hallService.getHallById(hallId)).thenReturn(response);

            mockMvc.perform(get("/api/halls/{id}", hallId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(hallId.toString()))
                    .andExpect(jsonPath("$.name").value("O2 Arena"));
        }
    }

    @Nested
    class GetAllHalls {

        @Test
        void shouldReturnListOfHalls() throws Exception {
            HallResponse response1 = createMockResponse(UUID.randomUUID());
            HallResponse response2 = new HallResponse(UUID.randomUUID(), "Wembley", "London", "Wembley Way", 90000);

            Mockito.when(hallService.getAllHalls()).thenReturn(List.of(response1, response2));

            mockMvc.perform(get("/api/halls"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("O2 Arena"))
                    .andExpect(jsonPath("$[1].name").value("Wembley"));
        }
    }

    @Nested
    class UpdateHall {

        @Test
        void shouldUpdateHallAndReturn200() throws Exception {
            UUID hallId = UUID.randomUUID();
            HallRequest request = new HallRequest("Wembley Updated", "London", "Wembley Way", 90000);
            HallResponse response = new HallResponse(hallId, "Wembley Updated", "London", "Wembley Way", 90000);

            Mockito.when(hallService.updateHall(eq(hallId), any(HallRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/halls/{id}", hallId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Wembley Updated"));
        }

        @Test
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            UUID hallId = UUID.randomUUID();
            HallRequest invalidRequest = new HallRequest("Wembley", "", "", -1);

            mockMvc.perform(put("/api/halls/{id}", hallId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            Mockito.verifyNoInteractions(hallService);
        }
    }

    @Nested
    class DeleteHall {

        @Test
        void shouldDeleteHallAndReturn204() throws Exception {
            UUID hallId = UUID.randomUUID();

            mockMvc.perform(delete("/api/halls/{id}", hallId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNoContent());

            Mockito.verify(hallService).deleteHall(hallId);
        }
    }
}
