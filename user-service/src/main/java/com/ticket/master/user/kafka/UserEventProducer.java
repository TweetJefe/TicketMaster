package com.ticket.master.user.kafka;


import com.ticket.master.user.dto.UserRegisteredEvent;

public interface UserEventProducer {
    void sendRegistrationEvent(UserRegisteredEvent event);
}