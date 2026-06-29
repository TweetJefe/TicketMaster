package com.ticket.master.event.service;

import com.ticket.master.event.dto.CategoryRequestDTO;
import com.ticket.master.event.dto.CreateEventDTO;
import com.ticket.master.event.dto.EventDTO;
import com.ticket.master.event.dto.UpdateEventDTO;
import com.ticket.master.common.exception.NullableViolation;
import com.ticket.master.common.exception.ServerException;
import com.ticket.master.common.exception.UniquenessViolation;
import jakarta.persistence.EntityNotFoundException;
import com.ticket.master.common.kafka.EventCreatedMessage;
import com.ticket.master.common.kafka.EventDeletedMessage;
import com.ticket.master.common.kafka.EventUpdatedMessage;
import com.ticket.master.event.kafka.EventKafkaProducer;
import lombok.RequiredArgsConstructor;
import com.ticket.master.event.mapper.EventMapper;
import com.ticket.master.event.model.Event;
import com.ticket.master.event.model.EventCategory;
import com.ticket.master.event.model.Hall;
import com.ticket.master.event.model.Performer;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticket.master.event.repository.EventRepository;
import com.ticket.master.event.repository.HallRepository;
import com.ticket.master.event.repository.PerformerRepository;
import com.ticket.master.common.scheme.TicketCategory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService{
    private final EventRepository repository;
    private final HallRepository hallRepository;
    private final PerformerRepository performerRepository;
    private final EventMapper mapper;
    private final EventKafkaProducer kafkaProd;

    private final String PostgreSQLUniquenessViolation = "23505";
    private final String PostgreSQLNullableViolation = "23502";

    @Override
    @Transactional
    public EventDTO createEvent(CreateEventDTO dto) {
        Hall hall = hallRepository.findById(dto.hallId())
                .orElseThrow(() -> new EntityNotFoundException("Hall not found"));

        Set<Performer> performers = new HashSet<>(performerRepository.findAllById(dto.performerIds()));
        if (performers.isEmpty()) {
            throw new EntityNotFoundException("No performers found for given IDs");
        }

        Event event = Event.builder()
                .name(dto.name())
                .address(dto.address())
                .city(dto.city())
                .time(dto.time())
                .hall(hall)
                .performers(performers)
                .build();

        if (dto.categories() != null){
            for (CategoryRequestDTO crDTO : dto.categories()){
                EventCategory category = EventCategory.builder()
                        .type(crDTO.type())
                        .price(crDTO.price())
                        .quantity(crDTO.quantity())
                        .build();
                event.addCategory(category);
            }
        }

        Event savedEvent = saveEvent(event);

        List<TicketCategory> kafkaCategories = savedEvent.getCategories().stream()
                .map(cat -> new TicketCategory(cat.getType(), cat.getQuantity(), cat.getPrice()))
                .toList();

        EventCreatedMessage message = new EventCreatedMessage(
                savedEvent.getId(),
                kafkaCategories
        );

        kafkaProd.sendEventCreatedMessage(message);
        return mapper.toDto(savedEvent);
    }

    @Override
    @Transactional
    public EventDTO updateEvent(UUID id, UpdateEventDTO dto) {
        Event event = repository.findById(id)
                .orElseThrow(EntityNotFoundException::new);

        boolean categoriesUpdated = false;

        if (dto.name() != null) event.setName(dto.name());
        if (dto.address() != null) event.setAddress(dto.address());
        if (dto.city() != null) event.setCity(dto.city());
        if (dto.time() != null) event.setTime(dto.time());

        if (dto.hallId() != null) {
            Hall hall = hallRepository.findById(dto.hallId())
                    .orElseThrow(() -> new EntityNotFoundException("Hall not found"));
            event.setHall(hall);
        }

        if (dto.performerIds() != null && !dto.performerIds().isEmpty()) {
            Set<Performer> performers = new HashSet<>(performerRepository.findAllById(dto.performerIds()));
            event.setPerformers(performers);
        }

        if(dto.categories() != null && !dto.categories().isEmpty()){
            event.getCategories().clear();

            for (CategoryRequestDTO crDTO : dto.categories()){
                EventCategory category = EventCategory.builder()
                        .type(crDTO.type())
                        .quantity(crDTO.quantity())
                        .price(crDTO.price())
                        .build();
                event.addCategory(category);
            }
            categoriesUpdated = true;
        }

        event = saveEvent(event);

        if (categoriesUpdated){
            List<TicketCategory> kafkaCategories = event.getCategories().stream()
                    .map(cat -> new TicketCategory(cat.getType(), cat.getQuantity(), cat.getPrice()))
                    .toList();

            EventUpdatedMessage message = new EventUpdatedMessage(
                    event.getId(),
                    kafkaCategories
            );
            kafkaProd.sendEventUpdatedMessage(message);
        }
        return mapper.toDto(event);
    }

    @Override
    @Transactional
    public void deleteEvent(UUID id) {
        repository.findById(id).ifPresentOrElse(event -> {
                    repository.deleteById(id);

                    EventDeletedMessage message = new EventDeletedMessage(event.getId());

                    kafkaProd.sendEventDeletedMessage(message);
                },
                () -> {
                    throw new EntityNotFoundException("Event with ID " + id + " not found");
                }
        );
    }

    @Override
    public Page<EventDTO> getAllEventsInTheCity(String city, Pageable pageable) {
        Page<Event> eventPage = repository.findAllByCityIgnoreCase(city, pageable);
        return eventPage.map(mapper::toDto);
    }

    @Override
    public EventDTO getEvent(UUID id) {
        Event event = repository.findById(id)
                .orElseThrow(EntityNotFoundException::new);
        return mapper.toDto(event);
    }

    @Override
    @Transactional
    public Event saveEvent(Event event) {
        try {
            return repository.save(event);
        }catch (DataIntegrityViolationException exception){
            Throwable cause = exception.getCause();
            if(cause instanceof ConstraintViolationException cve){
                String sqlState = cve.getSQLState();
                if(sqlState.equals(PostgreSQLUniquenessViolation)){
                    String constraintName =cve.getConstraintName();
                    throw new UniquenessViolation(constraintName);
                }else if(sqlState.equals(PostgreSQLNullableViolation)){
                    throw new NullableViolation(cause.getMessage());
                }
            }else{
                throw new ServerException();
            }
        }
        return null;
    }
}
