package io.github.mystagogy.savepocket.wish.messaging;

import io.github.mystagogy.savepocket.wish.service.WishService;
import io.github.mystagogy.savepocket.wish.service.WishService.PriceRefreshResult;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "wish.price-refresh.rabbitmq",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DirectWishPriceRefreshDispatcher implements WishPriceRefreshDispatcher {

    private final WishService wishService;

    public DirectWishPriceRefreshDispatcher(WishService wishService) {
        this.wishService = wishService;
    }

    @Override
    public PriceRefreshDispatchResult dispatch(LocalDateTime requestedAt) {
        PriceRefreshResult result = wishService.refreshLowestReferencePrices(requestedAt);
        return new PriceRefreshDispatchResult(
                result.scannedCount(),
                result.updatedCount(),
                result.skippedCount(),
                result.failedCount()
        );
    }
}
