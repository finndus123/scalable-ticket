package de.playground.scalable_ticketing.ticket_worker.service;

import de.playground.scalable_ticketing.common.domain.model.Event;
import de.playground.scalable_ticketing.common.domain.model.Ticket;
import de.playground.scalable_ticketing.common.domain.model.TicketStatus;
import de.playground.scalable_ticketing.common.exception.InsufficientTicketsException;
import de.playground.scalable_ticketing.ticket_worker.service.resiliencewrapper.WorkerResilienceDatabaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketAssignmentService Unit Tests")
class TicketAssignmentServiceTest {

    @Mock
    private WorkerResilienceDatabaseService databaseService;

    @InjectMocks
    private TicketAssignmentService ticketAssignmentService;

    private UUID eventId;
    private UUID orderId;
    private Event event;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        event = new Event(
                eventId, "Concert", "Berlin", 100, 10, BigDecimal.TEN
        );
    }

    @Test
    @DisplayName("Should assign requested quantity of tickets successfully")
    void shouldAssignTicketsSuccessfully() {
        // Arrange
        Ticket ticket1 = new Ticket(UUID.randomUUID(), eventId, TicketStatus.AVAILABLE);
        Ticket ticket2 = new Ticket(UUID.randomUUID(), eventId, TicketStatus.AVAILABLE);
        List<Ticket> availableTickets = Arrays.asList(ticket1, ticket2);

        when(databaseService.getEventForUpdateOrThrow(eventId)).thenReturn(event);
        when(databaseService.findAvailableTicketsForUpdate(eventId, 2))
                .thenReturn(availableTickets);

        // Act
        ticketAssignmentService.assignTickets(eventId, orderId, 2);

        // Assert
        verify(databaseService).getEventForUpdateOrThrow(eventId);
        verify(databaseService).findAvailableTicketsForUpdate(eventId, 2);

        assertThat(ticket1.getOrderId()).isEqualTo(orderId);
        assertThat(ticket2.getOrderId()).isEqualTo(orderId);
        assertThat(event.getAvailableTickets()).isEqualTo(8);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(databaseService).saveAllTickets(captor.capture());

        List<Ticket> savedTickets = captor.getValue();
        assertThat(savedTickets).hasSize(2)
                .containsExactly(ticket1, ticket2);
    }

    @Test
    @DisplayName("Should throw InsufficientTicketsException when fewer tickets are available")
    void shouldThrowWhenNotEnoughTickets() {
        // Arrange
        Ticket ticket1 = new Ticket(UUID.randomUUID(), eventId, TicketStatus.AVAILABLE);
        List<Ticket> availableTickets = List.of(ticket1);

        when(databaseService.getEventForUpdateOrThrow(eventId)).thenReturn(event);
        when(databaseService.findAvailableTicketsForUpdate(eventId, 3))
                .thenReturn(availableTickets);

        // Act & Assert
        int requestedQuantity = 3;
        assertThatThrownBy(() -> ticketAssignmentService.assignTickets(eventId, orderId, requestedQuantity))
                .isInstanceOf(InsufficientTicketsException.class);
    }
}
