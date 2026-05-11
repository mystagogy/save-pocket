package io.github.mystagogy.savepocket.wish.entity;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wish_id", nullable = false)
    private ProductWish wish;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false, length = 20)
    private PriceType priceType;

    @Column(name = "previous_price", nullable = false)
    private Long previousPrice;

    @Column(name = "changed_price", nullable = false)
    private Long changedPrice;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public ProductWish getWish() {
        return wish;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public Long getPreviousPrice() {
        return previousPrice;
    }

    public Long getChangedPrice() {
        return changedPrice;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setWish(ProductWish wish) {
        this.wish = wish;
    }

    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }

    public void setPreviousPrice(Long previousPrice) {
        this.previousPrice = previousPrice;
    }

    public void setChangedPrice(Long changedPrice) {
        this.changedPrice = changedPrice;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
