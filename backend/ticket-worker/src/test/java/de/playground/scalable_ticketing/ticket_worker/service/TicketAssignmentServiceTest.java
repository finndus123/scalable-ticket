package de.playground.scalable_ticketing.ticket_worker.service;

import de.playground.scalable_ticketing.common.domain.model.Ticket;
import de.playground.scalable_ticketing.common.domain.model.TicketStatus;
import de.playground.scalable_ticketing.common.domain.repository.TicketRepository;
import de.playground.scalable_ticketing.common.exception.InsufficientTicketsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketAssignmentService Unit Tests")
class TicketAssignmentServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketAssignmentService ticketAssignmentService;

    private UUID eventId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should assign requested quantity of tickets successfully")
    void shouldAssignTicketsSuccessfully() {
        // Arrange
        Ticket ticket1 = new Ticket(UUID.randomUUID(), eventId, TicketStatus.AVAILABLE);
        Ticket ticket2 = new Ticket(UUID.randomUUID(), eventId, TicketStatus.AVAILABLE);
        List<Ticket> availableTickets = Arrays.asList(ticket1, ticket2);

        when(ticketRepository.findAvailableByEventId(eq(eventId), any(Pageable.class)))
                .thenReturn(availableTickets);

        // Act
        ticketAssignmentService.assignTickets(eventId, orderId, 2);

        // Assert
        verify(ticketRepository).findAvailableByEventId(eventId, PageRequest.of(0, 2));

        assertThat(ticket1.getOrderId()).isEqualTo(orderId);
        assertThat(ticket2.getOrderId()).isEqualTo(orderId);

        ArgumentCaptor<List<Ticket>> captor = ArgumentCaptor.forClass(List.class);
        verify(ticketRepository).saveAll(captor.capture());

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

        when(ticketRepository.findAvailableByEventId(eq(eventId), any(Pageable.class)))
                .thenReturn(availableTickets);

        // Act & Assert
        int requestedQuantity = 3;
        assertThatThrownBy(() -> ticketAssignmentService.assignTickets(eventId, orderId, requestedQuantity))
                .isInstanceOf(InsufficientTicketsException.class);
    }
}
