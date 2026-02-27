package de.playground.scalable_ticketing.ticket_worker.service.notification;

import de.playground.scalable_ticketing.common.domain.model.User;

/**
 * Factory for creating OrderNotifier instances
 */
public class NotificationFactory {

    /**
     * Creates a OrderNotifier based on the user preferences
     * @param user for that we want to create the Notifier based on preferences
     *
     * @return OrderNotifier notifier implementation based on the user preferences
     */
    public static OrderNotifier createNotifierFromUserPreferences(User user) {
        return switch (user.getNotificationPreference()) {
            case EMAIL -> new EmailOrderNotifier();
            case SMS -> new SmsOrderNotifier();
        };
    }

}
