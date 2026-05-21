package io.github.mystagogy.savepocket.wish.scheduler;

import io.github.mystagogy.savepocket.scheduler.entity.SchedulerRunStatus;
import io.github.mystagogy.savepocket.scheduler.service.SchedulerRunHistoryService;
import io.github.mystagogy.savepocket.wish.service.WishService;
import io.github.mystagogy.savepocket.wish.service.WishService.PriceRefreshResult;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WishPriceRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(WishPriceRefreshScheduler.class);
    private static final String JOB_NAME = "wishPriceRefreshScheduler";

    private final WishService wishService;
    private final SchedulerRunHistoryService schedulerRunHistoryService;

    public WishPriceRefreshScheduler(
            WishService wishService,
            SchedulerRunHistoryService schedulerRunHistoryService
    ) {
        this.wishService = wishService;
        this.schedulerRunHistoryService = schedulerRunHistoryService;
    }

    @Scheduled(cron = "${wish.scheduler.price-refresh-cron:0 0 0,12 * * *}")
    public void refreshWishReferencePrices() {
        LocalDateTime executedAt = LocalDateTime.now();
        try {
            PriceRefreshResult result = wishService.refreshLowestReferencePrices(executedAt);
            LocalDateTime finishedAt = LocalDateTime.now();
            SchedulerRunStatus status = result.failedCount() > 0
                    ? SchedulerRunStatus.PARTIAL_SUCCESS
                    : SchedulerRunStatus.SUCCESS;

            schedulerRunHistoryService.record(
                    JOB_NAME,
                    status,
                    executedAt,
                    finishedAt,
                    result.scannedCount(),
                    result.updatedCount(),
                    result.skippedCount(),
                    result.failedCount(),
                    null
            );

            log.info(
                    "wish price refresh scheduler finished. executedAt={}, scannedCount={}, updatedCount={}, skippedCount={}, failedCount={}",
                    executedAt,
                    result.scannedCount(),
                    result.updatedCount(),
                    result.skippedCount(),
                    result.failedCount()
            );
        } catch (RuntimeException ex) {
            LocalDateTime finishedAt = LocalDateTime.now();
            schedulerRunHistoryService.record(
                    JOB_NAME,
                    SchedulerRunStatus.FAILED,
                    executedAt,
                    finishedAt,
                    0,
                    0,
                    0,
                    1,
                    ex.getMessage()
            );
            log.error("wish price refresh scheduler failed. executedAt={}", executedAt, ex);
        }
    }
}
