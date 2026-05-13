package io.github.mystagogy.savepocket.report.repository;

import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends Repository<ProductWish, Long> {

    @Query("""
            select coalesce(sum(w.savedAmount), 0)
            from ProductWish w
            where w.user.id = :userId
              and w.status = :expiredStatus
              and w.expiredAt >= :start
              and w.expiredAt < :end
            """)
    Long sumExpiredAmountByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("expiredStatus") WishStatus expiredStatus,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select coalesce(sum(coalesce(w.userDealPrice, w.referencePrice)), 0)
            from ProductWish w
            where w.user.id = :userId
              and w.status = :purchasedStatus
              and w.updatedAt >= :start
              and w.updatedAt < :end
            """)
    Long sumPurchasedAmountByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("purchasedStatus") WishStatus purchasedStatus,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select w.id as wishId,
                   w.productName as wishName,
                   w.expiredAt as occurredAt,
                   coalesce(w.savedAmount, 0) as amount
            from ProductWish w
            where w.user.id = :userId
              and w.status = :expiredStatus
              and w.expiredAt >= :start
              and w.expiredAt < :end
            order by w.expiredAt desc
            """)
    List<MonthlySavingsDetailProjection> findExpiredDetailsByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("expiredStatus") WishStatus expiredStatus,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select w.id as wishId,
                   w.productName as wishName,
                   w.updatedAt as occurredAt,
                   coalesce(coalesce(w.userDealPrice, w.referencePrice), 0) as amount
            from ProductWish w
            where w.user.id = :userId
              and w.status = :purchasedStatus
              and w.updatedAt >= :start
              and w.updatedAt < :end
            order by w.updatedAt desc
            """)
    List<MonthlySavingsDetailProjection> findPurchasedDetailsByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("purchasedStatus") WishStatus purchasedStatus,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
