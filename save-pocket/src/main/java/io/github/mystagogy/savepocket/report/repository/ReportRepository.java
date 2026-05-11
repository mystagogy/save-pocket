package io.github.mystagogy.savepocket.report.repository;

import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends Repository<ProductWish, Long> {

    @Query("""
            select coalesce(sum(w.savedAmount), 0)
            from ProductWish w
            where w.user.id = :userId
              and w.status = :status
              and w.expiredAt >= :start
              and w.expiredAt < :end
            """)
    Long sumSavedAmountByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("status") WishStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
