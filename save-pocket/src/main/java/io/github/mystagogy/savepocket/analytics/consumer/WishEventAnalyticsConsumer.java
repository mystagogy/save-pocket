package io.github.mystagogy.savepocket.analytics.consumer;

import io.github.mystagogy.savepocket.analytics.service.WishEventAnalyticsService;
import io.github.mystagogy.savepocket.wish.events.WishDomainEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "wish.events.kafka.consumer", name = "enabled", havingValue = "true")
public class WishEventAnalyticsConsumer {

    private final WishEventAnalyticsService wishEventAnalyticsService;

    public WishEventAnalyticsConsumer(WishEventAnalyticsService wishEventAnalyticsService) {
        this.wishEventAnalyticsService = wishEventAnalyticsService;
    }

    @KafkaListener(
            topics = "${wish.events.kafka.topic}",
            groupId = "${wish.events.kafka.consumer.group-id}"
    )
    public void consumeWishDomainEvent(WishDomainEvent event) {
        wishEventAnalyticsService.aggregateFromEvent(event);
    }
}
