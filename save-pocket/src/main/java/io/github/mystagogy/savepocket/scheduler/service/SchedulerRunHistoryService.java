package io.github.mystagogy.savepocket.scheduler.service;

import io.github.mystagogy.savepocket.scheduler.entity.SchedulerRunHistory;
import io.github.mystagogy.savepocket.scheduler.entity.SchedulerRunStatus;
import io.github.mystagogy.savepocket.scheduler.repository.SchedulerRunHistoryRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SchedulerRunHistoryService {

    private static final int ERROR_MESSAGE_LIMIT = 1000;

    private final SchedulerRunHistoryRepository schedulerRunHistoryRepository;

    public SchedulerRunHistoryService(SchedulerRunHistoryRepository schedulerRunHistoryRepository) {
        this.schedulerRunHistoryRepository = schedulerRunHistoryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String jobName,
            SchedulerRunStatus status,
            LocalDateTime executedAt,
            LocalDateTime finishedAt,
            int scannedCount,
            int successCount,
            int skippedCount,
            int failedCount,
            String errorMessage
    ) {
        SchedulerRunHistory history = new SchedulerRunHistory();
        history.setJobName(jobName);
        history.setStatus(status);
        history.setExecutedAt(executedAt);
        history.setFinishedAt(finishedAt);
        history.setScannedCount(scannedCount);
        history.setSuccessCount(successCount);
        history.setSkippedCount(skippedCount);
        history.setFailedCount(failedCount);
        history.setErrorMessage(trimErrorMessage(errorMessage));
        schedulerRunHistoryRepository.save(history);
    }

    private String trimErrorMessage(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            return null;
        }
        if (errorMessage.length() <= ERROR_MESSAGE_LIMIT) {
            return errorMessage;
        }
        return errorMessage.substring(0, ERROR_MESSAGE_LIMIT);
    }
}
