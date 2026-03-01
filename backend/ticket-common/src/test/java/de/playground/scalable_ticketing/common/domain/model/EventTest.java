package de.playground.scalable_ticketing.common.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    void decrementAvailableTickets_validQuantity_decreasesAvailability() {
        Event event = new Event(UUID.randomUUID(), "Test Event", "Location", 100, 50, BigDecimal.TEN);
        
        event.decrementAvailableTickets(5);
        
        assertEquals(45, event.getAvailableTickets());
    }

    @Test
    void decrementAvailableTickets_quantityExceedsAvailability_throwsException() {
        Event event = new Event(UUID.randomUUID(), "Test Event", "Location", 100, 50, BigDecimal.TEN);
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            event.decrementAvailableTickets(51);
        });
        
        assertTrue(exception.getMessage().contains("only 50 tickets available"));
    }
}
