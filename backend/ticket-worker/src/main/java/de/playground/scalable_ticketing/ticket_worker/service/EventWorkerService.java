package de.playground.scalable_ticketing.ticket_worker.service;

import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.domain.repository.OrderRepository;
import de.playground.scalable_ticketing.common.domain.repository.TicketRepository;
import de.playground.scalable_ticketing.common.domain.repository.UserRepository;
import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Service responsible for consuming ticket order events from RabbitMQ.
 */
@Service
public class EventWorkerService {

    private static final Logger log = LoggerFactory.getLogger(EventWorkerService.class);

    private UserRepository userRepository;
    private TicketRepository ticketRepository;
    private EventRepository eventRepository;
    private OrderRepository orderRepository;

    public EventWorkerService(
            UserRepository userRepository,
            TicketRepository ticketRepository,
            EventRepository eventRepository,
            OrderRepository orderRepository
    ) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Listens to the ticket order queue and processes incoming events.
     * TODO add description of business logic
     *
     * @param event The received ticket order event.
     */
    @RabbitListener(queues = "${rabbitmq.queue}")
    public void receiveOrderEvent(TicketOrderEvent event) {
        log.info("Received ticket order event: {}", event);


        // TODO: Implement business logic to process the order
    }
}
