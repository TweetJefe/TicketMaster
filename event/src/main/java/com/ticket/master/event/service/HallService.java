package com.ticket.master.event.service;

import com.ticket.master.event.dto.HallRequest;
import com.ticket.master.event.dto.HallResponse;

import java.util.List;
import java.util.UUID;

public interface HallService {
    HallResponse createHall(HallRequest request);

    HallResponse getHallById(UUID id);

    List<HallResponse> getAllHalls();

    HallResponse updateHall(UUID id, HallRequest request);

    void deleteHall(UUID id);
}
