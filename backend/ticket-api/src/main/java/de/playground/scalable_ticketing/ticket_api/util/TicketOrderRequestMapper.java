package de.playground.scalable_ticketing.ticket_api.util;

import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;

import java.time.Instant;

/**
 * Mapper for converting Request-DTOs: {@link TicketOrderRequest} and Event-DTOs for the Queue: {@link TicketOrderEvent}
 */
public final class TicketOrderRequestMapper {
    /**
     * Maps a validated {@link TicketOrderRequest} and the event ID from the URL to a {@link TicketOrderEvent} for publishing to RabbitMQ.
     *
     * @param eventId      the event ID taken from the URL path variable
     * @param orderRequest the validated request body
     * @return a {@link TicketOrderEvent}
     */
    public static TicketOrderEvent toEvent(String eventId, TicketOrderRequest orderRequest) {
        return new TicketOrderEvent(
                orderRequest.requestId(),
                eventId,
                orderRequest.userId(),
                orderRequest.quantity(),
                Instant.now().toString());
    }
}
