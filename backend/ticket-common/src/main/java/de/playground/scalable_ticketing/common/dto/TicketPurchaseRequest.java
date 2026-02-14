package de.playground.scalable_ticketing.common.dto;

/**
 * Record representing a ticket purchase request event.
 * Is used as a shared data model between services accessing the queue.
 */
public record TicketPurchaseRequest(
    String requestId,
    String eventId,
    String userId,
    int quantity,
    String timestamp
) {}
