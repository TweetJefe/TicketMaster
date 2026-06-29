package com.ticket.master.common.kafka;

import java.util.UUID;

public record CartExpiredMessage(
        UUID userId
) {}
