package de.playground.scalable_ticketing.common.exception;

/**
 * Exception thrown when there are not enough available tickets to fulfill an order.
 */
public class InsufficientTicketsException extends RuntimeException {

    /**
     * @param eventId  the event for which tickets were requested
     * @param requested the number of tickets requested
     * @param available the number of tickets currently available
     */
    public InsufficientTicketsException(String eventId, int requested, int available) {
        super("Insufficient tickets for event " + eventId + ": requested " + requested + ", available " + available);
    }
}
