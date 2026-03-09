package de.playground.scalable_ticketing.common.domain.model;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Domain Entity representing a single Ticket.
 * Tickets are created during event creation and assigned to an order upon purchase.
 * Uses optimistic locking via {@code @Version} to prevent concurrent assignment conflicts.
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "order_id")
    private UUID orderId;

    @Version
    @Column(nullable = false)
    private Long version;

    public Ticket() {
    }

    public Ticket(UUID id, UUID eventId, TicketStatus status) {
        this.id = id;
        this.eventId = eventId;
        this.status = status;
    }

    /**
     * Assigns this ticket to an order, transitioning its status to SOLD.
     * Enforces business rule: only AVAILABLE tickets can be sold.
     *
     * @param orderId the ID of the order this ticket is assigned to
     * @throws IllegalStateException if the ticket is not in AVAILABLE status
     */
    public void assignToOrder(UUID orderId) {
        if (this.status != TicketStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Ticket " + this.id + " cannot be assigned: current status is " + this.status
            );
        }
        this.status = TicketStatus.SOLD;
        this.orderId = orderId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Long getVersion() {
        return version;
    }
}
