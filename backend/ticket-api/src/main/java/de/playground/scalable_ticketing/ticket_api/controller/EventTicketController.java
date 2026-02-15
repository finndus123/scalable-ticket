package de.playground.scalable_ticketing.ticket_api.controller;

import de.playground.scalable_ticketing.ticket_api.dto.TicketAvailabilityResponse;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import de.playground.scalable_ticketing.ticket_api.service.EventTicketService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventTicketController {

    private static final Logger logger = LoggerFactory.getLogger(EventTicketController.class);
    private final EventTicketService eventTicketService;

    public EventTicketController(EventTicketService eventTicketService) {
        this.eventTicketService = eventTicketService;
    }

    /**
     * Checks the availability of tickets for a specific event.
     *
     * @param eventId The ID of the event.
     * @return The availability details.
     */
    @GetMapping("/{eventId}/tickets/availability")
    public ResponseEntity<TicketAvailabilityResponse> checkAvailability(@PathVariable String eventId) {
        logger.info("Request to check availability for event: {}", eventId);
        return ResponseEntity.ok(eventTicketService.getAvailability(eventId));
    }

    /**
     * Places a ticket order.
     *
     * @param eventId The ID of the event.
     * @param orderRequest The order request details.
     * @return 202 Accepted if validated.
     */
    @PostMapping("/{eventId}/tickets/order")
    public ResponseEntity<Void> placeOrder(
            @PathVariable String eventId,
            @Valid @RequestBody TicketOrderRequest orderRequest) {
        
        logger.info("Request to place order for event: {}, payload: {}", eventId, orderRequest);

        // Path variable validation
        if (!eventId.equals(orderRequest.eventId())) {
             logger.warn("Event ID mismatch: path={}, body={}", eventId, orderRequest.eventId());
             return ResponseEntity.badRequest().build();
        }

        eventTicketService.placeOrder(orderRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
