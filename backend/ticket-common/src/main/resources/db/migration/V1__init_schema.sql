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

-- Test Data (Simpsons Theme)
INSERT INTO users (id, name, email) VALUES
    ('e152f206-8ae8-41af-981f-702316e25547', 'Homer Simpson', 'homer.simpson@nuclear.com'),
    ('2cbbc4de-f831-4a1e-ab71-33104e7b8ec5', 'Montgomery Burns', 'mr.burns@nuclear.com');

INSERT INTO events (id, name, location, total_allocation, available_tickets, price) VALUES
    ('1ef5f991-62ba-4b35-80f4-cf3d7dfd31ba', 'Krusty the Clown Show', 'Channel 6 Studios', 1000, 1000, 15.00),
    ('81561a0b-118c-4f10-ae40-c3dccfcba904', 'Itchy & Scratchy Live', 'Springfield Arena', 5000, 5000, 45.50);

INSERT INTO tickets (id, event_id, status, order_id, version) VALUES
    ('6ad5eaea-80b7-4a19-9ce0-421712a83e01', '1ef5f991-62ba-4b35-80f4-cf3d7dfd31ba', 'AVAILABLE', NULL, 0),
    ('6ad5eaea-80b7-4a19-9ce0-421712a83e02', '1ef5f991-62ba-4b35-80f4-cf3d7dfd31ba', 'AVAILABLE', NULL, 0),
    ('6ad5eaea-80b7-4a19-9ce0-421712a83e03', '1ef5f991-62ba-4b35-80f4-cf3d7dfd31ba', 'AVAILABLE', NULL, 0);
