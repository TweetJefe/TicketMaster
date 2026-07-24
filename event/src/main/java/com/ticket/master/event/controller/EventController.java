package com.ticket.master.event.controller;

import com.ticket.master.event.dto.CreateEventDTO;
import jakarta.validation.Valid;
import com.ticket.master.event.dto.EventDTO;
import com.ticket.master.event.dto.UpdateEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ticket.master.event.service.EventService;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService service;

    @PreAuthorize("hasRole('ORGANIZATION')")
    @PostMapping
    public ResponseEntity<EventDTO> createEvent (
            @Valid @RequestBody CreateEventDTO dto
    ){
        EventDTO createdEvent = service.createEvent(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @PreAuthorize("hasRole('ORGANIZATION')")
    @PatchMapping("/{id}")
    public ResponseEntity<EventDTO> updateEvent (
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventDTO dto
    ){
        EventDTO updatedEvent = service.updateEvent(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(updatedEvent);
    }

    @PreAuthorize("hasRole('ORGANIZATION')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent (
            @PathVariable UUID id
    ){
        service.deleteEvent(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    public ResponseEntity<Page<EventDTO>> getAllEventsInTheCity (
            @RequestParam(defaultValue = "0") int page,
            @RequestParam String city
    ){
        final int size = 20;
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(service.getAllEventsInTheCity(city, pageable));
    }


    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEvent (
            @PathVariable UUID id
    ){
        return ResponseEntity.status(HttpStatus.OK).body(service.getEvent(id));
    }



}
