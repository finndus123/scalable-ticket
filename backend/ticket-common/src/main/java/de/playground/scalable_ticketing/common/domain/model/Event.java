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

    public Event() {}

    public Event(UUID id, String name, String location, Integer totalAllocation, Integer availableTickets,  BigDecimal price) {
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

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getTotalAllocation() {
        return totalAllocation;
    }

    public void setTotalAllocation(Integer totalAllocation) {
        this.totalAllocation = totalAllocation;
    }

    public Integer getAvailableTickets() {
        return availableTickets;
    }

    public void setAvailableTickets(Integer availableTickets) {
        this.availableTickets = availableTickets;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
