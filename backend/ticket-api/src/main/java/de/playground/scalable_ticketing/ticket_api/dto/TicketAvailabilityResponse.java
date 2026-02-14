package de.playground.scalable_ticketing.ticket_api.dto;

/**
 * Response DTO providing ticket availability details.
 * Implemented as a Record.
 */
public record TicketAvailabilityResponse(
    String eventId,
    int availableTickets,
    String eventName
) {}
