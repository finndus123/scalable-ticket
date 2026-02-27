package de.playground.scalable_ticketing.ticket_worker.service.notification;

/**
 * Interface defines the contract for notifying users about the order process
 * abstracts the underlying communication channel (e.g., Email, SMS, ...)
 */
public interface OrderNotifier {
    void notifyError();
    void notifySuccess();
}