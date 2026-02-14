package de.playground.scalable_ticketing.ticket_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for placing a ticket order.
 * Implemented as a Record for immutability and concise syntax.
 */
public record TicketOrderRequest(
    @NotBlank(message = "Event ID is required")
    String eventId,

    @NotBlank(message = "User ID is required")
    String userId,

    @Min(value = 1, message = "Quantity must be at least 1")
    @NotNull(message = "Quantity is required")
    Integer quantity,

    String requestId
) {}
