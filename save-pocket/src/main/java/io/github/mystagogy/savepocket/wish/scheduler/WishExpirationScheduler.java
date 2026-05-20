package io.github.mystagogy.savepocket.wish.scheduler;

import io.github.mystagogy.savepocket.scheduler.entity.SchedulerRunStatus;
import io.github.mystagogy.savepocket.scheduler.service.SchedulerRunHistoryService;
import io.github.mystagogy.savepocket.wish.service.WishService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WishExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(WishExpirationScheduler.class);
    private static final String JOB_NAME = "wishExpirationScheduler";

    private final WishService wishService;
    private final SchedulerRunHistoryService schedulerRunHistoryService;

    public WishExpirationScheduler(WishService wishService, SchedulerRunHistoryService schedulerRunHistoryService) {
        this.wishService = wishService;
        this.schedulerRunHistoryService = schedulerRunHistoryService;
    }

    @Scheduled(cron = "${wish.scheduler.expiration-cron:0 */10 * * * *}")
    public void expireDueWishes() {
        LocalDateTime executedAt = LocalDateTime.now();
        try {
            int expiredCount = wishService.expireDueWishes(executedAt);
            LocalDateTime finishedAt = LocalDateTime.now();
            schedulerRunHistoryService.record(
                    JOB_NAME,
                    SchedulerRunStatus.SUCCESS,
                    executedAt,
                    finishedAt,
                    expiredCount,
                    expiredCount,
                    0,
                    0,
                    null
            );
            log.info("wish expiration scheduler finished. executedAt={}, expiredCount={}", executedAt, expiredCount);
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
            log.error("wish expiration scheduler failed. executedAt={}", executedAt, ex);
        }
    }
}
