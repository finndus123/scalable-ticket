package de.playground.scalable_ticketing.ticket_worker.service.resiliencewrapper;

import de.playground.scalable_ticketing.common.domain.model.Event;
import de.playground.scalable_ticketing.common.domain.model.Order;
import de.playground.scalable_ticketing.common.domain.model.Ticket;
import de.playground.scalable_ticketing.common.domain.model.TicketStatus;
import de.playground.scalable_ticketing.common.domain.model.User;
import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.domain.repository.OrderRepository;
import de.playground.scalable_ticketing.common.domain.repository.TicketRepository;
import de.playground.scalable_ticketing.common.domain.repository.UserRepository;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import de.playground.scalable_ticketing.common.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerDatabaseService Unit Tests")
class WorkerResilienceDatabaseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private WorkerResilienceDatabaseService workerResilienceDatabaseService;

    private UUID userId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should return User if exists")
    void shouldReturnUserIfExists() {
        // Arrange
        User expectedUser = new User(userId, "John", "john@mail.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

        // Act
        User actualUser = workerResilienceDatabaseService.getUserOrThrow(userId);

        // Assert
        assertThat(actualUser).isEqualTo(expectedUser);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if User does not exist")
    void shouldThrowIfUserNotExists() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> workerResilienceDatabaseService.getUserOrThrow(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    @DisplayName("Should return Event if exists")
    void shouldReturnEventIfExists() {
        // Arrange
        Event expectedEvent = new Event(eventId, "Concert", "Location", 100, 100, BigDecimal.TEN);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(expectedEvent));

        // Act
        Event actualEvent = workerResilienceDatabaseService.getEventOrThrow(eventId);

        // Assert
        assertThat(actualEvent).isEqualTo(expectedEvent);
        verify(eventRepository).findById(eventId);
    }

    @Test
    @DisplayName("Should throw EventNotFoundException if Event does not exist")
    void shouldThrowIfEventNotExists() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> workerResilienceDatabaseService.getEventOrThrow(eventId))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    @Test
    @DisplayName("Should return Event with write lock if exists")
    void shouldReturnEventForUpdateIfExists() {
        Event expectedEvent = new Event(eventId, "Concert", "Location", 100, 100, BigDecimal.TEN);
        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(Optional.of(expectedEvent));

        Event actualEvent = workerResilienceDatabaseService.getEventForUpdateOrThrow(eventId);

        assertThat(actualEvent).isEqualTo(expectedEvent);
        verify(eventRepository).findByIdForUpdate(eventId);
    }

    @Test
    @DisplayName("Should correctly save an Order")
    void shouldSaveOrder() {
        // Arrange
        Order order = new Order(UUID.randomUUID(), userId, eventId, 2, BigDecimal.TEN, Instant.now());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order savedOrder = workerResilienceDatabaseService.saveOrder(order);

        // Assert
        assertThat(savedOrder).isEqualTo(order);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Should correctly save an Event")
    void shouldSaveEvent() {
        // Arrange
        Event event = new Event(eventId, "Concert", "Location", 100, 100, BigDecimal.TEN);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // Act
        Event savedEvent = workerResilienceDatabaseService.saveEvent(event);

        // Assert
        assertThat(savedEvent).isEqualTo(event);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("Should return locked available tickets")
    void shouldReturnLockedAvailableTickets() {
        Ticket ticket = new Ticket(UUID.randomUUID(), eventId, TicketStatus.AVAILABLE);
        when(ticketRepository.findAvailableByEventIdForUpdate(eventId, 2)).thenReturn(List.of(ticket));

        List<Ticket> tickets = workerResilienceDatabaseService.findAvailableTicketsForUpdate(eventId, 2);

        assertThat(tickets).containsExactly(ticket);
        verify(ticketRepository).findAvailableByEventIdForUpdate(eventId, 2);
    }
}
