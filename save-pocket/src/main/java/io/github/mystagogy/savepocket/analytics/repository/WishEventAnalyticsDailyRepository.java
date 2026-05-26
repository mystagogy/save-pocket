package io.github.mystagogy.savepocket.analytics.repository;

import io.github.mystagogy.savepocket.analytics.entity.WishEventAnalyticsDaily;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishEventAnalyticsDailyRepository extends JpaRepository<WishEventAnalyticsDaily, Long> {

    Optional<WishEventAnalyticsDaily> findByUserIdAndStatDate(Long userId, LocalDate statDate);
}
