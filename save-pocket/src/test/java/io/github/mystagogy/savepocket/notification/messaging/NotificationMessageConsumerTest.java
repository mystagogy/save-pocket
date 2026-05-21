package io.github.mystagogy.savepocket.notification.messaging;

import static org.mockito.Mockito.verify;

import io.github.mystagogy.savepocket.notification.service.NotificationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationMessageConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void consumePriceDropMessageDelegatesToNotificationService() {
        NotificationMessageConsumer consumer = new NotificationMessageConsumer(notificationService);
        PriceDropNotificationMessage message = new PriceDropNotificationMessage(
                1L,
                10L,
                120000L,
                99000L,
                LocalDateTime.of(2026, 5, 21, 10, 0)
        );

        consumer.consumePriceDropMessage(message);

        verify(notificationService).createPriceDropLowestNotification(
                1L,
                10L,
                120000L,
                99000L,
                LocalDateTime.of(2026, 5, 21, 10, 0)
        );
    }
}
