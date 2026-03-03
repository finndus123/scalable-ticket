package de.playground.scalable_ticketing.ticket_worker.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.playground.scalable_ticketing.common.domain.model.Ticket;
import de.playground.scalable_ticketing.common.domain.repository.TicketRepository;
import de.playground.scalable_ticketing.common.exception.InsufficientTicketsException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Service responsible for assigning available tickets to an order.
 * Separated from {@link EventWorkerService} to enable Retry on optimistic locking conflicts.
 */
@Service
public class TicketAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(TicketAssignmentService.class);

    private final TicketRepository ticketRepository;

    public TicketAssignmentService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Queries and assigns available tickets to the given order.
     * Retries up to 3 additional attempts on optimistic locking failures caused by concurrent modifications to the same ticket rows.
     *
     * @param eventId  the event to pull tickets from
     * @param orderId  the order to assign tickets to
     * @param quantity the number of tickets to assign
     * @throws InsufficientTicketsException      if fewer tickets are available than requested
     * @throws OptimisticLockingFailureException if all retry attempts are exhausted
     */
    @Retry(name = "database")
    @CircuitBreaker(name = "database")
    @Bulkhead(name = "database")
    @Transactional
    public void assignTickets(UUID eventId, UUID orderId, int quantity) {
        List<Ticket> availableTickets = ticketRepository.findAvailableByEventId(
                eventId, PageRequest.of(0, quantity)
        );

        if (availableTickets.size() < quantity) {
            throw new InsufficientTicketsException(
                    eventId.toString(), quantity, availableTickets.size()
            );
        }

        availableTickets.forEach(ticket -> ticket.assignToOrder(orderId));
        ticketRepository.saveAll(availableTickets);

        logger.info("Assigned {} tickets for event {} to order {}", quantity, eventId, orderId);
    }
}
