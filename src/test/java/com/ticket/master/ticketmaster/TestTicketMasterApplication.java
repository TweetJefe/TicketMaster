package com.ticket.master.ticketmaster;

import org.springframework.boot.SpringApplication;

public class TestTicketMasterApplication {

    public static void main(String[] args) {
        SpringApplication.from(TicketMasterApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
