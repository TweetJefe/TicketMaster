package com.ticket.master.event.service;

import com.ticket.master.event.dto.HallRequest;
import com.ticket.master.event.dto.HallResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import com.ticket.master.event.mapper.HallMapper;
import com.ticket.master.event.model.Hall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticket.master.event.repository.HallRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HallServiceImpl implements HallService {

    private final HallRepository repository;
    private final HallMapper mapper;

    @Override
    @Transactional
    public HallResponse createHall(HallRequest request) {
        Hall hall = mapper.toEntity(request);
        Hall savedHall = repository.save(hall);
        return mapper.toDto(savedHall);
    }

    @Override
    @Transactional(readOnly = true)
    public HallResponse getHallById(UUID id) {
        Hall hall = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hall with ID " + id + " not found"));
        return mapper.toDto(hall);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HallResponse> getAllHalls() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HallResponse updateHall(UUID id, HallRequest request) {
        Hall hall = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hall with ID " + id + " not found"));

        hall.setName(request.name());
        hall.setCity(request.city());
        hall.setAddress(request.address());
        hall.setCapacity(request.capacity());

        Hall updatedHall = repository.save(hall);
        return mapper.toDto(updatedHall);
    }

    @Override
    @Transactional
    public void deleteHall(UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Hall with ID " + id + " not found");
        }
        repository.deleteById(id);
    }
}
