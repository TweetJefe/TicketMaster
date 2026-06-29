package com.ticket.master.event.mapper;

import com.ticket.master.event.dto.HallRequest;
import com.ticket.master.event.dto.HallResponse;
import com.ticket.master.event.model.Hall;
import org.springframework.stereotype.Component;

@Component
public class HallMapper {

    public Hall toEntity(HallRequest request) {
        if (request == null) return null;

        return Hall.builder()
                .name(request.name())
                .city(request.city())
                .address(request.address())
                .capacity(request.capacity())
                .build();
    }

    public HallResponse toDto(Hall hall) {
        if (hall == null) return null;

        return new HallResponse(
                hall.getId(),
                hall.getName(),
                hall.getCity(),
                hall.getAddress(),
                hall.getCapacity()
        );
    }
}
