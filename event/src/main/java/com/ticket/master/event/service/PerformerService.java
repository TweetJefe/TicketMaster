package com.ticket.master.event.service;

import com.ticket.master.event.dto.PerformerRequest;
import com.ticket.master.event.dto.PerformerResponse;

import java.util.List;
import java.util.UUID;

public interface PerformerService {
    PerformerResponse createPerformer(PerformerRequest request);

    PerformerResponse getPerformerById(UUID id);

    List<PerformerResponse> getAllPerformers();

    PerformerResponse updatePerformer(UUID id, PerformerRequest request);

    void deletePerformer(UUID id);
}
