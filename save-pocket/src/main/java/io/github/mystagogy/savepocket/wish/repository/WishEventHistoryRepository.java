package io.github.mystagogy.savepocket.wish.repository;

import io.github.mystagogy.savepocket.wish.entity.WishEventHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishEventHistoryRepository extends JpaRepository<WishEventHistory, Long> {
    List<WishEventHistory> findTop50ByWishIdOrderByEventAtDesc(Long wishId);
}
