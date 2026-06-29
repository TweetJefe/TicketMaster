package com.ticket.master.booking.service;

import com.ticket.master.booking.dto.OrderCreateRequest;
import com.ticket.master.booking.dto.OrderDTO;
import jakarta.persistence.EntityNotFoundException;
import com.ticket.master.booking.kafka.BookingKafkaProducer;
import com.ticket.master.common.kafka.OrderPaidMessage;
import lombok.RequiredArgsConstructor;
import com.ticket.master.booking.mapper.OrderMapper;
import com.ticket.master.booking.model.Order;
import com.ticket.master.booking.model.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticket.master.booking.repository.OrderRepository;
import com.ticket.master.booking.repository.CartRepository;
import org.springframework.data.redis.core.RedisTemplate;
import com.ticket.master.booking.service.TicketLockService;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final BookingKafkaProducer kafkaProducer;
    private final CartRepository cartRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TicketLockService lockService;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderCreateRequest request) {
        Order order = Order.builder()
                .userId(request.userId())
                .eventId(request.eventId())
                .totalAmount(request.totalAmount())
                .ticketIds(request.ticketIds())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        Order savedOrder = repository.save(order);

        cartRepository.deleteById(request.userId());
        redisTemplate.delete("cart:" + request.userId() + ":shadow");

        redisTemplate.opsForValue().set("order:" + savedOrder.getId() + ":shadow", "", Duration.ofMinutes(10));

        return mapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO payOrder(UUID id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        order.setStatus(OrderStatus.PAID);
        Order savedOrder = repository.save(order);

        redisTemplate.delete("order:" + savedOrder.getId() + ":shadow");

        if (savedOrder.getTicketIds() != null) {
            for (UUID ticketId : savedOrder.getTicketIds()) {
                lockService.unlockTicket(ticketId, savedOrder.getUserId());
            }
        }

        List<UUID> purchaseTickets = savedOrder.getTicketIds();

        OrderPaidMessage message = new OrderPaidMessage(
                savedOrder.getId(),
                savedOrder.getUserId(),
                purchaseTickets
        );

        kafkaProducer.sendOrderPaidMessage(message);
        return mapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public void cancelOrdersByEventId(UUID eventId) {
        List<Order> orders = repository.findAllByEventId(eventId);

        for (Order order : orders){
            order.setStatus(OrderStatus.CANCELLED);
        }
        repository.saveAll(orders);
    }
}
