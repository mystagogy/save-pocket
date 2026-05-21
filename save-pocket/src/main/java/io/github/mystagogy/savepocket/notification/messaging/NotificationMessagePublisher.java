package io.github.mystagogy.savepocket.notification.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@ConditionalOnProperty(prefix = "notification.rabbitmq", name = "enabled", havingValue = "true")
public class NotificationMessagePublisher implements NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final NotificationRabbitProperties properties;

    public NotificationMessagePublisher(RabbitTemplate rabbitTemplate, NotificationRabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publishPriceDropAfterCommit(PriceDropNotificationMessage message) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishPriceDrop(message);
                }
            });
            return;
        }

        publishPriceDrop(message);
    }

    private void publishPriceDrop(PriceDropNotificationMessage message) {
        rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), message);
    }
}
