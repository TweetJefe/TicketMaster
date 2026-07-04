package com.ticket.master.booking.service;

import com.ticket.master.booking.dto.OrderCreateRequest;
import com.ticket.master.booking.dto.OrderDTO;
import com.ticket.master.booking.model.OrderStatus;

import java.util.UUID;

public interface OrderService {
    OrderDTO createOrder(OrderCreateRequest request);

    OrderDTO payOrder(UUID id);

    void cancelOrdersByEventId(UUID EventId);

    void cancelOrder(UUID id, boolean releaseDatabaseReservation);
}
