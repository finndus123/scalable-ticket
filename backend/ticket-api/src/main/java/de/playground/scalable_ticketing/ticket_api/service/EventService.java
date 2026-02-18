package de.playground.scalable_ticketing.ticket_api.service;

import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.ticket_api.dto.TicketAvailabilityResponse;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import de.playground.scalable_ticketing.ticket_api.util.TicketOrderRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service handling event and ticket related business logic.
 * Responsibilities:
 * - Checking ticket availability using Redis cache or database as fallback (Look-aside pattern).
 * - Placing ticket orders by publishing events to RabbitMQ.
 */
@Service
public class EventService {

    private static final String CACHE_KEY_FORMAT = "event:%s:availability";
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final EventRepository eventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange:ticket.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.key:ticket.order.created}")
    private String routingKey;

    public EventService(
            RedisTemplate<String, Object> redisTemplate,
            EventRepository eventRepository,
            RabbitTemplate rabbitTemplate) {
        this.redisTemplate = redisTemplate;
        this.eventRepository = eventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Checks ticket availability from Redis cache.
     * Fallback to database and populate cache if cache miss occurs.
     *
     * @param eventId The event ID.
     * @throws EventNotFoundException if the event is not found in the database.
     * @return {@link TicketAvailabilityResponse} containing the event ID and availability count.
     */
    public TicketAvailabilityResponse getAvailabilityCount(String eventId) {
        final String cacheKey = String.format(CACHE_KEY_FORMAT, eventId);
        final Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue != null) {
            final int availabilityCount = (Integer) cachedValue;
            return new TicketAvailabilityResponse(eventId, availabilityCount);
        }

        logger.info("Cache miss for event availability: {}. Falling back to database.", eventId);
        return eventRepository.findById(eventId)
                .map(event -> {
                    int count = event.getAvailableTickets();
                    redisTemplate.opsForValue().set(cacheKey, count, CACHE_TTL);
                    return new TicketAvailabilityResponse(eventId, count);
                })
                .orElseThrow(() -> {
                    logger.warn("Event not found in database: {}", eventId);
                    return new EventNotFoundException(eventId);
                });
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
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        logger.info("Order event sent to RabbitMQ exchange: {}, routingKey: {}", exchange, routingKey);
    }
}
