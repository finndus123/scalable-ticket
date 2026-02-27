package de.playground.scalable_ticketing.ticket_worker.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import de.playground.scalable_ticketing.common.domain.model.Event;
import de.playground.scalable_ticketing.common.domain.model.Order;
import de.playground.scalable_ticketing.common.domain.model.OrderStatus;
import de.playground.scalable_ticketing.common.domain.model.User;
import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
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

    private final WorkerDatabaseService databaseService;
    private final WorkerCacheService cacheService;
    private final TicketAssignmentService ticketAssignmentService;

    public EventWorkerService(
            WorkerDatabaseService databaseService,
            WorkerCacheService cacheService,
            TicketAssignmentService ticketAssignmentService
    ) {
        this.databaseService = databaseService;
        this.cacheService = cacheService;
        this.ticketAssignmentService = ticketAssignmentService;
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

        User user = databaseService.getUserOrThrow(UUID.fromString(ticketOrder.userId()));
        Event event = databaseService.getEventOrThrow(UUID.fromString(ticketOrder.eventId()));

        Order order = databaseService.saveOrder(
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
            databaseService.saveEvent(event);

            cacheService.invalidateAvailabilityCache(ticketOrder.eventId());

            order.setStatus(OrderStatus.COMPLETED);
            databaseService.saveOrder(order);

            logger.info("Order {} completed successfully for event {} with {} tickets",
                    order.getId(), ticketOrder.eventId(), ticketOrder.quantity());
            userNotifier.notifySuccess();

        } catch (Exception ex) {
            logger.error("Order {} failed for event {}: {}",
                    order.getId(), ticketOrder.eventId(), ex.getMessage(), ex);

            order.setStatus(OrderStatus.FAILED);
            databaseService.saveOrder(order);

            userNotifier.notifyError();
        }
    }
}
