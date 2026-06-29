package com.ticket.master.booking.controller;

import lombok.RequiredArgsConstructor;
import com.ticket.master.booking.model.CartItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ticket.master.booking.redis.CartService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/{userId}/add")
    public ResponseEntity<Void> addItemsToCart(
            @PathVariable UUID userId,
            @RequestBody List<CartItem> items) {

        cartService.addItemToCart(userId, items);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/events/{eventId}/tickets/{ticketId}")
    public ResponseEntity<Void> removeItemFromCart(
            @PathVariable UUID userId,
            @PathVariable UUID eventId,
            @PathVariable UUID ticketId) {

        cartService.unlockItemFromCart(ticketId, userId, eventId);
        return ResponseEntity.noContent().build();
    }
}