package de.playground.scalable_ticketing.ticket_worker.service.resiliencewrapper;

import de.playground.scalable_ticketing.common.domain.model.Event;
import de.playground.scalable_ticketing.common.domain.model.Order;
import de.playground.scalable_ticketing.common.domain.model.Ticket;
import de.playground.scalable_ticketing.common.domain.model.User;
import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.domain.repository.OrderRepository;
import de.playground.scalable_ticketing.common.domain.repository.TicketRepository;
import de.playground.scalable_ticketing.common.domain.repository.UserRepository;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import de.playground.scalable_ticketing.common.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service to execute database operations for the TicketWorker with Resilience4j patterns.
 * Explicitly separated to allow Spring to apply `@CircuitBreaker` and `@Bulkhead` without self-invocation issues.
 */
@Service
public class WorkerResilienceDatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerResilienceDatabaseService.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;

    public WorkerResilienceDatabaseService(
            UserRepository userRepository,
            EventRepository eventRepository,
            OrderRepository orderRepository,
            TicketRepository ticketRepository
    ) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
    }

    @CircuitBreaker(name = "database")
    @Bulkhead(name = "database")
    public User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found with id {}", userId);
                    return new UserNotFoundException(userId.toString());
                });
    }

    @CircuitBreaker(name = "database")
    @Bulkhead(name = "database")
    public Event getEventOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    logger.error("Event not found with id {}", eventId);
                    return new EventNotFoundException(eventId.toString());
                });
    }

    @CircuitBreaker(name = "database")
    @Bulkhead(name = "database")
    public Event getEventForUpdateOrThrow(UUID eventId) {
        return eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> {
                    logger.error("Event not found with id {}", eventId);
                    return new EventNotFoundException(eventId.toString());
                });
    }

    @CircuitBreaker(name = "database")
    @Bulkhead(name = "database")
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @CircuitBreaker(name = "database")
    @Bulkhead(name = "database")
    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    @CircuitBreaker(name = "database")
    @Bulkhead(name = "database")
    public List<Ticket> findAvailableTicketsForUpdate(UUID eventId, int quantity) {
        return ticketRepository.findAvailableByEventIdForUpdate(eventId, quantity);
    }

     @CircuitBreaker(name = "database")
     @Bulkhead(name = "database")
     public void saveAllTickets(List<Ticket> tickets) {
         ticketRepository.saveAll(tickets);
     }
}
