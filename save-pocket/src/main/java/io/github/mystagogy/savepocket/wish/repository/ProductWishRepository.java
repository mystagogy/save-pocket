package io.github.mystagogy.savepocket.wish.repository;

import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductWishRepository extends JpaRepository<ProductWish, Long> {
    List<ProductWish> findByUser_IdAndStatusOrderByUpdatedAtDesc(Long userId, WishStatus status);

    List<ProductWish> findByUser_IdAndStatusInOrderByUpdatedAtDesc(Long userId, List<WishStatus> statuses);

    Optional<ProductWish> findByIdAndUser_Id(Long id, Long userId);

    List<ProductWish> findByStatusAndExpireAtLessThanEqual(WishStatus status, LocalDateTime targetTime);

    List<ProductWish> findByStatus(WishStatus status);

    long countByUser_IdAndStatusAndExpiredAtBetween(Long userId, WishStatus status, LocalDateTime start, LocalDateTime end);
}
