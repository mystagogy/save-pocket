package io.github.mystagogy.savepocket.analytics.repository;

import io.github.mystagogy.savepocket.analytics.entity.WishEventAnalyticsMonthly;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishEventAnalyticsMonthlyRepository extends JpaRepository<WishEventAnalyticsMonthly, Long> {

    Optional<WishEventAnalyticsMonthly> findByUserIdAndStatYearAndStatMonth(Long userId, int statYear, int statMonth);
}
