package com.ticket.master.booking.redis;

import com.ticket.master.booking.kafka.BookingKafkaProducer;
import com.ticket.master.booking.service.OrderService;
import com.ticket.master.common.kafka.CartExpiredMessage;
import com.ticket.master.common.kafka.TicketUnlockMessage;
import lombok.extern.slf4j.Slf4j;
import com.ticket.master.booking.model.CartItem;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import com.ticket.master.booking.repository.CartRepository;
import com.ticket.master.booking.service.TicketLockService;
import com.ticket.master.booking.repository.OrderRepository;
import com.ticket.master.booking.model.OrderStatus;
import com.ticket.master.booking.model.Order;
import java.util.UUID;

@Slf4j
@Component
public class CartExpirationListener extends KeyExpirationEventMessageListener {
    private final BookingKafkaProducer kafkaProd;
    private final CartRepository cartRepository;
    private final TicketLockService lockService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderService orderService;

    public CartExpirationListener(
            RedisMessageListenerContainer listenerContainer,
            BookingKafkaProducer kafkaProd,
            CartRepository cartRepository,
            TicketLockService lockService,
            RedisTemplate<String, Object> redisTemplate,
            OrderService orderService
    ) {
        super(listenerContainer);
        this.kafkaProd = kafkaProd;
        this.cartRepository = cartRepository;
        this.lockService = lockService;
        this.redisTemplate = redisTemplate;
        this.orderService = orderService;
    }

    @Override
    public void onMessage (Message message, byte[] pattern){
        String expiredKey = message.toString();

        if(expiredKey.startsWith("cart:") && expiredKey.endsWith(":shadow")){
            try{
                String userIdString = expiredKey.substring(5, expiredKey.length() - 7);
                UUID userId = UUID.fromString(userIdString);

                log.info("Time is up! Cart for user ({}) expired. Processing cleanup...", userId);

                cartRepository.findByUserId(userId).ifPresent(
                        cart -> {
                            if(cart.getItems() != null && !cart.getItems().isEmpty()){
                                for (CartItem item : cart.getItems()){
                                    lockService.unlockTicket(item.getTicketId(), userId);
                                    kafkaProd.sendTicketUnlockMessage(new TicketUnlockMessage(item.getTicketId(), item.getEventId()));
                                    String eventIndexKey = "event:" + item.getEventId() + ":carts";
                                    redisTemplate.opsForSet().remove(eventIndexKey, userId.toString());
                                }
                                log.info("Released {} locks for expired cart of user {}", cart.getItems().size(), userId);
                            }
                            cartRepository.delete(cart);
                        });
            }catch (Exception e){
                log.error("Error cleaning up expired cart for key {}: {}", expiredKey, e.getMessage(), e);
            }
        } else if (expiredKey.startsWith("order:") && expiredKey.endsWith(":shadow")) {
            try {
                String orderIdString = expiredKey.substring(6, expiredKey.length() - 7);
                UUID orderId = UUID.fromString(orderIdString);

                log.info("Payment window is up! Checking order ({}) for expiration...", orderId);

                    orderService.cancelOrder(orderId, true);
            } catch (Exception e) {
                log.error("Error cleaning up expired order for key {}: {}", expiredKey, e.getMessage(), e);
            }
        }
    }
}
