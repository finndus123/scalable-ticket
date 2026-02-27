package de.playground.scalable_ticketing.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain Entity representing an Event.
 * Shared between API and Worker modules.
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String location;

    @Column(name = "total_allocation", nullable = false)
    private Integer totalAllocation;

    @Column(name = "available_tickets", nullable = false)
    private Integer availableTickets;

    private BigDecimal price;

    public Event() {
    }

    public Event(
            UUID id,
            String name,
            String location,
            Integer totalAllocation,
            Integer availableTickets,
            BigDecimal price
    ) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.totalAllocation = totalAllocation;
        this.availableTickets = availableTickets;
        this.price = price;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public Integer getTotalAllocation() {
        return totalAllocation;
    }

    public Integer getAvailableTickets() {
        return availableTickets;
    }

    /**
     * Decrements the available ticket count by the given quantity.
     * Enforces invariant: available tickets cannot go below zero.
     *
     * @param quantity the number of tickets to subtract from availability
     * @throws IllegalStateException if the remaining tickets would be negative
     */
    public void decrementAvailableTickets(int quantity) {
        if (this.availableTickets < quantity) {
            throw new IllegalStateException(
                    "Cannot decrement by " + quantity + ": only " + this.availableTickets + " tickets available for event " + this.id
            );
        }
        this.availableTickets -= quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
