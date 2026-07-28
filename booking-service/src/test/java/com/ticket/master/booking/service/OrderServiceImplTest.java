package com.ticket.master.booking.service;

import com.ticket.master.booking.dto.OrderCreateRequest;
import com.ticket.master.booking.dto.OrderDTO;
import com.ticket.master.booking.kafka.BookingKafkaProducer;
import com.ticket.master.booking.mapper.OrderMapper;
import com.ticket.master.booking.model.Order;
import com.ticket.master.booking.model.OrderStatus;
import com.ticket.master.booking.repository.CartRepository;
import com.ticket.master.booking.repository.OrderRepository;
import com.ticket.master.common.kafka.CancelTicketsReservationMessage;
import com.ticket.master.common.kafka.ConfirmTicketsSoldMessage;
import com.ticket.master.common.kafka.ReserveTicketsMessage;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BookingKafkaProducer kafkaProducer;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private TicketLockService lockService;

    @Spy
    private OrderMapper mapper = new OrderMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    class createOrderTests {
        @Test
        public void createOrderSuccess() {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            OrderCreateRequest request = new OrderCreateRequest(
                    userId,
                    eventId,
                    BigDecimal.TEN,
                    List.of(ticketId)
            );

            Order savedOrder = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .eventId(eventId)
                    .totalAmount(BigDecimal.TEN)
                    .ticketIds(List.of(ticketId))
                    .status(OrderStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            OrderDTO result = orderService.createOrder(request);

            assertNotNull(result);
            assertEquals(orderId, result.id());
            assertEquals(userId, result.userId());
            assertEquals(eventId, result.eventId());
            assertEquals(OrderStatus.PENDING, result.status());

            verify(cartRepository).deleteById(userId);
            verify(redisTemplate).delete("cart:" + userId + ":shadow");
            verify(valueOperations).set(eq("order:" + orderId + ":shadow"), eq(""), any());

            ArgumentCaptor<ReserveTicketsMessage> messageCaptor = ArgumentCaptor.forClass(ReserveTicketsMessage.class);
            verify(kafkaProducer).sendReserveTicketMessage(messageCaptor.capture());
            ReserveTicketsMessage capturedMessage = messageCaptor.getValue();
            assertEquals(orderId, capturedMessage.orderId());
            assertEquals(eventId, capturedMessage.eventId());
            assertEquals(userId, capturedMessage.userId());
            assertEquals(List.of(ticketId), capturedMessage.ticketIds());
        }
    }

    @Nested
    class payOrderTests {
        @Test
        public void payOrderSuccess() {
            UUID orderId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            Order order = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .eventId(eventId)
                    .totalAmount(BigDecimal.TEN)
                    .ticketIds(List.of(ticketId))
                    .status(OrderStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderDTO result = orderService.payOrder(orderId);

            assertNotNull(result);
            assertEquals(orderId, result.id());
            assertEquals(OrderStatus.PAID, result.status());

            verify(redisTemplate).delete("order:" + orderId + ":shadow");
            verify(lockService).unlockTicket(ticketId, userId);

            ArgumentCaptor<ConfirmTicketsSoldMessage> messageCaptor = ArgumentCaptor.forClass(ConfirmTicketsSoldMessage.class);
            verify(kafkaProducer).sendConfirmTicketsSoldMessage(messageCaptor.capture());
            ConfirmTicketsSoldMessage capturedMessage = messageCaptor.getValue();
            assertEquals(orderId, capturedMessage.orderId());
            assertEquals(userId, capturedMessage.userId());
            assertEquals(List.of(ticketId), capturedMessage.ticketIds());
        }

        @Test
        public void payOrderNotFound() {
            UUID orderId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> orderService.payOrder(orderId));
        }
    }

    @Nested
    class cancelOrdersByEventIdTests {
        @Test
        public void cancelOrdersByEventIdSuccess() {
            UUID eventId = UUID.randomUUID();

            Order order1 = Order.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .status(OrderStatus.PENDING)
                    .build();

            Order order2 = Order.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .status(OrderStatus.PENDING)
                    .build();

            when(orderRepository.findAllByEventId(eventId)).thenReturn(List.of(order1, order2));

            orderService.cancelOrdersByEventId(eventId);

            assertEquals(OrderStatus.CANCELLED, order1.getStatus());
            assertEquals(OrderStatus.CANCELLED, order2.getStatus());
            verify(orderRepository).saveAll(List.of(order1, order2));
        }
    }

    @Nested
    class cancelOrderTests {
        @Test
        public void cancelOrderWithReleaseSuccess() {
            UUID orderId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            Order order = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .ticketIds(List.of(ticketId))
                    .status(OrderStatus.PENDING)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            orderService.cancelOrder(orderId, true);

            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            verify(orderRepository).save(order);
            verify(redisTemplate).delete("order:" + orderId + ":shadow");
            verify(lockService).unlockTicket(ticketId, userId);

            ArgumentCaptor<CancelTicketsReservationMessage> messageCaptor = ArgumentCaptor.forClass(CancelTicketsReservationMessage.class);
            verify(kafkaProducer).sendCancelTicketsReservationMessage(messageCaptor.capture());
            CancelTicketsReservationMessage capturedMessage = messageCaptor.getValue();
            assertEquals(orderId, capturedMessage.orderId());
            assertEquals(List.of(ticketId), capturedMessage.ticketIds());
        }

        @Test
        public void cancelOrderWithoutReleaseSuccess() {
            UUID orderId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            Order order = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .ticketIds(List.of(ticketId))
                    .status(OrderStatus.PENDING)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            orderService.cancelOrder(orderId, false);

            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            verify(orderRepository).save(order);
            verify(redisTemplate).delete("order:" + orderId + ":shadow");
            verify(lockService).unlockTicket(ticketId, userId);
            verify(kafkaProducer, never()).sendCancelTicketsReservationMessage(any());
        }

        @Test
        public void cancelOrderIgnoredWhenNotPending() {
            UUID orderId = UUID.randomUUID();

            Order order = Order.builder()
                    .id(orderId)
                    .status(OrderStatus.PAID)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            orderService.cancelOrder(orderId, true);

            assertEquals(OrderStatus.PAID, order.getStatus());
            verify(orderRepository, never()).save(any());
            verify(kafkaProducer, never()).sendCancelTicketsReservationMessage(any());
        }

        @Test
        public void cancelOrderIgnoredWhenNotFound() {
            UUID orderId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            orderService.cancelOrder(orderId, true);

            verify(orderRepository, never()).save(any());
        }
    }
}
