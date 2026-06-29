package com.ticket.master.event.service;

import com.ticket.master.event.dto.CreateEventDTO;
import com.ticket.master.event.dto.EventDTO;
import com.ticket.master.event.dto.UpdateEventDTO;
import com.ticket.master.event.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventService {

    EventDTO createEvent (CreateEventDTO dto);

    EventDTO updateEvent (UUID id, UpdateEventDTO dto);

    void deleteEvent (UUID id);

    Page<EventDTO> getAllEventsInTheCity (String city, Pageable pageable);

    EventDTO getEvent (UUID id);

    Event saveEvent (Event event);
}
