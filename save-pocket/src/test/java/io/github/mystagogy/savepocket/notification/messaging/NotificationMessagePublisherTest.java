package io.github.mystagogy.savepocket.notification.messaging;

import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationMessagePublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publishPriceDropAfterCommitPublishesImmediatelyWhenTransactionIsNotActive() {
        NotificationRabbitProperties properties = new NotificationRabbitProperties(
                "wish.notification.exchange",
                "wish.notification.queue",
                "wish.notification.dlq",
                "wish.notification.price-drop"
        );
        NotificationMessagePublisher publisher = new NotificationMessagePublisher(rabbitTemplate, properties);
        PriceDropNotificationMessage message = new PriceDropNotificationMessage(
                1L,
                10L,
                120000L,
                99000L,
                LocalDateTime.of(2026, 5, 21, 10, 0)
        );

        publisher.publishPriceDropAfterCommit(message);

        verify(rabbitTemplate).convertAndSend(
                "wish.notification.exchange",
                "wish.notification.price-drop",
                message
        );
    }
}
