package io.github.mystagogy.savepocket.notification.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.rabbitmq")
public record NotificationRabbitProperties(
        String exchange,
        String queue,
        String dlq,
        String routingKey
) {
}
