package de.playground.scalable_ticketing.ticket_api.service;

import de.playground.scalable_ticketing.ticket_api.dto.TicketAvailabilityResponse;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service handling ticket business logic, specifically interacting with Redis for fast reads.
 */
@Service
public class EventTicketService {

    private static final Logger logger = LoggerFactory.getLogger(EventTicketService.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public EventTicketService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks ticket availability from Redis cache.
     * Pattern: event:{eventId}:availability
     *
     * @param eventId The event ID.
     * @return Availability response.
     */
    public TicketAvailabilityResponse getAvailability(String eventId) {
        String cacheKey = "event:" + eventId + ":availability";
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue == null) {
            logger.info("Cache miss for event availability: {}", eventId);
            // TODO: add fallback to DB
            return new TicketAvailabilityResponse(eventId, 0);
        }

        int availabilityCount = (Integer) cachedValue;
        return new TicketAvailabilityResponse(eventId, availabilityCount);
    }

    /**
     * Places an order (Async publishing to RabbitMQ will be added in the future).
     *
     * @param orderRequest The order details.
     */
    public void placeOrder(TicketOrderRequest orderRequest) {
        logger.info("Placing order for event: {} by user: {}", orderRequest.eventId(), orderRequest.userId());
        // TODO: RabbitMQ publishing logic
    }
}
