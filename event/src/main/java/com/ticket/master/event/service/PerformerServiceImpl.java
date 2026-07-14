package com.ticket.master.event.service;

import com.ticket.master.event.dto.PerformerRequest;
import com.ticket.master.event.dto.PerformerResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import com.ticket.master.event.mapper.PerformerMapper;
import com.ticket.master.event.model.Event;
import com.ticket.master.event.model.Performer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticket.master.event.repository.PerformerRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformerServiceImpl implements PerformerService {

    private final PerformerRepository repository;
    private final PerformerMapper mapper;

    @Override
    @Transactional
    public PerformerResponse createPerformer(PerformerRequest request) {
        Performer performer = mapper.toEntity(request);
        Performer savedPerformer = repository.save(performer);
        return mapper.toDto(savedPerformer);
    }

    @Override
    @Transactional(readOnly = true)
    public PerformerResponse getPerformerById(UUID id) {
        Performer performer = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Performer with ID " + id + " not found"));
        return mapper.toDto(performer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerformerResponse> getAllPerformers() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PerformerResponse updatePerformer(UUID id, PerformerRequest request) {
        Performer performer = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Performer with ID " + id + " not found"));

        performer.setName(request.name());
        performer.setGenre(request.genre());
        performer.setDescription(request.description());

        Performer updatedPerformer = repository.save(performer);
        return mapper.toDto(updatedPerformer);
    }

    @Override
    @Transactional
    public void deletePerformer(UUID id) {
        Performer performer = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Performer with ID " + id + " not found"));

        for (Event event : performer.getEvents()) {
            event.getPerformers().remove(performer);
        }
        repository.delete(performer);
    }
}
