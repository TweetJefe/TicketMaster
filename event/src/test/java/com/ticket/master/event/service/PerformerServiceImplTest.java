package com.ticket.master.event.service;

import com.ticket.master.event.dto.PerformerRequest;
import com.ticket.master.event.dto.PerformerResponse;
import com.ticket.master.event.mapper.PerformerMapper;
import com.ticket.master.event.model.Performer;
import com.ticket.master.event.repository.PerformerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PerformerServiceImplTest {

    @Mock
    private PerformerRepository repository;

    @Spy
    private PerformerMapper mapper;

    @InjectMocks
    private PerformerServiceImpl service;

    private Performer performer;
    private PerformerRequest request;
    private PerformerResponse response;
    private UUID generatedId;
    private Performer savedPerformer;

    @BeforeEach
    void setUp() {
        generatedId = UUID.randomUUID();

        request = new PerformerRequest(
                "Kanye West",
                "Hip-Hop",
                "Description");

        performer = Performer.builder()
                .id(generatedId)
                .name("Kanye West")
                .genre("Hip-Hop")
                .description("Description")
                .build();


        response = new PerformerResponse(
                generatedId,
                "Kanye West",
                "Hip-Hop",
                "Description");

        savedPerformer = Performer.builder()
                .id(generatedId)
                .name("Kanye West")
                .genre("Hip-Hop")
                .description("Description")
                .build();

    }

    @Nested
    public class createPerformerTests{
        @Test
        public void createPerformerSuccess(){
            when(repository.save(any(Performer.class))).thenReturn(savedPerformer);

            PerformerResponse actualResponse = service.createPerformer(request);

            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.id()).isEqualTo(generatedId);
            assertThat(actualResponse.name()).isEqualTo("Kanye West");

            verify(repository, times(1)).save(any(Performer.class));
        }

        @Test
        public void createPerformerRepositoryFails(){
            when(repository.save(any(Performer.class))).thenThrow(new RuntimeException("Error text"));

            assertThatThrownBy(() -> service.createPerformer(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error text");

            verify(repository, times(1)).save(any(Performer.class));
        }
    }

    @Nested
    public class updatePerformerTests{
        @Test
        public void updatePerformerSuccess(){
            PerformerRequest updateRequest = new PerformerRequest(
                    "Ye",
                    "Hip-Hop",
                    "Description");

            Performer updatedPerformer = Performer.builder()
                    .id(generatedId)
                    .name("Ye")
                    .genre("Hip-Hop")
                    .description("Description")
                    .build();

            when(repository.findById(generatedId)).thenReturn(Optional.of(performer));
            when(repository.save(any(Performer.class))).thenReturn(updatedPerformer);

            PerformerResponse actualResponse = service.updatePerformer(generatedId, updateRequest);

            assertThat(actualResponse.id()).isNotNull();
            assertThat(actualResponse.name()).isEqualTo("Ye");

            verify(repository, times(1)).findById(generatedId);
            verify(repository, times(1)).save(any(Performer.class));
        }

        @Test
        public void updatePerformerNotFound(){
            when(repository.findById(generatedId)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.updatePerformer(generatedId, request));
            verify(repository, never()).save(any(Performer.class));

        }
    }

    @Nested
    public class deletePerformer{
        @Test
        public void deletePerformerSuccess(){
            when(repository.findById(generatedId)).thenReturn(Optional.of(performer));
            service.deletePerformer(generatedId);
            verify(repository, times(1)).delete(performer);
        }

        @Test
        public void deletePerformerNotFound(){
            when(repository.findById(generatedId)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.deletePerformer(generatedId));
            verify(repository, never()).delete(any(Performer.class));
        }
    }

    @Nested
    public class getPerformerTests{
        @Test
        public void getPerformerByIdSuccess(){
            when(repository.findById(generatedId)).thenReturn(Optional.of(savedPerformer));

            PerformerResponse actualResponse = service.getPerformerById(generatedId);

            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.id()).isEqualTo(generatedId);
            assertThat(actualResponse.name()).isEqualTo(performer.getName());

            verify(repository, times(1)).findById(generatedId);
        }

        @Test
        public void getPerformerByIdNotFound(){
            when(repository.findById(generatedId)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.getPerformerById(generatedId));
            verify(repository, times(1)).findById(generatedId);
        }

        @Test
        public void getAllPerformersSuccess(){
            List<Performer> testPerformers = List.of(performer);
            when(repository.findAll()).thenReturn(testPerformers);

            List<PerformerResponse> actualResponse = service.getAllPerformers();

            assertNotNull(actualResponse);
            assertEquals(1, actualResponse.size());
            assertEquals("Kanye West", actualResponse.get(0).name());

            verify(repository, times(1)).findAll();
        }

        @Test
        public void getAllPerformersNotFoundAny(){
            when(repository.findAll()).thenReturn(List.of());

            List<PerformerResponse> actualResponse = service.getAllPerformers();

            assertNotNull(actualResponse);
            assertTrue(actualResponse.isEmpty());

            verify(repository, times(1)).findAll();
        }


    }
}