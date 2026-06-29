package com.ticket.master.event.mapper;

import com.ticket.master.event.dto.PerformerRequest;
import com.ticket.master.event.dto.PerformerResponse;
import com.ticket.master.event.model.Performer;
import org.springframework.stereotype.Component;

@Component
public class PerformerMapper {

    public Performer toEntity(PerformerRequest request) {
        if (request == null) return null;

        return Performer.builder()
                .name(request.name())
                .genre(request.genre())
                .description(request.description())
                .build();
    }

    public PerformerResponse toDto(Performer performer) {
        if (performer == null) return null;

        return new PerformerResponse(
                performer.getId(),
                performer.getName(),
                performer.getGenre(),
                performer.getDescription()
        );
    }
}
