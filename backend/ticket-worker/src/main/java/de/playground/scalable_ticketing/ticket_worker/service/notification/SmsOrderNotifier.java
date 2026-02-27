package de.playground.scalable_ticketing.ticket_worker.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Email implementation of the OrderNotifier Interface
 * Implements methods for sending notifications to Users (Currently only logging)
 */
public class SmsOrderNotifier implements OrderNotifier {

    private static final Logger logger = LoggerFactory.getLogger(SmsOrderNotifier.class);

    @Override
    public void notifyError() {
        logger.info("Sending SMS Error Notification to User");
    }

    @Override
    public void notifySuccess() {
        logger.info("Sending SMS Success Notification to User");
    }
}
