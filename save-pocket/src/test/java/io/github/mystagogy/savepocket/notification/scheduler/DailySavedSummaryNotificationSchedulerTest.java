package io.github.mystagogy.savepocket.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mystagogy.savepocket.notification.service.NotificationService;
import io.github.mystagogy.savepocket.notification.service.NotificationService.DailySavedSummaryResult;
import io.github.mystagogy.savepocket.scheduler.entity.SchedulerRunStatus;
import io.github.mystagogy.savepocket.scheduler.service.SchedulerRunHistoryService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailySavedSummaryNotificationSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private SchedulerRunHistoryService schedulerRunHistoryService;

    @Test
    void sendDailySavedSummaryNotificationsRecordsSuccessHistory() {
        DailySavedSummaryNotificationScheduler scheduler = new DailySavedSummaryNotificationScheduler(
                notificationService,
                schedulerRunHistoryService
        );
        when(notificationService.createDailySavedSummaryNotifications(any(LocalDateTime.class)))
                .thenReturn(new DailySavedSummaryResult(10, 6, 4, 0));

        scheduler.sendDailySavedSummaryNotifications();

        ArgumentCaptor<LocalDateTime> executedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationService).createDailySavedSummaryNotifications(executedAtCaptor.capture());
        assertThat(executedAtCaptor.getValue()).isNotNull();
        verify(schedulerRunHistoryService).record(
                eq("dailySavedSummaryNotificationScheduler"),
                eq(SchedulerRunStatus.SUCCESS),
                any(),
                any(),
                eq(10),
                eq(6),
                eq(4),
                eq(0),
                eq(null)
        );
    }
}
