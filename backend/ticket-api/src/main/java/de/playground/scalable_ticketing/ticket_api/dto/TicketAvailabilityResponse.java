package de.playground.scalable_ticketing.ticket_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO providing ticket availability details.
 * Implemented as a Record.
 */
@Schema(description = "Ticket availability information for a specific event")
public record TicketAvailabilityResponse(

        @Schema(description = "Unique identifier of the event", example = "e58ed763-928c-4155-bee9-fdbaaadc15f3")
        String eventId,
        @Schema(description = "Number of tickets currently available for purchase", example = "150")
        int availableTickets
) {
}
