package de.playground.scalable_ticketing.ticket_api.controller;

import de.playground.scalable_ticketing.ticket_api.dto.TicketAvailabilityResponse;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import de.playground.scalable_ticketing.ticket_api.service.EventApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@Validated
@Tag(name = "Events", description = "Endpoints for querying ticket availability and placing ticket orders for events")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    private final EventApiService eventApiService;

    public EventController(EventApiService eventApiService) {
        this.eventApiService = eventApiService;
    }

    /**
     * Checks the availability of tickets for a specific event.
     *
     * @param eventId The ID of the event.
     * @return The availability details.
     */
    @Operation(summary = "Get ticket availability", description = "Returns the number of available tickets for the given event.", parameters = {@Parameter(name = "eventId", description = "Unique identifier of the event", required = true, example = "e58ed763-928c-4155-bee9-fdbaaadc15f3")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability information retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TicketAvailabilityResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string")))
    })
    @GetMapping("/{eventId}/tickets/availability")
    public ResponseEntity<TicketAvailabilityResponse> checkAvailability(
            @PathVariable
            @NotBlank(message = "Event ID must not be blank")
            String eventId
    ) {
        logger.info("Request to check availability for event: {}", eventId);
        return ResponseEntity.ok(eventApiService.getAvailabilityCount(eventId));
    }

    /**
     * Places a ticket order.
     *
     * @param eventId      The ID of the event
     * @param orderRequest The order request details
     * @return 202 Accepted if the request is valid and has been queued.
     */
    @Operation(summary = "Place a ticket order", description = "Places an order for a ticket for the corresponding event.", parameters = {@Parameter(name = "eventId", description = "Unique identifier of the event", required = true, example = "e58ed763-928c-4155-bee9-fdbaaadc15f3")}, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ticket order details", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TicketOrderRequest.class))))
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Order accepted and queued for processing"),
            @ApiResponse(responseCode = "400", description = "Invalid request: validation failed")
    })
    @PostMapping("/{eventId}/tickets/order")
    public ResponseEntity<Void> placeOrder(
            @PathVariable
            @NotBlank(message = "Event ID must not be blank")
            String eventId,
            @Valid
            @RequestBody
            TicketOrderRequest orderRequest
    ) {
        logger.info("Request to place order for event: {}, payload: {}", eventId, orderRequest);
        eventApiService.createOrder(eventId, orderRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
