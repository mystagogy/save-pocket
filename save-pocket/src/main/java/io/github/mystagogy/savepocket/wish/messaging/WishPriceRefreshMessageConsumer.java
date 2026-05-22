package io.github.mystagogy.savepocket.wish.messaging;

import io.github.mystagogy.savepocket.wish.service.WishService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "wish.price-refresh.rabbitmq", name = "enabled", havingValue = "true")
public class WishPriceRefreshMessageConsumer {

    private final WishService wishService;

    public WishPriceRefreshMessageConsumer(WishService wishService) {
        this.wishService = wishService;
    }

    @RabbitListener(queues = "${wish.price-refresh.rabbitmq.queue}")
    public void consumePriceRefreshMessage(PriceRefreshMessage message) {
        wishService.refreshLowestReferencePriceByWishId(message.wishId(), message.requestedAt());
    }
}
