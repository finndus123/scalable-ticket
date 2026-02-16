package de.playground.scalable_ticketing.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Domain Entity representing an Event.
 * Shared between API and Worker modules.
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String location;

    @Column(name = "total_allocation", nullable = false)
    private Integer totalAllocation;

    @Column(name = "available_tickets", nullable = false)
    private Integer availableTickets;

    private BigDecimal price;

    public Event() {}

    public Event(String id, String name, Integer totalAllocation, Integer availableTickets) {
        this.id = id;
        this.name = name;
        this.totalAllocation = totalAllocation;
        this.availableTickets = availableTickets;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
