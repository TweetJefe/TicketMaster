package com.ticket.master.ticket.model;

import com.ticket.master.ticket.enums.Status;
import com.ticket.master.common.enums.TicketType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tickets")
@RequiredArgsConstructor
@Getter
@Setter
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Positive
    private double price;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = true)
    private UUID userId;

    private String sector;
    private String row;
    private String seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType type;
}
