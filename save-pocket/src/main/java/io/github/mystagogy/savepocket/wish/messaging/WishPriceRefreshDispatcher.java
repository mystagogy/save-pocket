package io.github.mystagogy.savepocket.wish.messaging;

import java.time.LocalDateTime;

public interface WishPriceRefreshDispatcher {

    PriceRefreshDispatchResult dispatch(LocalDateTime requestedAt);
}
