--liquibase formatted sql
--changeset Aleksandr:init-changeset

CREATE TABLE orders (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    event_id UUID NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE TABLE order_tickets (
    order_id UUID NOT NULL,
    ticket_id UUID,
    CONSTRAINT fk_order_tickets_on_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
