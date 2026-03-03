package de.playground.scalable_ticketing.ticket_api.service;

import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import de.playground.scalable_ticketing.ticket_api.dto.TicketAvailabilityResponse;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import de.playground.scalable_ticketing.ticket_api.service.resiliencewrapper.EventResilienceCacheService;
import de.playground.scalable_ticketing.ticket_api.service.resiliencewrapper.EventResilienceDatabaseService;
import de.playground.scalable_ticketing.ticket_api.service.resiliencewrapper.EventResilienceMessagingService;
import de.playground.scalable_ticketing.ticket_api.util.TicketOrderRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service handling event and ticket related business logic.
 * Responsibilities:
 * - Checking ticket availability using Redis cache or database as fallback (Look-aside pattern).
 * - Placing ticket orders by publishing events to RabbitMQ.
 * Infrastructure calls are delegated to dedicated service classes: ({@link EventResilienceCacheService}, {@link EventResilienceDatabaseService}, {@link EventResilienceMessagingService}) so that resilience4j annotations are applied via Spring AOP proxy.
 */
@Service
public class EventApiService {

    private static final Logger logger = LoggerFactory.getLogger(EventApiService.class);

    private final EventResilienceCacheService eventResilienceCacheService;
    private final EventResilienceDatabaseService eventResilienceDatabaseService;
    private final EventResilienceMessagingService eventResilienceMessagingService;

    public EventApiService(
            EventResilienceCacheService eventResilienceCacheService,
            EventResilienceDatabaseService eventResilienceDatabaseService,
            EventResilienceMessagingService eventResilienceMessagingService
    ) {
        this.eventResilienceCacheService = eventResilienceCacheService;
        this.eventResilienceDatabaseService = eventResilienceDatabaseService;
        this.eventResilienceMessagingService = eventResilienceMessagingService;
    }

    /**
     * Checks ticket availability from Redis cache.
     * Fallback to database and populate cache if cache miss occurs.
     *
     * @param eventId The event ID.
     * @return {@link TicketAvailabilityResponse} containing the event ID and availability count.
     * @throws EventNotFoundException if the event is not found in the database.
     */
    public TicketAvailabilityResponse getAvailabilityCount(String eventId) {
        Optional<Integer> cacheAvailabilityCount = eventResilienceCacheService.getAvailabilityCountFromCache(eventId);

        if (cacheAvailabilityCount.isPresent()) {
            return new TicketAvailabilityResponse(eventId, cacheAvailabilityCount.get());
        }

        logger.info("Cache miss for event availability: {}. Falling back to database.", eventId);

        int databaseAvailabilityCount = eventResilienceDatabaseService.findAvailableTickets(eventId);
        eventResilienceCacheService.writeAvailabilityCountToCache(eventId, databaseAvailabilityCount);
        return new TicketAvailabilityResponse(eventId, databaseAvailabilityCount);
    }

    /**
     * Places a ticket order by publishing a {@link TicketOrderEvent} to the RabbitMQ queue.
     *
     * @param eventId      the unique identifier of the event
     * @param orderRequest the validated request body
     */
    public void createOrder(String eventId, TicketOrderRequest orderRequest) {
        logger.info("Placing order for event: {} by user: {}", eventId, orderRequest.userId());
        TicketOrderEvent event = TicketOrderRequestMapper.toEvent(eventId, orderRequest);
        eventResilienceMessagingService.sendOrderEvent(event);
    }
}
