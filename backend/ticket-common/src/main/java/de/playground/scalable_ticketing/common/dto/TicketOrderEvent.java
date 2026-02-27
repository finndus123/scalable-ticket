package de.playground.scalable_ticketing.common.dto;

/**
 * Record representing a ticket purchase event for the queue.
 * Is used as a shared data model between the services accessing the queue.
 */
public record TicketOrderEvent(
        String requestId,
        String eventId,
        String userId,
        int quantity,
        String timestamp
) {
}
