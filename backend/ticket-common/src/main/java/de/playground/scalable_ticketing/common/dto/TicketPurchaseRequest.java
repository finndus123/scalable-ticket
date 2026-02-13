package de.playground.scalable_ticketing.common.dto;

import java.io.Serializable;

/**
 * Class representing a ticket purchase request event.
 * Is used as a shared data model between services accessing the queue.
 */
public class TicketPurchaseRequest implements Serializable {

    private String requestId;
    private String eventId;
    private String userId;
    private int quantity;
    private String timestamp;

    public TicketPurchaseRequest() {}

    public TicketPurchaseRequest(String requestId, String eventId, String userId, int quantity, String timestamp) {
        this.requestId = requestId;
        this.eventId = eventId;
        this.userId = userId;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TicketPurchaseRequest{" +
                "requestId='" + requestId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", userId='" + userId + '\'' +
                ", quantity=" + quantity +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
