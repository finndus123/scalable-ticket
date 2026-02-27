package de.playground.scalable_ticketing.common.exception;

/**
 * Exception thrown when an event is not found in the system.
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructs a new UserNotFoundException with the specified event ID.
     *
     * @param id The ID of the event that was not found.
     */
    public UserNotFoundException(String id) {
        super("User with ID: " + id + " not found");
    }
}
