package de.playground.scalable_ticketing.ticket_worker.service.notification;

import de.playground.scalable_ticketing.common.domain.model.NotificationType;
import de.playground.scalable_ticketing.common.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationFactory Unit Tests")
class NotificationFactoryTest {

    @Test
    @DisplayName("Should return EmailOrderNotifier for EMAIL preference")
    void shouldReturnEmailNotifier() {
        // Arrange
        User user = new User(UUID.randomUUID(), "Jane", "jane@mail.com");
        user.setNotificationPreference(NotificationType.EMAIL);

        // Act
        OrderNotifier notifier = NotificationFactory.createNotifierFromUserPreferences(user);

        // Assert
        assertThat(notifier).isInstanceOf(EmailOrderNotifier.class);
    }

    @Test
    @DisplayName("Should return SmsOrderNotifier for SMS preference")
    void shouldReturnSmsNotifier() {
        // Arrange
        User user = new User(UUID.randomUUID(), "Jane", "jane@mail.com");
        user.setNotificationPreference(NotificationType.SMS);

        // Act
        OrderNotifier notifier = NotificationFactory.createNotifierFromUserPreferences(user);

        // Assert
        assertThat(notifier).isInstanceOf(SmsOrderNotifier.class);
    }
}
