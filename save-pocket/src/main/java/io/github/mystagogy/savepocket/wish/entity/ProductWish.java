package io.github.mystagogy.savepocket.wish.entity;

import io.github.mystagogy.savepocket.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_wish")
public class ProductWish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_url", nullable = false, columnDefinition = "TEXT")
    private String productUrl;

    @Column(name = "tracked_product_id", length = 100)
    private String trackedProductId;

    @Column(name = "product_image_url", columnDefinition = "TEXT")
    private String productImageUrl;

    @Column(length = 500)
    private String memo;

    @Column(name = "reference_price")
    private Long referencePrice;

    @Column(name = "user_deal_price")
    private Long userDealPrice;

    @Column(name = "deal_url", columnDefinition = "TEXT")
    private String dealUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_source_type", length = 20)
    private DealSourceType dealSourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WishStatus status;

    @Column(name = "first_registered_at", nullable = false)
    private LocalDateTime firstRegisteredAt;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "reactivated_count", nullable = false)
    private Integer reactivatedCount;

    @Column(name = "saved_amount")
    private Long savedAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public String getTrackedProductId() {
        return trackedProductId;
    }

    public String getMemo() {
        return memo;
    }

    public Long getReferencePrice() {
        return referencePrice;
    }

    public Long getUserDealPrice() {
        return userDealPrice;
    }

    public String getDealUrl() {
        return dealUrl;
    }

    public DealSourceType getDealSourceType() {
        return dealSourceType;
    }

    public WishStatus getStatus() {
        return status;
    }

    public LocalDateTime getFirstRegisteredAt() {
        return firstRegisteredAt;
    }

    public LocalDateTime getLastViewedAt() {
        return lastViewedAt;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public Integer getReactivatedCount() {
        return reactivatedCount;
    }

    public Long getSavedAmount() {
        return savedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public void setTrackedProductId(String trackedProductId) {
        this.trackedProductId = trackedProductId;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setReferencePrice(Long referencePrice) {
        this.referencePrice = referencePrice;
    }

    public void setUserDealPrice(Long userDealPrice) {
        this.userDealPrice = userDealPrice;
    }

    public void setDealUrl(String dealUrl) {
        this.dealUrl = dealUrl;
    }

    public void setDealSourceType(DealSourceType dealSourceType) {
        this.dealSourceType = dealSourceType;
    }

    public void setStatus(WishStatus status) {
        this.status = status;
    }

    public void setFirstRegisteredAt(LocalDateTime firstRegisteredAt) {
        this.firstRegisteredAt = firstRegisteredAt;
    }

    public void setLastViewedAt(LocalDateTime lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public void setReactivatedCount(Integer reactivatedCount) {
        this.reactivatedCount = reactivatedCount;
    }

    public void setSavedAmount(Long savedAmount) {
        this.savedAmount = savedAmount;
    }

    public Long effectivePrice() {
        return userDealPrice != null ? userDealPrice : referencePrice;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.reactivatedCount == null) {
            this.reactivatedCount = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
