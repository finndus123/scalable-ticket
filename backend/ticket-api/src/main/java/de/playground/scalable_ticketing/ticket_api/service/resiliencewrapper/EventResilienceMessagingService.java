package de.playground.scalable_ticketing.ticket_api.service.resiliencewrapper;

import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.ticket_api.service.EventApiService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service encapsulating all RabbitMQ messaging interactions for ticket orders with resilience annotations.
 * Separated from {@link EventApiService} so that resilience4j annotations are applied via Spring AOP proxy (avoids the self-invocation problem).
 * Circuit-breaker instance: "rabbitmq"
 * Retry instance: "rabbitmq" (retries on AmqpException and IOException)
 */
@Service
public class EventResilienceMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(EventResilienceMessagingService.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange:ticket.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.key:ticket.order.created}")
    private String routingKey;

    public EventResilienceMessagingService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a {@link TicketOrderEvent} to the configured RabbitMQ exchange.
     * <p>
     * Protected by a circuit breaker and a retry mechanism.
     * The retry is executed inside the circuit breaker window
     *
     * @param event the ticket order event to publish
     */
    @CircuitBreaker(name = "rabbitmq")
    @Retry(name = "rabbitmq")
    public void sendOrderEvent(TicketOrderEvent event) {
        logger.info("Sending order event to RabbitMQ exchange: {}, routingKey: {}", exchange, routingKey);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
