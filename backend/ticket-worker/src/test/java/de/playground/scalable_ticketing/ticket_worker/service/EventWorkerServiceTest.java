package de.playground.scalable_ticketing.ticket_worker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.playground.scalable_ticketing.common.domain.model.Event;
import de.playground.scalable_ticketing.common.domain.model.Order;
import de.playground.scalable_ticketing.common.domain.model.OrderStatus;
import de.playground.scalable_ticketing.common.domain.model.User;
import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.common.exception.InsufficientTicketsException;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventWorkerService Unit Tests")
class EventWorkerServiceTest {

    @Mock
    private WorkerDatabaseService databaseService;

    @Mock
    private WorkerCacheService cacheService;

    @Mock
    private TicketAssignmentService ticketAssignmentService;

    @InjectMocks
    private EventWorkerService eventWorkerService;

    private User sampleUser;
    private Event sampleEvent;
    private TicketOrderEvent sampleOrderEvent;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        sampleUser = new User(userId, "John Doe", "john@example.com");

        sampleEvent = new Event(
                eventId,
                "Test Concert",
                "Berlin",
                100,
                100,
                BigDecimal.valueOf(50.0)
        );

        sampleOrderEvent = new TicketOrderEvent(
                UUID.randomUUID().toString(),
                eventId.toString(),
                userId.toString(),
                2,
                Instant.now().toString()
        );

        sampleOrder = new Order(
                orderId,
                userId,
                eventId,
                2,
                BigDecimal.valueOf(50.0),
                Instant.parse(sampleOrderEvent.timestamp())
        );
    }

    @Test
    @DisplayName("Should successfully process order, assign tickets, and switch status to COMPLETED")
    void shouldSuccessfullyProcessOrder() {
        // Arrange
        when(databaseService.getUserOrThrow(any(UUID.class))).thenReturn(sampleUser);
        when(databaseService.getEventOrThrow(any(UUID.class))).thenReturn(sampleEvent);
        when(databaseService.saveOrder(any(Order.class))).thenReturn(sampleOrder);

        // Act
        eventWorkerService.receiveOrderEvent(sampleOrderEvent);

        // Assert
        verify(databaseService).getUserOrThrow(sampleUser.getId());
        verify(databaseService).getEventOrThrow(sampleEvent.getId());

        // Verify PENDING order is saved first
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(databaseService, times(2)).saveOrder(orderCaptor.capture());
        
        Order firstSavedOrder = orderCaptor.getAllValues().getFirst();
        assertThat(firstSavedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Verify assignment and cache invalidation
        verify(ticketAssignmentService).assignTickets(sampleEvent.getId(), sampleOrder.getId(), sampleOrderEvent.quantity());
        verify(databaseService).saveEvent(sampleEvent);
        verify(cacheService).invalidateAvailabilityCache(sampleOrderEvent.eventId());

        // Verify final order state
        Order finalSavedOrder = orderCaptor.getAllValues().get(1);
        assertThat(finalSavedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should update order status to FAILED when ticket assignment fails")
    void shouldFailOrderWhenAssignmentFails() {
        // Arrange
        when(databaseService.getUserOrThrow(any(UUID.class))).thenReturn(sampleUser);
        when(databaseService.getEventOrThrow(any(UUID.class))).thenReturn(sampleEvent);
        when(databaseService.saveOrder(any(Order.class))).thenReturn(sampleOrder);

        doThrow(new InsufficientTicketsException(sampleEvent.getId().toString(), 2, 0))
                .when(ticketAssignmentService).assignTickets(any(UUID.class), any(UUID.class), anyInt());

        // Act
        eventWorkerService.receiveOrderEvent(sampleOrderEvent);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(databaseService, times(2)).saveOrder(orderCaptor.capture());

        Order finalSavedOrder = orderCaptor.getAllValues().get(1);
        assertThat(finalSavedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);

        // Ensure cache invalidation was skipped
        verify(cacheService, never()).invalidateAvailabilityCache(anyString());
    }
}
