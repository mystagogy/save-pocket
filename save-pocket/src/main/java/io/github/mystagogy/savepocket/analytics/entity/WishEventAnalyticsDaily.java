package io.github.mystagogy.savepocket.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wish_event_analytics_daily",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_wish_event_analytics_daily_user_date",
                columnNames = {"user_id", "stat_date"}
        )
)
public class WishEventAnalyticsDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "expired_count", nullable = false)
    private int expiredCount;

    @Column(name = "purchased_count", nullable = false)
    private int purchasedCount;

    @Column(name = "expired_amount", nullable = false)
    private long expiredAmount;

    @Column(name = "purchased_amount", nullable = false)
    private long purchasedAmount;

    @Column(name = "net_amount", nullable = false)
    private long netAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public int getExpiredCount() {
        return expiredCount;
    }

    public int getPurchasedCount() {
        return purchasedCount;
    }

    public long getExpiredAmount() {
        return expiredAmount;
    }

    public long getPurchasedAmount() {
        return purchasedAmount;
    }

    public long getNetAmount() {
        return netAmount;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public void applyExpired(long amount) {
        this.expiredCount += 1;
        this.expiredAmount += amount;
        this.netAmount = this.expiredAmount - this.purchasedAmount;
    }

    public void applyPurchased(long amount) {
        this.purchasedCount += 1;
        this.purchasedAmount += amount;
        this.netAmount = this.expiredAmount - this.purchasedAmount;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
