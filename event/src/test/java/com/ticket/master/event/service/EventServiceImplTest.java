package com.ticket.master.event.service;

import com.ticket.master.common.enums.TicketType;
import com.ticket.master.event.dto.*;
import com.ticket.master.event.kafka.EventKafkaProducer;
import com.ticket.master.event.mapper.*;
import com.ticket.master.event.model.*;
import com.ticket.master.event.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import com.ticket.master.common.exception.UniquenessViolation;
import com.ticket.master.common.exception.NullableViolation;
import com.ticket.master.common.exception.ServerException;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository repository;

    @Mock
    private HallRepository hallRepository;

    @Mock
    private PerformerRepository performerRepository;

    @Mock
    private EventKafkaProducer kafkaProd;

    @Spy
    private HallMapper hallMapper;

    @Spy
    private PerformerMapper performerMapper;

    @InjectMocks
    @Spy
    private EventMapper mapper;

    @InjectMocks
    private EventServiceImpl service;


    private UUID eventId;
    private UUID hallId;
    private UUID performerId;
    
    private Hall hall;
    private Performer performer;
    private Event event;
    private Event savedEvent;
    
    private CreateEventDTO createEventDTO;
    private UpdateEventDTO updateEventDTO;

    @BeforeEach
    void setUp() {
        HallMapper hallMapper = new HallMapper();
        PerformerMapper performerMapper = new PerformerMapper();
        mapper = new EventMapper(hallMapper, performerMapper);
        service = new EventServiceImpl(repository, hallRepository, performerRepository, mapper, kafkaProd);

        eventId = UUID.randomUUID();
        hallId = UUID.randomUUID();
        performerId = UUID.randomUUID();

        hall = Hall.builder()
                .id(hallId)
                .name("Grand Arena")
                .city("Madrid")
                .address("Avenue 1")
                .capacity(Integer.valueOf(5000))
                .build();

        performer = Performer.builder()
                .id(performerId)
                .name("Kanye West")
                .genre("Hip-Hop")
                .description("Description")
                .build();

        CategoryRequestDTO categoryRequest = new CategoryRequestDTO(TicketType.ECONOMY, Integer.valueOf(100), 50.0);

        createEventDTO = new CreateEventDTO(
                "Concert",
                "Avenue 1",
                "Madrid",
                Instant.now().plusSeconds(3600),
                hallId,
                List.of(performerId),
                List.of(categoryRequest)
        );

        event = Event.builder()
                .name("Concert")
                .address("Avenue 1")
                .city("Madrid")
                .time(createEventDTO.time())
                .hall(hall)
                .performers(Set.of(performer))
                .build();

        EventCategory category = EventCategory.builder()
                .id(UUID.randomUUID())
                .type(TicketType.ECONOMY)
                .quantity(Integer.valueOf(100))
                .price(50.0)
                .event(event)
                .build();
        event.addCategory(category);

        savedEvent = Event.builder()
                .id(eventId)
                .name("Concert")
                .address("Avenue 1")
                .city("Madrid")
                .time(createEventDTO.time())
                .hall(hall)
                .performers(Set.of(performer))
                .build();
        
        EventCategory savedCategory = EventCategory.builder()
                .id(UUID.randomUUID())
                .type(TicketType.ECONOMY)
                .quantity(Integer.valueOf(100))
                .price(50.0)
                .event(savedEvent)
                .build();
        savedEvent.addCategory(savedCategory);

        updateEventDTO = new UpdateEventDTO(
                "Updated Concert",
                "Avenue 2",
                "Madrid",
                Instant.now().plusSeconds(7200),
                hallId,
                List.of(performerId),
                List.of(categoryRequest)
        );
    }

    @Nested
    public class eventCreateTests{
        @Test
        public void createEventSuccess(){
            when(hallRepository.findById(createEventDTO.hallId())).thenReturn(Optional.of(hall));
            when(performerRepository.findAllById(createEventDTO.performerIds())).thenReturn(List.of(performer));
            when(repository.save(any(Event.class))).thenReturn(savedEvent);

            EventDTO actualResponse = service.createEvent(createEventDTO);

            assertThat(actualResponse).isNotNull();
            assertEquals(eventId, actualResponse.id());
            assertEquals("Concert", actualResponse.name());
            assertEquals(1, actualResponse.performers().size());

            assertNotNull(actualResponse.hall());
            assertEquals(hallId, actualResponse.hall().id());
            assertEquals("Grand Arena", actualResponse.hall().name());
            assertEquals("Madrid", actualResponse.hall().city());

            PerformerResponse actualPerformer = actualResponse.performers().iterator().next();
            assertEquals("Kanye West", actualPerformer.name());

            verify(hallRepository, times(1)).findById(any(UUID.class));
            verify(performerRepository, times(1)).findAllById(createEventDTO.performerIds());
            verify(kafkaProd, times(1)).sendEventCreatedMessage(argThat
                    (msg -> msg.eventId().equals(eventId)));
            verify(repository, times(1)).save(any(Event.class));
        }

        @Test
        public void createEvent_HallNotFound(){
            when(hallRepository.findById(createEventDTO.hallId())).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(EntityNotFoundException.class,
                            () -> service.createEvent(createEventDTO));
            assertEquals("Hall not found", exception.getMessage());

            verify(performerRepository, never()).findAllById(any());
            verify(kafkaProd, never()).sendEventCreatedMessage(any());
            verify(repository, never()).save(any());
        }

        @Test
        public void createEvent_PerformerNotFound(){
            when(hallRepository.findById(createEventDTO.hallId())).thenReturn(Optional.of(hall));
            when(performerRepository.findAllById(createEventDTO.performerIds())).thenReturn(List.of());

            EntityNotFoundException exception =
                    assertThrows(EntityNotFoundException.class,
                            () -> service.createEvent(createEventDTO));
            assertEquals("No performers found for given IDs", exception.getMessage());

            verify(hallRepository, times(1)).findById(any(UUID.class));
            verify(kafkaProd, never()).sendEventCreatedMessage(any());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    public class updateEventTests{
        @Test
        public void updateEventSuccess(){
            when(repository.findById(eventId)).thenReturn(Optional.of(event));
            when(hallRepository.findById(updateEventDTO.hallId())).thenReturn(Optional.of(hall));
            when(performerRepository.findAllById(updateEventDTO.performerIds())).thenReturn(List.of(performer));
            when(repository.save(any(Event.class))).thenReturn(savedEvent);

            EventDTO actualResponse = service.updateEvent(eventId, updateEventDTO);

            assertNotNull(actualResponse);
            assertEquals(1, actualResponse.performers().size());
            assertEquals("Kanye West", actualResponse.performers().iterator().next().name());
            assertEquals(savedEvent.getHall().getId(), actualResponse.hall().id());
            assertEquals(savedEvent.getName(), actualResponse.name());
            
            PerformerResponse actualPerformer = actualResponse.performers().iterator().next();
            assertEquals("Kanye West", actualPerformer.name());

            verify(repository, times(1)).findById(any(UUID.class));
            verify(hallRepository,times(1)).findById(any(UUID.class));
            verify(performerRepository,times(1)).findAllById(updateEventDTO.performerIds());
            verify(kafkaProd, times(1)).sendEventUpdatedMessage(any());
            verify(repository,times(1)).save(any(Event.class));
        }

        @Test
        public void updateEvent_HallNotFound(){
            when(repository.findById(eventId)).thenReturn(Optional.of(event));
            when(hallRepository.findById(updateEventDTO.hallId())).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(EntityNotFoundException.class,
                            () -> service.updateEvent(eventId, updateEventDTO));

            assertEquals("Hall not found", exception.getMessage());

            verify(repository, times(1)).findById(any(UUID.class));
            verify(performerRepository, never()).findAllById(any());
            verify(kafkaProd, never()).sendEventUpdatedMessage(any());
            verify(repository,never()).save(any(Event.class));
        }

        @Test
        public void updateEvent_EventNotFound(){
            when(repository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.updateEvent(eventId, updateEventDTO));
            
            verify(hallRepository, never()).findById(any());
            verify(performerRepository, never()).findAllById(any());
            verify(kafkaProd, never()).sendEventUpdatedMessage(any());
            verify(repository, never()).save(any(Event.class));
        }

        @Test
        public void updateEvent_PerformersNotFound_SetsEmptyPerformers() {
            when(repository.findById(eventId)).thenReturn(Optional.of(event));
            when(hallRepository.findById(updateEventDTO.hallId())).thenReturn(Optional.of(hall));
            when(performerRepository.findAllById(updateEventDTO.performerIds())).thenReturn(List.of());
            when(repository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

            EventDTO actualResponse = service.updateEvent(eventId, updateEventDTO);

            assertNotNull(actualResponse);
            assertTrue(actualResponse.performers().isEmpty());

            verify(performerRepository, times(1)).findAllById(updateEventDTO.performerIds());
            verify(repository, times(1)).save(any(Event.class));
            verify(kafkaProd, times(1)).sendEventUpdatedMessage(any());
        }
    }

    @Nested
    public class deleteEventTests {
        @Test
        public void deleteEventSuccess() {
            when(repository.findById(eventId)).thenReturn(Optional.of(savedEvent));

            service.deleteEvent(eventId);

            verify(repository, times(1)).deleteById(eventId);
            verify(kafkaProd, times(1)).sendEventDeletedMessage(argThat(msg -> msg.eventId().equals(eventId)));
        }

        @Test
        public void deleteEvent_NotFound() {
            when(repository.findById(eventId)).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(EntityNotFoundException.class,
                            () -> service.deleteEvent(eventId));

            assertEquals("Event with ID " + eventId + " not found", exception.getMessage());
            verify(repository, never()).deleteById(any(UUID.class));
            verify(kafkaProd, never()).sendEventDeletedMessage(any());
        }
    }

    @Nested
    public class getEventTests {
        @Test
        public void getEventSuccess() {
            when(repository.findById(eventId)).thenReturn(Optional.of(savedEvent));

            EventDTO actualResponse = service.getEvent(eventId);

            assertNotNull(actualResponse);
            assertEquals(eventId, actualResponse.id());
            assertEquals("Concert", actualResponse.name());

            verify(repository, times(1)).findById(eventId);
        }

        @Test
        public void getEvent_NotFound() {
            when(repository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.getEvent(eventId));

            verify(repository, times(1)).findById(eventId);
        }
    }

    @Nested
    public class getAllEventsInTheCityTests {
        @Test
        public void getAllEventsInTheCitySuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> mockPage = new PageImpl<>(List.of(savedEvent));
            
            when(repository.findAllByCityIgnoreCase("Madrid", pageable)).thenReturn(mockPage);

            Page<EventDTO> actualResponse = service.getAllEventsInTheCity("Madrid", pageable);

            assertNotNull(actualResponse);
            assertEquals(1, actualResponse.getContent().size());
            assertEquals("Concert", actualResponse.getContent().get(0).name());
            verify(repository, times(1)).findAllByCityIgnoreCase("Madrid", pageable);
        }

        @Test
        public void getAllEventsInTheCity_EmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> mockPage = new PageImpl<>(List.of());
            
            when(repository.findAllByCityIgnoreCase("London", pageable)).thenReturn(mockPage);

            Page<EventDTO> actualResponse = service.getAllEventsInTheCity("London", pageable);

            assertNotNull(actualResponse);
            assertTrue(actualResponse.getContent().isEmpty());
            verify(repository, times(1)).findAllByCityIgnoreCase("London", pageable);
        }
    }

    @Nested
    public class saveEventTests {
        @Test
        public void saveEventSuccess() {
            when(repository.save(any(Event.class))).thenReturn(savedEvent);

            Event actualResponse = service.saveEvent(event);

            assertNotNull(actualResponse);
            assertEquals(eventId, actualResponse.getId());
            verify(repository, times(1)).save(event);
        }

        @Test
        public void saveEvent_UniquenessViolation() {
            ConstraintViolationException hibernateExc =
                    mock(ConstraintViolationException.class);
            when(hibernateExc.getSQLState()).thenReturn("23505");
            when(hibernateExc.getConstraintName()).thenReturn("uq_event_name");

            DataIntegrityViolationException springExc =
                    new DataIntegrityViolationException("Error", hibernateExc);

            when(repository.save(any(Event.class))).thenThrow(springExc);

            UniquenessViolation exception =
                    assertThrows(UniquenessViolation.class,
                            () -> service.saveEvent(event));

            assertThat(exception.getMessage()).contains("uq_event_name");
            verify(repository, times(1)).save(event);
        }

        @Test
        public void saveEvent_NullableViolation() {
            ConstraintViolationException hibernateExc =
                    mock(ConstraintViolationException.class);
            when(hibernateExc.getSQLState()).thenReturn("23502");
            when(hibernateExc.getMessage()).thenReturn("null_value_error");

            DataIntegrityViolationException springExc =
                    new DataIntegrityViolationException("Error", hibernateExc);

            when(repository.save(any(Event.class))).thenThrow(springExc);

            NullableViolation exception =
                    assertThrows(NullableViolation.class,
                            () -> service.saveEvent(event));

            assertThat(exception.getMessage()).contains("null_value_error");
            verify(repository, times(1)).save(event);
        }

        @Test
        public void saveEvent_ServerException() {
            DataIntegrityViolationException springExc =
                    new DataIntegrityViolationException("Error", new RuntimeException("other error"));

            when(repository.save(any(Event.class))).thenThrow(springExc);

            assertThrows(ServerException.class,
                    () -> service.saveEvent(event));

            verify(repository, times(1)).save(event);
        }
    }
}
