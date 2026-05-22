package io.github.mystagogy.savepocket.wish.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wish.price-refresh.rabbitmq")
public record WishPriceRefreshRabbitProperties(
        String exchange,
        String queue,
        String dlq,
        String routingKey
) {
}
