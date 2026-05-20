package io.github.mystagogy.savepocket.scheduler.repository;

import io.github.mystagogy.savepocket.scheduler.entity.SchedulerRunHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerRunHistoryRepository extends JpaRepository<SchedulerRunHistory, Long> {
}
