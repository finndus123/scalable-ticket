package de.playground.scalable_ticketing.ticket_worker.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import de.playground.scalable_ticketing.common.domain.model.Event;
import de.playground.scalable_ticketing.common.domain.model.Order;
import de.playground.scalable_ticketing.common.domain.model.OrderStatus;
import de.playground.scalable_ticketing.common.domain.model.User;
import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.domain.repository.OrderRepository;
import de.playground.scalable_ticketing.common.domain.repository.UserRepository;
import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import de.playground.scalable_ticketing.common.exception.UserNotFoundException;
import de.playground.scalable_ticketing.ticket_worker.service.notification.NotificationFactory;
import de.playground.scalable_ticketing.ticket_worker.service.notification.OrderNotifier;

/**
 * Service responsible for consuming ticket order events from RabbitMQ
 * and orchestrating the complete booking flow.
 *
 * Flow:
 * <ol>
 *   <li>Validate user and event existence</li>
 *   <li>Create a PENDING order</li>
 *   <li>Assign tickets via {@link TicketAssignmentService} (retryable)</li>
 *   <li>Decrement event availability and invalidate Redis cache</li>
 *   <li>Mark order as COMPLETED or FAILED and notify the user</li>
 * </ol>
 */
@Service
public class EventWorkerService {

    private static final Logger logger = LoggerFactory.getLogger(EventWorkerService.class);
    private static final String CACHE_KEY_PATTERN = "event:%s:availability";

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketAssignmentService ticketAssignmentService;
    private final RedisTemplate<String, Object> redisTemplate;

    public EventWorkerService(
            UserRepository userRepository,
            EventRepository eventRepository,
            OrderRepository orderRepository,
            TicketAssignmentService ticketAssignmentService,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.ticketAssignmentService = ticketAssignmentService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Listens to the ticket order queue and processes incoming purchase events.
     * Creates an order in PENDING state, attempts to assign tickets, and transitions the order to COMPLETED or FAILED depending on the outcome.
     *
     * @param ticketOrder the received ticket order event from RabbitMQ
     */
    @RabbitListener(queues = "${rabbitmq.queue}")
    public void receiveOrderEvent(TicketOrderEvent ticketOrder) {
        logger.info("Received ticket order event: requestId={}, eventId={}, userId={}, quantity={}",
                ticketOrder.requestId(), ticketOrder.eventId(), ticketOrder.userId(), ticketOrder.quantity());

        User user = userRepository.findById(UUID.fromString(ticketOrder.userId()))
                .orElseThrow(() -> {
                    logger.error("User not found with id {}", ticketOrder.userId());
                    return new UserNotFoundException(ticketOrder.userId());
                });

        Event event = eventRepository.findById(UUID.fromString(ticketOrder.eventId()))
                .orElseThrow(() -> {
                    logger.error("Event not found with id {}", ticketOrder.eventId());
                    return new EventNotFoundException(ticketOrder.eventId());
                });

        Order order = orderRepository.save(
                new Order(
                        UUID.randomUUID(),
                        user.getId(),
                        UUID.fromString(ticketOrder.eventId()),
                        ticketOrder.quantity(),
                        event.getPrice(),
                        Instant.parse(ticketOrder.timestamp()),
                        OrderStatus.PENDING
                )
        );

        OrderNotifier userNotifier = NotificationFactory.createNotifierFromUserPreferences(user);

        try {
            ticketAssignmentService.assignTickets(
                    event.getId(), order.getId(), ticketOrder.quantity()
            );

            event.decrementAvailableTickets(ticketOrder.quantity());
            eventRepository.save(event);

            invalidateAvailabilityCache(ticketOrder.eventId());

            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);

            logger.info("Order {} completed successfully for event {} with {} tickets",
                    order.getId(), ticketOrder.eventId(), ticketOrder.quantity());
            userNotifier.notifySuccess();

        } catch (Exception ex) {
            logger.error("Order {} failed for event {}: {}",
                    order.getId(), ticketOrder.eventId(), ex.getMessage(), ex);

            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            userNotifier.notifyError();
        }
    }

    private void invalidateAvailabilityCache(String eventId) {
        String cacheKey = String.format(CACHE_KEY_PATTERN, eventId);
        redisTemplate.delete(cacheKey);
        logger.info("Invalidated Redis cache for key {}", cacheKey);
    }
}
