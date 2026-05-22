package io.github.mystagogy.savepocket.wish.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.mystagogy.savepocket.wish.service.WishService;
import io.github.mystagogy.savepocket.wish.service.WishService.PriceRefreshResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectWishPriceRefreshDispatcherTest {

    @Mock
    private WishService wishService;

    @Test
    void dispatchDelegatesToWishService() {
        DirectWishPriceRefreshDispatcher dispatcher = new DirectWishPriceRefreshDispatcher(wishService);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 22, 10, 30);
        when(wishService.refreshLowestReferencePrices(requestedAt))
                .thenReturn(new PriceRefreshResult(9, 4, 3, 2));

        PriceRefreshDispatchResult result = dispatcher.dispatch(requestedAt);

        assertThat(result.scannedCount()).isEqualTo(9);
        assertThat(result.updatedCount()).isEqualTo(4);
        assertThat(result.skippedCount()).isEqualTo(3);
        assertThat(result.failedCount()).isEqualTo(2);
    }
}
