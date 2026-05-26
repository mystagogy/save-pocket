package io.github.mystagogy.savepocket.analytics.consumer;

import static org.mockito.Mockito.verify;

import io.github.mystagogy.savepocket.analytics.service.WishEventAnalyticsService;
import io.github.mystagogy.savepocket.wish.events.WishDomainEvent;
import io.github.mystagogy.savepocket.wish.events.WishDomainEventType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishEventAnalyticsConsumerTest {

    @Mock
    private WishEventAnalyticsService wishEventAnalyticsService;

    @Test
    void consumeWishDomainEventDelegatesToAnalyticsService() {
        WishEventAnalyticsConsumer consumer = new WishEventAnalyticsConsumer(wishEventAnalyticsService);
        WishDomainEvent event = new WishDomainEvent(
                "evt-analytics-1",
                WishDomainEventType.WISH_EXPIRED,
                1,
                LocalDateTime.of(2026, 5, 26, 11, 0),
                200L,
                2L,
                "EXPIRED",
                null,
                18000L
        );

        consumer.consumeWishDomainEvent(event);

        verify(wishEventAnalyticsService).aggregateFromEvent(event);
    }
}
