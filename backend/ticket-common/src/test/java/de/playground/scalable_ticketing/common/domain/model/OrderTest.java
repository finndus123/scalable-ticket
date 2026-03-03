package de.playground.scalable_ticketing.common.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void getTotalPrice_returnsQuantityMultipliedByPrice() {
        Order order = new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                3,
                BigDecimal.valueOf(25),
                Instant.now()
        );
        
        assertEquals(BigDecimal.valueOf(75), order.getTotalPrice());
    }

    @Test
    void setStatus_validTransitions_updatesStatus() {
        // Valid transition PENDING -> COMPLETED
        Order order1 = new Order(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 
                BigDecimal.TEN, Instant.now()
        );
        order1.setStatus(OrderStatus.COMPLETED);
        assertEquals(OrderStatus.COMPLETED, order1.getStatus());
        
        // Valid transition PENDING -> FAILED
        Order order2 = new Order(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 
                BigDecimal.TEN, Instant.now()
        );
        order2.setStatus(OrderStatus.FAILED);
        assertEquals(OrderStatus.FAILED, order2.getStatus());
    }

    @Test
    void setStatus_invalidTransition_throwsException() {
        Order orderCompleted = new Order(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 
                BigDecimal.TEN, Instant.now()
        );
        orderCompleted.setStatus(OrderStatus.COMPLETED);

        Order orderFailed = new Order(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 
                BigDecimal.TEN, Instant.now()
        );
        orderFailed.setStatus(OrderStatus.FAILED);
        
        assertThrows(IllegalStateException.class, () -> {
            orderCompleted.setStatus(OrderStatus.PENDING);
        });

        assertThrows(IllegalStateException.class, () -> {
            orderFailed.setStatus(OrderStatus.PENDING);
        });
        
    }
}
