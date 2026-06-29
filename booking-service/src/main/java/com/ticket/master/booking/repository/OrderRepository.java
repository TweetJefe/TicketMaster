package com.ticket.master.booking.repository;

import com.ticket.master.booking.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByEventId(UUID eventId);

    List<Order> findAllByEventId(UUID eventId);
}
