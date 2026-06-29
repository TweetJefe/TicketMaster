package com.ticket.master.booking.redis;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import com.ticket.master.booking.kafka.BookingKafkaProducer;
import com.ticket.master.common.kafka.TicketUnlockMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ticket.master.booking.model.Cart;
import com.ticket.master.booking.model.CartItem;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticket.master.booking.repository.CartRepository;
import com.ticket.master.booking.service.TicketLockService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Long CART_TTL = 900L;
    private final BookingKafkaProducer kafkaProd;
    private  final TicketLockService lockService;



    public void addItemToCart(UUID userId, List<CartItem> items){
        if (items == null || items.isEmpty()){
            return;
        }

        List<UUID> successfullyLocked = new ArrayList<>();

        for (CartItem item : items){
            boolean isLocked = lockService.lockTicket(item.getTicketId(), userId);

            if (!isLocked){
                for (UUID lockedId : successfullyLocked){
                    lockService.unlockTicket(lockedId, userId);
                }

                throw new IllegalStateException("Ticket " + item.getTicketId() + " is already booked");
            }
            successfullyLocked.add(item.getTicketId());
        }

        Cart cart = cartRepository.findByUserId(userId).orElse(
                Cart.builder()
                        .userId(userId)
                        .items(new ArrayList<>())
                        .build()
        );

        cart.getItems().addAll(items);
        cart.setTtlInSeconds(86400L);
        cartRepository.save(cart);

        for (CartItem item : items){
            String eventIndexKey = "event:" + item.getEventId() + ":carts";
            redisTemplate.opsForSet().add(eventIndexKey, userId.toString());
            redisTemplate.expire(eventIndexKey, Duration.ofSeconds(CART_TTL));
        }
        String shadowKey = "cart:" + userId + ":shadow";
        redisTemplate.opsForValue().set(shadowKey, "", Duration.ofSeconds(CART_TTL));

        log.info("{} tickets were added to user's {} cart. Booking lasts 15 minutes", items.size(), userId);
    }

    @Transactional
    public void removeItemFromCart(UUID userId, List<CartItem> items){
        if (items == null || items.isEmpty()){
            return;
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart doesn't exist"));

        for (CartItem item : items){
            lockService.unlockTicket(item.getTicketId(), userId);
            kafkaProd.sendTicketUnlockMessage(new TicketUnlockMessage(item.getTicketId(), item.getEventId()));
        }

        List<UUID> ticketIdsToRemove = items.stream()
                .map(CartItem::getTicketId)
                .toList();

        cart.getItems().removeIf(item -> ticketIdsToRemove.contains(item.getTicketId()));

        if (cart.getItems().isEmpty()){
            cartRepository.delete(cart);

            log.info("Cart for user {} is completely empty and was deleted", userId);
        }else{
            cart.setTtlInSeconds(86400L);
            cartRepository.save(cart);
            log.info("{} tickets were removed from user's {} cart", items.size(), userId);
        }
    }

    public void removeEventFromAllCarts(UUID eventId){
        String eventIndexKey = "event:" + eventId + ":carts";

        Set<Object> userIds = redisTemplate.opsForSet().members(eventIndexKey);

        if (userIds == null || userIds.isEmpty()){
            log.info("No one had tickets for cancelled event");
            return;
        }

        for (Object idObj : userIds){
            UUID userId = UUID.fromString(idObj.toString());
            cartRepository.findByUserId(userId).ifPresent(cart -> {
                List<CartItem> itemsToRemove = cart.getItems().stream()
                        .filter(item -> item.getEventId().equals(eventId)).toList();
                for (CartItem item : itemsToRemove){
                    lockService.unlockTicket(item.getTicketId(), userId);
                    kafkaProd.sendTicketUnlockMessage(new TicketUnlockMessage(item.getTicketId(), item.getEventId()));
                }
                cart.getItems().removeIf(item -> item.getEventId().equals(eventId));

                if(cart.getItems().isEmpty()){
                    cartRepository.delete(cart);
                }else{
                    cart.setTtlInSeconds(86400L);
                    cartRepository.save(cart);
                }
            });
        }
        redisTemplate.delete(eventIndexKey);
        log.info("Tickets for cancelled event {} were removed from user's cart", eventId);
    }

    public void unlockItemFromCart(UUID ticketId, UUID userId, UUID eventId){
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            boolean isRemoved = cart.getItems().removeIf(item -> item.getTicketId().equals(ticketId));

            if (isRemoved){
                if(cart.getItems().isEmpty()){
                    cartRepository.deleteById(userId);
                }else{
                    cartRepository.save(cart);
                }
            }

            boolean stillHasTicketsForThisEvent = cart.getItems().stream()
                    .anyMatch(item -> item.getEventId().equals(eventId));

            if(!stillHasTicketsForThisEvent){
                String eventIndexKey = "event:" + eventId + ":carts";
                redisTemplate.opsForSet().remove(eventIndexKey, userId.toString());
            }

            lockService.unlockTicket(ticketId, userId);
            kafkaProd.sendTicketUnlockMessage(new TicketUnlockMessage(ticketId, eventId));
            log.info("Ticket {} successfully were removed from cart {}", ticketId, userId);
        });
    }

}
