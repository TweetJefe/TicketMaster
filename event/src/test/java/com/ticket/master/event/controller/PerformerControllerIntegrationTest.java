package com.ticket.master.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.event.TestcontainersConfiguration;
import com.ticket.master.event.dto.PerformerRequest;
import com.ticket.master.event.dto.PerformerResponse;
import com.ticket.master.event.model.Performer;
import com.ticket.master.event.repository.PerformerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class PerformerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PerformerRepository performerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        performerRepository.deleteAll();
    }

    @Test
    void shouldCreatePerformerWhenUserIsAdmin() throws Exception {
        PerformerRequest request = new PerformerRequest(
                "Rammstein", 
                "Industrial Metal", 
                "German metal band"
        );
        String responseContent = mockMvc.perform(post("/api/performers")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PerformerResponse response = objectMapper.readValue(responseContent, PerformerResponse.class);
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Rammstein");
        assertThat(response.genre()).isEqualTo("Industrial Metal");

        Performer savedPerformer = performerRepository.findById(response.id()).orElse(null);
        assertThat(savedPerformer).isNotNull();
        assertThat(savedPerformer.getName()).isEqualTo("Rammstein");
        assertThat(savedPerformer.getGenre()).isEqualTo("Industrial Metal");
    }

    @Test
    void shouldReturnForbiddenWhenUserIsCustomer() throws Exception {
        PerformerRequest request = new PerformerRequest(
                "Coldplay", 
                "Pop Rock", 
                "British band"
        );
        mockMvc.perform(post("/api/performers")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
