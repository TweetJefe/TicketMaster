package com.ticket.master.event.mapper;

import com.ticket.master.event.dto.CategoryResponseDTO;
import com.ticket.master.event.dto.EventDTO;
import com.ticket.master.event.dto.HallResponse;
import com.ticket.master.event.dto.PerformerResponse;
import lombok.RequiredArgsConstructor;
import com.ticket.master.event.model.Event;
import com.ticket.master.event.model.EventCategory;
import com.ticket.master.event.model.Hall;
import com.ticket.master.event.model.Performer;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final HallMapper hallMapper;
    private final PerformerMapper performerMapper;

    public EventDTO toDto (Event event){
        if (event == null) return null;

        return new EventDTO(
                event.getId(),
                event.getName(),
                event.getAddress(),
                event.getCity(),
                event.getTime(),
                toHallResponse(event.getHall()),
                toPerformerResponses(event.getPerformers()),
                toCategoryResponses(event.getCategories())
        );
    }

    private HallResponse toHallResponse(Hall hall) {
        if (hall == null) return null;
        return hallMapper.toDto(hall);
    }

    private Set<PerformerResponse> toPerformerResponses(Set<Performer> performers) {
        if (performers == null) return Set.of();
        return performers.stream()
                .map(performerMapper::toDto)
                .collect(Collectors.toSet());
    }

    private Set<CategoryResponseDTO> toCategoryResponses(Set<EventCategory> categories) {
        if (categories == null) return Set.of();
        return categories.stream()
                .map(cat -> new CategoryResponseDTO(
                        cat.getId(),
                        cat.getType(),
                        cat.getQuantity(),
                        cat.getPrice()
                ))
                .collect(Collectors.toSet());
    }
}
