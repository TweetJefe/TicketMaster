--liquibase formatted sql
--changeset Aleksandr:init-changeset

CREATE TABLE tickets (
    id UUID NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    event_id UUID NOT NULL,
    user_id UUID,
    sector VARCHAR(255),
    row VARCHAR(255),
    seat VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    CONSTRAINT pk_tickets PRIMARY KEY (id)
);
