package io.github.mystagogy.savepocket.wish.messaging;

public record PriceRefreshDispatchResult(
        int scannedCount,
        int updatedCount,
        int skippedCount,
        int failedCount
) {
}
