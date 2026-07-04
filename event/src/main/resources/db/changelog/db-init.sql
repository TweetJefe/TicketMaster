--liquibase formatted sql
--changeset Aleksandr:init-changeset

CREATE TABLE halls (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    CONSTRAINT pk_halls PRIMARY KEY (id)
);

CREATE TABLE performers (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    genre VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT pk_performers PRIMARY KEY (id)
);

CREATE TABLE events (
    id UUID NOT NULL,
    name VARCHAR(255),
    address VARCHAR(255),
    city VARCHAR(255),
    time TIMESTAMP,
    hall_id UUID NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT FK_EVENTS_ON_HALL FOREIGN KEY (hall_id) REFERENCES halls(id)
);

CREATE TABLE event_categories (
    id UUID NOT NULL,
    event_id UUID NOT NULL,
    type VARCHAR(255),
    price DOUBLE PRECISION NOT NULL,
    quantity INT,
    CONSTRAINT pk_event_categories PRIMARY KEY (id),
    CONSTRAINT FK_EVENT_CATEGORIES_ON_EVENT FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE TABLE event_performers (
    event_id UUID NOT NULL,
    performer_id UUID NOT NULL,
    CONSTRAINT pk_event_performers PRIMARY KEY (event_id, performer_id),
    CONSTRAINT fk_eveper_on_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_eveper_on_performer FOREIGN KEY (performer_id) REFERENCES performers(id)
);

