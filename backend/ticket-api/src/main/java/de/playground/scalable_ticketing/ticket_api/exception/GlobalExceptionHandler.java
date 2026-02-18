package de.playground.scalable_ticketing.ticket_api.exception;

import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the Ticket API.
 * Captures specific exceptions and transforms them into HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles EventNotFoundException and returns a 404 Not Found response.
     *
     * @param eventNotFoundException The exception instance.
     * @return ResponseEntity containing the error message and HTTP status 404.
     */
    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<String> handleEventNotFoundException(EventNotFoundException eventNotFoundException) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(eventNotFoundException.getMessage());
    }

    /**
     * Handles ConstraintViolationException and returns a 400 Bad Request response.
     *
     * @param ex The exception instance.
     * @return ResponseEntity containing the violation messages and HTTP status 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }
}
