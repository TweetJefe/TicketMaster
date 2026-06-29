package com.ticket.master.booking.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("cart")
public class Cart {
    @Id
    private UUID userId;
    private List<CartItem> items;
    @TimeToLive(unit = TimeUnit.MILLISECONDS)
    private Long ttlInSeconds;
}
