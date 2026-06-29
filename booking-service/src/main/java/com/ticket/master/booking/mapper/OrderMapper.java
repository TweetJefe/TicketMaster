package com.ticket.master.booking.mapper;

import com.ticket.master.booking.dto.OrderDTO;
import com.ticket.master.booking.model.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderDTO toDto(Order order) {
        if (order == null) return null;
        return new OrderDTO(
                order.getId(),
                order.getUserId(),
                order.getEventId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
