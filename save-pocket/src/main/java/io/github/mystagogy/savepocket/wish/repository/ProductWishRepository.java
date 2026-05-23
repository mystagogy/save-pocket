package io.github.mystagogy.savepocket.wish.repository;

import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductWishRepository extends JpaRepository<ProductWish, Long> {
    List<ProductWish> findByUser_IdAndStatusOrderByUpdatedAtDesc(Long userId, WishStatus status);

    List<ProductWish> findByUser_IdAndStatusInOrderByUpdatedAtDesc(Long userId, List<WishStatus> statuses);

    Optional<ProductWish> findByIdAndUser_Id(Long id, Long userId);

    List<ProductWish> findByStatusAndExpireAtLessThanEqual(WishStatus status, LocalDateTime targetTime);

    List<ProductWish> findByStatus(WishStatus status);

    Optional<ProductWish> findByIdAndStatus(Long id, WishStatus status);

    @Query("""
            select pw.id
              from ProductWish pw
             where pw.status = :status
            """)
    List<Long> findIdsByStatus(@Param("status") WishStatus status);

    @Query("""
            select pw.user.id as userId,
                   sum(pw.savedAmount) as totalSavedAmount,
                   max(pw.id) as representativeWishId
              from ProductWish pw
             where pw.status = :status
               and pw.expiredAt >= :start
               and pw.expiredAt < :end
               and pw.savedAmount > 0
             group by pw.user.id
            """)
    List<DailySavedAmountSummary> summarizeDailySavedAmountByUser(
            @Param("status") WishStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long countByUser_IdAndStatusAndExpiredAtBetween(Long userId, WishStatus status, LocalDateTime start, LocalDateTime end);

    interface DailySavedAmountSummary {
        Long getUserId();

        Long getTotalSavedAmount();

        Long getRepresentativeWishId();
    }
}
