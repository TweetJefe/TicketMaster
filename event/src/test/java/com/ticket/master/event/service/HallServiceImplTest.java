package com.ticket.master.event.service;

import com.ticket.master.event.dto.HallRequest;
import com.ticket.master.event.dto.HallResponse;
import com.ticket.master.event.mapper.HallMapper;
import com.ticket.master.event.model.Hall;
import com.ticket.master.event.repository.HallRepository;
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

@ExtendWith(MockitoExtension.class)
public class HallServiceImplTest {

    @Mock
    private HallRepository repository;

    @Spy
    private HallMapper mapper;

    @InjectMocks
    private HallServiceImpl service;

    private HallRequest request;
    private Hall hall;
    private Hall savedHall;
    private UUID generatedId;

    @BeforeEach
    void setUp(){
        generatedId = UUID.randomUUID();

        request = new HallRequest(
                "Grand Arena",
                "Madrid",
                "Avenue 1",
                5000);

        hall = new Hall();
        hall.setName("Grand Arena");
        hall.setCity("Madrid");
        hall.setAddress("Avenue 1");
        hall.setCapacity(5000);

        savedHall = new Hall();
        savedHall.setId(generatedId);
        savedHall.setName("Grand Arena");
        savedHall.setCity("Madrid");
        savedHall.setAddress("Avenue 1");
        savedHall.setCapacity(5000);
    }

    @Nested
    public class createHallTests{
        @Test
        public void createHallSuccess(){
            when(repository.save(hall)).thenReturn(savedHall);

            HallResponse actualResponse = service.createHall(request);

            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.id()).isEqualTo(generatedId);
            assertThat(actualResponse.name()).isEqualTo("Grand Arena");

            verify(repository, times(1)).save(hall);
        }

        @Test
        public void createHallShouldRepositoryFails(){
            when(repository.save(hall)).thenThrow(new RuntimeException("Error text"));

            assertThatThrownBy(() -> service.createHall(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error text");

            verify(repository, times(1)).save(hall);
        }
    }

    @Nested
    public class updateHallTests{
        @Test
        public void updateHallSuccess(){
            HallRequest updateRequest = new HallRequest(
                    "Super Arena",
                    "Madrid",
                    "Avenue 1",
                    6000);

            Hall updatedHall = new Hall();
            updatedHall.setId(generatedId);
            updatedHall.setName("Super Arena");
            updatedHall.setCity("Madrid");
            updatedHall.setAddress("Avenue 1");
            updatedHall.setCapacity(6000);

            HallResponse updatedResponse = new HallResponse(
                    generatedId,
                    "Super Arena",
                    "Madrid",
                    "Avenue 1",
                    6000
            );

            when(repository.findById(generatedId)).thenReturn(Optional.of(hall));
            when(repository.save(any(Hall.class))).thenReturn(updatedHall);

            HallResponse actualResponse = service.updateHall(generatedId, updateRequest);

            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.name()).isEqualTo("Super Arena");
            assertThat(actualResponse.capacity()).isEqualTo(6000);

            verify(repository, times(1)).findById(generatedId);
            verify(repository, times(1)).save(any(Hall.class));
        }

        @Test
        public void updateHallNotFound(){
            when(repository.findById(generatedId)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.updateHall(generatedId, request));
            verify(repository, never()).save(any(Hall.class));
        }
    }

    @Nested
    public class deleteHallTests{
        @Test
        public void deleteHallSuccess(){
            when(repository.existsById(generatedId)).thenReturn(true);
            service.deleteHall(generatedId);
            verify(repository, times(1)).deleteById(generatedId);
        }

        @Test
        public void deleteHallNotFound(){
            when(repository.existsById(generatedId)).thenReturn(false);
            assertThrows(EntityNotFoundException.class, () -> service.deleteHall(generatedId));
            verify(repository, never()).deleteById(any(UUID.class));
        }
    }

    @Nested
    public class getTests{
        @Test
        public void getHallByIdSuccess(){
            when(repository.findById(generatedId)).thenReturn(Optional.of(savedHall));

            HallResponse actualResponse = service.getHallById(generatedId);

            assertNotNull(actualResponse);
            assertEquals(generatedId, actualResponse.id());
            assertEquals("Grand Arena", actualResponse.name());

            verify(repository, times(1)).findById(generatedId);
        }

        @Test
        public void getHallByIdNotFound(){
            when(repository.findById(generatedId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.getHallById(generatedId));

            verify(repository, times(1)).findById(generatedId);
        }

        @Test
        public void getAllHallsSuccess(){
            List<Hall> halls = List.of(hall);
            when(repository.findAll()).thenReturn(halls);

            List<HallResponse> actualResponse = service.getAllHalls();

            assertNotNull(actualResponse);
            assertEquals(1, actualResponse.size());
            assertEquals("Grand Arena", actualResponse.get(0).name());

            verify(repository, times(1)).findAll();
        }

        @Test
        public void getAllHallsNotFoundAny(){
            when(repository.findAll()).thenReturn(List.of());

            List<HallResponse> actualResponses = service.getAllHalls();

            assertNotNull(actualResponses);
            assertTrue(actualResponses.isEmpty());

            verify(repository, times(1)).findAll();
        }
    }
}