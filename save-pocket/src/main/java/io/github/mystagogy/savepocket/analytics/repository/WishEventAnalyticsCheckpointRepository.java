package io.github.mystagogy.savepocket.analytics.repository;

import io.github.mystagogy.savepocket.analytics.entity.WishEventAnalyticsCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishEventAnalyticsCheckpointRepository extends JpaRepository<WishEventAnalyticsCheckpoint, String> {
}
