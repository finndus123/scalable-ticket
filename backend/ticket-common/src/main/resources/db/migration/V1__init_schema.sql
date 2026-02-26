-- Flyway Database Migration V1: Init Schema

CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    total_allocation INT NOT NULL,
    available_tickets INT NOT NULL,
    price DECIMAL(10, 2)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    event_id UUID NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_order_event FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    order_id UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_ticket_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_ticket_order FOREIGN KEY (order_id) REFERENCES orders(id)
);