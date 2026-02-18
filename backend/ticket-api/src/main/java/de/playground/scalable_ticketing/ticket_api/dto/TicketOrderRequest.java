package de.playground.scalable_ticketing.ticket_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for placing a ticket order.
 * Implemented as a Record for immutability and concise syntax.
 */
@Schema(description = "Request payload for placing a ticket order")
public record TicketOrderRequest(

        @Schema(description = "Unique identifier of the event to order tickets for", example = "e58ed763-928c-4155-bee9-fdbaaadc15f3")
        @NotBlank(message = "Event ID is required")
        String eventId,

        @Schema(description = "Unique identifier of the user placing the order", example = "e58ed763-928c-4155-bee9-fdbaaadc15f3")
        @NotBlank(message = "User ID is required")
        String userId,

        @Schema(description = "Number of tickets to order, must be at least 1", example = "2")
        @Min(value = 1, message = "Quantity must be at least 1")
        @NotNull(message = "Quantity is required")
        Integer quantity,

        @Schema(description = "Optional idempotency key supplied by the client to deduplicate requests", example = "e58ed763-928c-4155-bee9-fdbaaadc15f3", nullable = true)
        String requestId
) {
}
