package io.github.mystagogy.savepocket.wish.repository;

import io.github.mystagogy.savepocket.wish.entity.PriceHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findTop20ByWishIdOrderByChangedAtDesc(Long wishId);

    @Query("""
            select count(distinct ph.wish.id)
            from PriceHistory ph
            where ph.wish.user.id = :userId
              and ph.changedAt >= :start
              and ph.changedAt < :end
              and ph.changedPrice < ph.previousPrice
            """)
    long countDistinctDroppedWishCount(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
