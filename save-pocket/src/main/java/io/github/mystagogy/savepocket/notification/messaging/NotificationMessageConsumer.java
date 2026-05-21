package io.github.mystagogy.savepocket.notification.messaging;

import io.github.mystagogy.savepocket.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notification.rabbitmq", name = "enabled", havingValue = "true")
public class NotificationMessageConsumer {

    private final NotificationService notificationService;

    public NotificationMessageConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${notification.rabbitmq.queue}")
    public void consumePriceDropMessage(PriceDropNotificationMessage message) {
        notificationService.createPriceDropLowestNotification(
                message.userId(),
                message.wishId(),
                message.previousReferencePrice(),
                message.latestReferencePrice(),
                message.occurredAt()
        );
    }
}
