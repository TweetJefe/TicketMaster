package com.ticket.master.event.controller;

import com.ticket.master.event.dto.HallRequest;
import com.ticket.master.event.dto.HallResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ticket.master.event.service.HallService;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/halls")
@RequiredArgsConstructor
public class HallController {

    private final HallService service;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<HallResponse> createHall(@Valid @RequestBody HallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createHall(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HallResponse> getHall(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getHallById(id));
    }

    @GetMapping
    public ResponseEntity<List<HallResponse>> getAllHalls() {
        return ResponseEntity.ok(service.getAllHalls());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<HallResponse> updateHall(@PathVariable UUID id, @Valid @RequestBody HallRequest request) {
        return ResponseEntity.ok(service.updateHall(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHall(@PathVariable UUID id) {
        service.deleteHall(id);
        return ResponseEntity.noContent().build();
    }
}
