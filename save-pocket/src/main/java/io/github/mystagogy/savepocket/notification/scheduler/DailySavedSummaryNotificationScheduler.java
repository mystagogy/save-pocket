package io.github.mystagogy.savepocket.notification.scheduler;

import io.github.mystagogy.savepocket.notification.service.NotificationService;
import io.github.mystagogy.savepocket.notification.service.NotificationService.DailySavedSummaryResult;
import io.github.mystagogy.savepocket.scheduler.entity.SchedulerRunStatus;
import io.github.mystagogy.savepocket.scheduler.service.SchedulerRunHistoryService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "notification.daily-savings",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DailySavedSummaryNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailySavedSummaryNotificationScheduler.class);
    private static final String JOB_NAME = "dailySavedSummaryNotificationScheduler";

    private final NotificationService notificationService;
    private final SchedulerRunHistoryService schedulerRunHistoryService;

    public DailySavedSummaryNotificationScheduler(
            NotificationService notificationService,
            SchedulerRunHistoryService schedulerRunHistoryService
    ) {
        this.notificationService = notificationService;
        this.schedulerRunHistoryService = schedulerRunHistoryService;
    }

    @Scheduled(cron = "${notification.daily-savings.cron:0 0 22 * * *}")
    public void sendDailySavedSummaryNotifications() {
        LocalDateTime executedAt = LocalDateTime.now();
        try {
            DailySavedSummaryResult result = notificationService.createDailySavedSummaryNotifications(executedAt);
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
                    result.createdCount(),
                    result.skippedCount(),
                    result.failedCount(),
                    null
            );
            log.info(
                    "daily saved summary notification scheduler finished. executedAt={}, scannedCount={}, createdCount={}, skippedCount={}, failedCount={}",
                    executedAt,
                    result.scannedCount(),
                    result.createdCount(),
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
            log.error("daily saved summary notification scheduler failed. executedAt={}", executedAt, ex);
        }
    }
}
