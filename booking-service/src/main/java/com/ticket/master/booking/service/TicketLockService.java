package com.ticket.master.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketLockService {
    private final StringRedisTemplate redisTemplate;

    private static final Duration LOCK_TTL = Duration.ofMinutes(15);
    private static final String LOCK_PREFIX = "ticket:lock:";


    public boolean lockTicket (UUID ticketId, UUID userId){
        String key = LOCK_PREFIX + ticketId;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, userId.toString(), LOCK_TTL);

        return Boolean.TRUE.equals(acquired);
    }

    public void unlockTicket (UUID ticketId, UUID userId){
        String key = LOCK_PREFIX + ticketId;
        String owner = redisTemplate.opsForValue().get(key);

        if (userId.toString().equals(owner)){
            redisTemplate.delete(key);
        }
    }
}
