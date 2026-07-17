package com.ticket.master.ticket.controller;

import com.ticket.master.ticket.dto.kafka.BuyTicketRequest;
import jakarta.validation.Valid;
import com.ticket.master.ticket.dto.CreateTicketDTO;
import com.ticket.master.ticket.dto.TicketDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ticket.master.ticket.service.TicketService;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService service;

    @PostMapping("/{id}/buy")
    public ResponseEntity<TicketDTO> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody BuyTicketRequest dto
    ){
        TicketDTO boughtTicket = service.buyTicket(dto, id);
        return ResponseEntity.status(HttpStatus.OK).body(boughtTicket);
    }
}
