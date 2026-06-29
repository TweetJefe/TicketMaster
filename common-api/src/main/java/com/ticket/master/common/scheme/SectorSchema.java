package com.ticket.master.common.scheme;

import com.ticket.master.common.enums.TicketType;

public record SectorSchema(
        String sectorName,
        TicketType type,
        double price,
        int rowCount,
        int seatsPerRow
) {}