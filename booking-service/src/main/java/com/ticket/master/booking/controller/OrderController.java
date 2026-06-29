package com.ticket.master.booking.controller;

import com.ticket.master.booking.dto.OrderCreateRequest;
import com.ticket.master.booking.dto.OrderDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ticket.master.booking.service.OrderService;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<OrderDTO> payOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.payOrder(id));
    }
}
