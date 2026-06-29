package com.ticket.master.event.controller;

import com.ticket.master.event.dto.PerformerRequest;
import com.ticket.master.event.dto.PerformerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ticket.master.event.service.PerformerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/performers")
@RequiredArgsConstructor
public class PerformerController {

    private final PerformerService service;

    @PostMapping
    public ResponseEntity<PerformerResponse> createPerformer(@Valid @RequestBody PerformerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPerformer(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerformerResponse> getPerformer(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getPerformerById(id));
    }

    @GetMapping
    public ResponseEntity<List<PerformerResponse>> getAllPerformers() {
        return ResponseEntity.ok(service.getAllPerformers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PerformerResponse> updatePerformer(@PathVariable UUID id, @Valid @RequestBody PerformerRequest request) {
        return ResponseEntity.ok(service.updatePerformer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerformer(@PathVariable UUID id) {
        service.deletePerformer(id);
        return ResponseEntity.noContent().build();
    }
}
