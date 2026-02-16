package de.playground.scalable_ticketing.ticket_api.service;

import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.ticket_api.dto.TicketAvailabilityResponse;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service handling ticket business logic, specifically interacting with Redis for fast reads.
 */
@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);
    private static final String CACHE_KEY_FORMAT = "event:%s:availability";
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final EventRepository eventRepository;

    public EventService(RedisTemplate<String, Object> redisTemplate, EventRepository eventRepository) {
        this.redisTemplate = redisTemplate;
        this.eventRepository = eventRepository;
    }

    /**
     * Checks ticket availability from Redis cache.
     * Fallback to database and populate cache if cache miss occurs.
     *
     * @param eventId The event ID.
     * @return Availability response.
     */
    public TicketAvailabilityResponse getAvailabilityCount(String eventId) {
        final String cacheKey = String.format(CACHE_KEY_FORMAT, eventId);
        final Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue != null) {
            final int availabilityCount = (Integer) cachedValue;
            return new TicketAvailabilityResponse(eventId, availabilityCount);
        }

        logger.info("Cache miss for event availability: {}. Falling back to database.", eventId);
        // Populate cache (Look-aside pattern)
        return eventRepository.findById(eventId)
                .map(event -> {
                    int count = event.getAvailableTickets();
                    redisTemplate.opsForValue().set(cacheKey, count, CACHE_TTL);
                    return new TicketAvailabilityResponse(eventId, count);
                })
                .orElseGet(() -> {
                    logger.warn("Event not found in database: {}", eventId);
                    // Todo Return custom error instead of 0 available Tickets
                    return new TicketAvailabilityResponse(eventId, 0);
                });
    }

    /**
     * Places an order (Async publishing to RabbitMQ will be added in the future).
     *
     * @param orderRequest The order details.
     */
    public void createOrder(TicketOrderRequest orderRequest) {
        logger.info("Placing order for event: {} by user: {}", orderRequest.eventId(), orderRequest.userId());
        // TODO: RabbitMQ publishing logic
    }
}
