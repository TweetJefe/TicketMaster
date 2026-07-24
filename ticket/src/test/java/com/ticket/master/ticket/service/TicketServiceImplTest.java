package com.ticket.master.ticket.service;

import com.ticket.master.common.enums.TicketType;
import com.ticket.master.common.exception.NullableViolation;
import com.ticket.master.common.exception.PostgresErrorCodes;
import com.ticket.master.common.exception.UniquenessViolation;
import com.ticket.master.common.kafka.*;
import com.ticket.master.common.scheme.TicketCategory;
import com.ticket.master.ticket.dto.TicketDTO;
import com.ticket.master.ticket.dto.kafka.BuyTicketRequest;
import com.ticket.master.ticket.enums.Status;
import com.ticket.master.ticket.kafka.TicketKafkaProducer;
import com.ticket.master.ticket.mapper.TicketMapper;
import com.ticket.master.ticket.model.Ticket;
import com.ticket.master.ticket.repository.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository repository;

    @Mock
    private TicketKafkaProducer ticketKafkaProducer;

    @Spy
    private TicketMapper mapper = new TicketMapper();

    @InjectMocks
    private TicketServiceImpl service;

    private UUID ticketId;
    private UUID eventId;
    private UUID userId;
    private UUID orderId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setEventId(eventId);
        ticket.setPrice(100.0);
        ticket.setType(TicketType.STANDARD);
        ticket.setSeat("1");
        ticket.setStatus(Status.AVAILABLE);
    }

    @Nested
    class GenerateTicketsFromSchema {
        @Test
        void shouldGenerateAndSaveTickets() {
            List<TicketCategory> categories = List.of(
                    new TicketCategory(TicketType.STANDARD, 2, 100.0),
                    new TicketCategory(TicketType.VIP, 1, 300.0)
            );
            EventCreatedMessage message = new EventCreatedMessage(eventId, categories);

            service.generateTicketsFromSchema(message);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Ticket>> captor = ArgumentCaptor.forClass(List.class);
            verify(repository).saveAll(captor.capture());

            List<Ticket> savedTickets = captor.getValue();
            assertThat(savedTickets).hasSize(3);
            assertThat(savedTickets).anyMatch(t -> t.getType() == TicketType.VIP && t.getPrice() == 300.0);
            assertThat(savedTickets).anyMatch(t -> t.getType() == TicketType.STANDARD && t.getSeat().equals("2"));
        }
    }

    @Nested
    class SaveTicket {
        @Test
        void shouldSaveTicketSuccessfully() {
            when(repository.save(any(Ticket.class))).thenReturn(ticket);

            Ticket saved = service.saveTicket(ticket);

            assertThat(saved).isNotNull();
            verify(repository).save(ticket);
        }

        @Test
        void shouldThrowUniquenessViolationWhenUniqueConstraintFails() {
            SQLException sqlEx = mock(SQLException.class);
            when(sqlEx.getSQLState()).thenReturn(PostgresErrorCodes.UNIQUE_VIOLATION);
            ConstraintViolationException cveMock = new ConstraintViolationException("msg", sqlEx, "constraint_name");
            DataIntegrityViolationException diveMock = new DataIntegrityViolationException("msg", cveMock);

            when(repository.save(any(Ticket.class))).thenThrow(diveMock);

            assertThrows(UniquenessViolation.class, () -> service.saveTicket(ticket));
        }

        @Test
        void shouldThrowNullableViolationWhenNotNullConstraintFails() {
            SQLException sqlEx = mock(SQLException.class);
            when(sqlEx.getSQLState()).thenReturn(PostgresErrorCodes.NOT_NULL_VIOLATION);
            ConstraintViolationException cveMock = new ConstraintViolationException("msg", sqlEx, "constraint_name");
            DataIntegrityViolationException diveMock = new DataIntegrityViolationException("msg", cveMock);

            when(repository.save(any(Ticket.class))).thenThrow(diveMock);

            assertThrows(NullableViolation.class, () -> service.saveTicket(ticket));
        }
    }

    @Nested
    class BuyTicket {
        @Test
        void shouldBuyTicketSuccessfully() {
            BuyTicketRequest request = new BuyTicketRequest(ticketId, userId);
            when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(repository.save(any(Ticket.class))).thenReturn(ticket);

            TicketDTO result = service.buyTicket(request, ticketId);

            assertThat(result).isNotNull();
            assertThat(ticket.getStatus()).isEqualTo(Status.SOLD);
            assertThat(ticket.getUserId()).isEqualTo(userId);
        }

        @Test
        void shouldThrowExceptionWhenTicketAlreadySold() {
            ticket.setUserId(UUID.randomUUID());
            BuyTicketRequest request = new BuyTicketRequest(ticketId, userId);
            when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));

            assertThrows(IllegalStateException.class, () -> service.buyTicket(request, ticketId));
        }

        @Test
        void shouldThrowExceptionWhenTicketNotFound() {
            BuyTicketRequest request = new BuyTicketRequest(ticketId, userId);
            when(repository.findById(ticketId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.buyTicket(request, ticketId));
        }
    }

    @Nested
    class MarkTicketsAsSold {
        @Test
        void shouldMarkTicketsAsSold() {
            List<UUID> ticketIds = List.of(ticketId);
            when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));

            service.markTicketsAsSold(ticketIds, userId);

            assertThat(ticket.getStatus()).isEqualTo(Status.SOLD);
            assertThat(ticket.getUserId()).isEqualTo(userId);
            verify(repository).save(ticket);
        }

        @Test
        void shouldThrowExceptionWhenAlreadySold() {
            ticket.setStatus(Status.SOLD);
            List<UUID> ticketIds = List.of(ticketId);
            when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));

            assertThrows(IllegalStateException.class, () -> service.markTicketsAsSold(ticketIds, userId));
        }
    }

    @Nested
    class UnlockTicket {
        @Test
        void shouldUnlockTicket() {
            ticket.setStatus(Status.LOCKED);
            ticket.setUserId(userId);
            when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));

            service.unlockTicket(ticketId);

            assertThat(ticket.getStatus()).isEqualTo(Status.AVAILABLE);
            assertThat(ticket.getUserId()).isNull();
            verify(repository).save(ticket);
        }
    }

    @Nested
    class DeleteTicketsByEventId {
        @Test
        void shouldDeleteTickets() {
            service.deleteTicketsByEventId(eventId);
            verify(repository).deleteByEventId(eventId);
        }
    }

    @Nested
    class ReserveTickets {
        @Test
        void shouldReserveTicketsSuccessfully() {
            ReserveTicketsMessage message = new ReserveTicketsMessage(orderId, eventId, userId, List.of(ticketId));
            when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));

            service.reserveTickets(message);

            assertThat(ticket.getStatus()).isEqualTo(Status.LOCKED);
            verify(repository).save(ticket);
            verify(ticketKafkaProducer).sendTicketsReservedMessage(any(TicketsReservedMessage.class));
        }

        @Test
        void shouldFailReservationWhenTicketUnavailable() {
            ticket.setStatus(Status.SOLD);
            ReserveTicketsMessage message = new ReserveTicketsMessage(orderId, eventId, userId, List.of(ticketId));
            when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));

            service.reserveTickets(message);

            verify(ticketKafkaProducer).sendTicketsReservationFailedMessage(any(TicketsReservationFailedMessage.class));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class CancelReservation {
        @Test
        void shouldCancelReservation() {
            ticket.setStatus(Status.LOCKED);
            ticket.setUserId(userId);
            CancelTicketsReservationMessage message = new CancelTicketsReservationMessage(orderId, List.of(ticketId));
            when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));

            service.cancelReservation(message);

            assertThat(ticket.getStatus()).isEqualTo(Status.AVAILABLE);
            assertThat(ticket.getUserId()).isNull();
            verify(repository).save(ticket);
        }
    }

    @Nested
    class ConfirmTicketsSold {
        @Test
        void shouldConfirmSold() {
            ticket.setStatus(Status.LOCKED);
            ConfirmTicketsSoldMessage message = new ConfirmTicketsSoldMessage(orderId, userId, List.of(ticketId));
            when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));

            service.confirmTicketsSold(message);

            assertThat(ticket.getStatus()).isEqualTo(Status.SOLD);
            assertThat(ticket.getUserId()).isEqualTo(userId);
            verify(repository).save(ticket);
        }
    }
}
