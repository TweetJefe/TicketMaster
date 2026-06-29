package com.ticket.master.event.repository;


import com.ticket.master.event.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    Page<Event> findAllByCityIgnoreCase(String city, Pageable pageable);
}
