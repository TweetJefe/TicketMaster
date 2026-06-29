package com.ticket.master.event.repository;

import com.ticket.master.event.model.Performer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PerformerRepository extends JpaRepository<Performer, UUID> {
}
