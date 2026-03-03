package de.playground.scalable_ticketing.common.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain Entity representing an Order.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    public Order() {
    }

    public Order(
            UUID id,
            UUID userId,
            UUID eventId,
            int quantity,
            BigDecimal price,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.quantity = quantity;
        this.price = price;
        this.createdAt = createdAt;
        this.status = OrderStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public BigDecimal getTotalPrice() {
        return BigDecimal.valueOf(quantity).multiply(price);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Transitions the order to a new status.
     * Only allows valid transitions: {@code PENDING → COMPLETED} and {@code PENDING → FAILED}.
     *
     * @param newStatus the target status to transition to
     * @throws IllegalStateException if the transition is not allowed
     */
    public void setStatus(OrderStatus newStatus) {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Order " + this.id + " cannot transition from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
    }
}
