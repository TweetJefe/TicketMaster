package com.ticket.master.booking.redis;

import com.ticket.master.booking.kafka.BookingKafkaProducer;
import com.ticket.master.booking.model.Cart;
import com.ticket.master.booking.model.CartItem;
import com.ticket.master.booking.repository.CartRepository;
import com.ticket.master.booking.service.TicketLockService;
import com.ticket.master.common.kafka.TicketUnlockMessage;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private BookingKafkaProducer kafkaProd;

    @Mock
    private TicketLockService lockService;

    @InjectMocks
    private CartService cartService;

    @Nested
    class addItemToCartTests {
        @Test
        public void addItemToCartSuccess() {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            CartItem item = CartItem.builder()
                    .eventId(eventId)
                    .ticketId(ticketId)
                    .price(BigDecimal.TEN)
                    .build();

            when(lockService.lockTicket(ticketId, userId)).thenReturn(true);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            cartService.addItemToCart(userId, List.of(item));

            verify(cartRepository).save(any(Cart.class));
            verify(setOperations).add(eq("event:" + eventId + ":carts"), eq(userId.toString()));
            verify(valueOperations).set(eq("cart:" + userId + ":shadow"), eq(""), any());
        }

        @Test
        public void addItemToCartLockFailed() {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId1 = UUID.randomUUID();
            UUID ticketId2 = UUID.randomUUID();

            CartItem item1 = CartItem.builder().eventId(eventId).ticketId(ticketId1).price(BigDecimal.TEN).build();
            CartItem item2 = CartItem.builder().eventId(eventId).ticketId(ticketId2).price(BigDecimal.TEN).build();

            when(lockService.lockTicket(ticketId1, userId)).thenReturn(true);
            when(lockService.lockTicket(ticketId2, userId)).thenReturn(false);

            assertThrows(IllegalStateException.class, () -> cartService.addItemToCart(userId, List.of(item1, item2)));

            verify(lockService).unlockTicket(ticketId1, userId);
            verify(cartRepository, never()).save(any());
        }

        @Test
        public void addItemToCartNullOrEmpty() {
            UUID userId = UUID.randomUUID();

            cartService.addItemToCart(userId, null);
            cartService.addItemToCart(userId, Collections.emptyList());

            verify(lockService, never()).lockTicket(any(), any());
            verify(cartRepository, never()).save(any());
        }
    }

    @Nested
    class removeItemFromCartTests {
        @Test
        public void removeItemFromCartSuccessAndCartNotEmpty() {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId1 = UUID.randomUUID();
            UUID ticketId2 = UUID.randomUUID();

            CartItem item1 = CartItem.builder().eventId(eventId).ticketId(ticketId1).price(BigDecimal.TEN).build();
            CartItem item2 = CartItem.builder().eventId(eventId).ticketId(ticketId2).price(BigDecimal.TEN).build();

            Cart cart = Cart.builder()
                    .userId(userId)
                    .items(new ArrayList<>(List.of(item1, item2)))
                    .build();

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            cartService.removeItemFromCart(userId, List.of(item1));

            verify(lockService).unlockTicket(ticketId1, userId);
            verify(kafkaProd).sendTicketUnlockMessage(any(TicketUnlockMessage.class));
            verify(cartRepository).save(cart);
            assertEquals(1, cart.getItems().size());
        }

        @Test
        public void removeItemFromCartSuccessAndCartBecomesEmpty() {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            CartItem item = CartItem.builder().eventId(eventId).ticketId(ticketId).price(BigDecimal.TEN).build();

            Cart cart = Cart.builder()
                    .userId(userId)
                    .items(new ArrayList<>(List.of(item)))
                    .build();

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            cartService.removeItemFromCart(userId, List.of(item));

            verify(lockService).unlockTicket(ticketId, userId);
            verify(kafkaProd).sendTicketUnlockMessage(any(TicketUnlockMessage.class));
            verify(cartRepository).delete(cart);
        }

        @Test
        public void removeItemFromCartNotFound() {
            UUID userId = UUID.randomUUID();
            CartItem item = CartItem.builder().eventId(UUID.randomUUID()).ticketId(UUID.randomUUID()).build();

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cartService.removeItemFromCart(userId, List.of(item)));
        }

        @Test
        public void removeItemFromCartNullOrEmpty() {
            UUID userId = UUID.randomUUID();

            cartService.removeItemFromCart(userId, null);
            cartService.removeItemFromCart(userId, Collections.emptyList());

            verify(cartRepository, never()).findByUserId(any());
        }
    }

    @Nested
    class removeEventFromAllCartsTests {
        @Test
        public void removeEventFromAllCartsSuccess() {
            UUID eventId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            CartItem item = CartItem.builder().eventId(eventId).ticketId(ticketId).price(BigDecimal.TEN).build();
            Cart cart = Cart.builder().userId(userId).items(new ArrayList<>(List.of(item))).build();

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.members("event:" + eventId + ":carts")).thenReturn(Set.of(userId.toString()));
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            cartService.removeEventFromAllCarts(eventId);

            verify(lockService).unlockTicket(ticketId, userId);
            verify(kafkaProd).sendTicketUnlockMessage(any(TicketUnlockMessage.class));
            verify(cartRepository).delete(cart);
            verify(redisTemplate).delete("event:" + eventId + ":carts");
        }

        @Test
        public void removeEventFromAllCartsNoUsersInSet() {
            UUID eventId = UUID.randomUUID();

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.members("event:" + eventId + ":carts")).thenReturn(Collections.emptySet());

            cartService.removeEventFromAllCarts(eventId);

            verify(cartRepository, never()).findByUserId(any());
        }
    }

    @Nested
    class unlockItemFromCartTests {
        @Test
        public void unlockItemFromCartSuccess() {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            CartItem item = CartItem.builder().eventId(eventId).ticketId(ticketId).price(BigDecimal.TEN).build();
            Cart cart = Cart.builder().userId(userId).items(new ArrayList<>(List.of(item))).build();

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            cartService.unlockItemFromCart(ticketId, userId, eventId);

            verify(cartRepository).deleteById(userId);
            verify(setOperations).remove("event:" + eventId + ":carts", userId.toString());
            verify(lockService).unlockTicket(ticketId, userId);
            verify(kafkaProd).sendTicketUnlockMessage(any(TicketUnlockMessage.class));
        }

        @Test
        public void unlockItemFromCartWhenCartNotFound() {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ticketId = UUID.randomUUID();

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

            cartService.unlockItemFromCart(ticketId, userId, eventId);

            verify(lockService, never()).unlockTicket(any(), any());
            verify(kafkaProd, never()).sendTicketUnlockMessage(any());
        }
    }
}
