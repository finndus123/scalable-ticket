package de.playground.scalable_ticketing.common.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketTest {

    @Test
    void assignToOrder_validStatus_updatesTicket() {
        Ticket ticket = new Ticket(UUID.randomUUID(), UUID.randomUUID(), TicketStatus.AVAILABLE);
        UUID orderId = UUID.randomUUID();
        
        ticket.assignToOrder(orderId);
        
        assertEquals(TicketStatus.SOLD, ticket.getStatus());
        assertEquals(orderId, ticket.getOrderId());
    }

    @Test
    void assignToOrder_invalidStatus_throwsException() {
        Ticket ticket = new Ticket(UUID.randomUUID(), UUID.randomUUID(), TicketStatus.SOLD);
        UUID orderId = UUID.randomUUID();
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ticket.assignToOrder(orderId);
        });
        
        assertTrue(exception.getMessage().contains("cannot be assigned: current status is SOLD"));
    }
}
