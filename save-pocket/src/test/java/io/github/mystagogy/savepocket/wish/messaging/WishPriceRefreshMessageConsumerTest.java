package io.github.mystagogy.savepocket.wish.messaging;

import static org.mockito.Mockito.verify;

import io.github.mystagogy.savepocket.wish.service.WishService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishPriceRefreshMessageConsumerTest {

    @Mock
    private WishService wishService;

    @Test
    void consumePriceRefreshMessageDelegatesToWishService() {
        WishPriceRefreshMessageConsumer consumer = new WishPriceRefreshMessageConsumer(wishService);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 22, 11, 0);
        PriceRefreshMessage message = new PriceRefreshMessage(31L, requestedAt);

        consumer.consumePriceRefreshMessage(message);

        verify(wishService).refreshLowestReferencePriceByWishId(31L, requestedAt);
    }
}
