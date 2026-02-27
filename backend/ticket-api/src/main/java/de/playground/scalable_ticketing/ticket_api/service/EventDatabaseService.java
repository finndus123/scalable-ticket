package de.playground.scalable_ticketing.ticket_api.service;

import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service encapsulating all database interactions for event data.
 * <p>
 * Separated from {@link EventService} so that resilience4j annotations are applied via Spring AOP proxy (avoids the self-invocation problem).
 * Circuit-breaker instance: "database" (ignores {@link EventNotFoundException})
 * Bulkhead instance: "database" (limits concurrent DB calls)
 */
@Service
public class EventDatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(EventDatabaseService.class);

    private final EventRepository eventRepository;

    public EventDatabaseService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Queries the database for the number of available tickets for a given event.
     * Protected by a circuit breaker and a bulkhead to prevent overloading the database.
     * {@link EventNotFoundException} is configured as an ignored exception in the
     * circuit breaker and will not count as a failure.
     *
     * @param eventId the unique identifier of the event
     * @return the available ticket count
     * @throws EventNotFoundException if no event with the given ID exists
     */
    @CircuitBreaker(name = "database")
    @Bulkhead(name = "database")
    public int findAvailableTickets(String eventId) throws EventNotFoundException {
        logger.debug("Querying database for available tickets of event: {}", eventId);

        return eventRepository.findAvailableTicketsById(UUID.fromString(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
