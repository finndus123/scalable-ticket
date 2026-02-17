package de.playground.scalable_ticketing.common.exception;

/**
 * Exception thrown when an event is not found in the system.
 */
public class EventNotFoundException extends RuntimeException {

    /**
     * Constructs a new EventNotFoundException with the specified event ID.
     *
     * @param id The ID of the event that was not found.
     */
    public EventNotFoundException(String id) {
        super("Event with ID: " + id + " not found");
    }
}
