package io.github.mystagogy.savepocket.analytics.service;

import io.github.mystagogy.savepocket.analytics.entity.WishEventAnalyticsCheckpoint;
import io.github.mystagogy.savepocket.analytics.entity.WishEventAnalyticsDaily;
import io.github.mystagogy.savepocket.analytics.entity.WishEventAnalyticsMonthly;
import io.github.mystagogy.savepocket.analytics.repository.WishEventAnalyticsCheckpointRepository;
import io.github.mystagogy.savepocket.analytics.repository.WishEventAnalyticsDailyRepository;
import io.github.mystagogy.savepocket.analytics.repository.WishEventAnalyticsMonthlyRepository;
import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.events.WishDomainEvent;
import io.github.mystagogy.savepocket.wish.events.WishDomainEventType;
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishEventAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(WishEventAnalyticsService.class);
    private static final Set<WishDomainEventType> AGGREGATABLE_EVENT_TYPES = EnumSet.of(
            WishDomainEventType.WISH_EXPIRED,
            WishDomainEventType.WISH_PURCHASED
    );

    private final WishEventAnalyticsDailyRepository dailyRepository;
    private final WishEventAnalyticsMonthlyRepository monthlyRepository;
    private final WishEventAnalyticsCheckpointRepository checkpointRepository;
    private final ProductWishRepository productWishRepository;

    public WishEventAnalyticsService(
            WishEventAnalyticsDailyRepository dailyRepository,
            WishEventAnalyticsMonthlyRepository monthlyRepository,
            WishEventAnalyticsCheckpointRepository checkpointRepository,
            ProductWishRepository productWishRepository
    ) {
        this.dailyRepository = dailyRepository;
        this.monthlyRepository = monthlyRepository;
        this.checkpointRepository = checkpointRepository;
        this.productWishRepository = productWishRepository;
    }

    @Transactional
    public void aggregateFromEvent(WishDomainEvent event) {
        if (event == null || !AGGREGATABLE_EVENT_TYPES.contains(event.eventType())) {
            return;
        }

        if (event.eventId() == null || event.userId() == null || event.occurredAt() == null || event.wishId() == null) {
            log.warn("집계 필수값이 없어 이벤트를 건너뜁니다. eventId={}, eventType={}",
                    event != null ? event.eventId() : null,
                    event != null ? event.eventType() : null
            );
            return;
        }

        if (checkpointRepository.existsById(event.eventId())) {
            return;
        }

        Optional<ProductWish> wishOptional = productWishRepository.findById(event.wishId());
        if (wishOptional.isEmpty()) {
            log.warn("집계 대상 위시를 찾을 수 없어 이벤트를 건너뜁니다. eventId={}, wishId={}", event.eventId(), event.wishId());
            return;
        }

        ProductWish wish = wishOptional.get();
        long amount = resolveAmount(event, wish);

        LocalDate statDate = event.occurredAt().toLocalDate();
        YearMonth statMonth = YearMonth.from(statDate);

        WishEventAnalyticsDaily daily = dailyRepository.findByUserIdAndStatDate(event.userId(), statDate)
                .orElseGet(() -> newDaily(event.userId(), statDate));
        WishEventAnalyticsMonthly monthly = monthlyRepository.findByUserIdAndStatYearAndStatMonth(
                        event.userId(),
                        statMonth.getYear(),
                        statMonth.getMonthValue()
                )
                .orElseGet(() -> newMonthly(event.userId(), statMonth));

        if (event.eventType() == WishDomainEventType.WISH_EXPIRED) {
            daily.applyExpired(amount);
            monthly.applyExpired(amount);
        } else if (event.eventType() == WishDomainEventType.WISH_PURCHASED) {
            daily.applyPurchased(amount);
            monthly.applyPurchased(amount);
        }

        dailyRepository.save(daily);
        monthlyRepository.save(monthly);
        checkpointRepository.save(newCheckpoint(event.eventId(), event.eventType(), LocalDateTime.now()));
    }

    private long resolveAmount(WishDomainEvent event, ProductWish wish) {
        long resolved;
        if (event.eventType() == WishDomainEventType.WISH_EXPIRED) {
            Long base = wish.getSavedAmount();
            if (base == null) {
                base = event.currentReferencePrice();
            }
            if (base == null) {
                base = wish.effectivePrice();
            }
            resolved = base != null ? base : 0L;
        } else {
            Long base = wish.effectivePrice();
            if (base == null) {
                base = event.currentReferencePrice();
            }
            resolved = base != null ? base : 0L;
        }

        return Math.max(resolved, 0L);
    }

    private WishEventAnalyticsDaily newDaily(Long userId, LocalDate statDate) {
        WishEventAnalyticsDaily daily = new WishEventAnalyticsDaily();
        daily.setUserId(userId);
        daily.setStatDate(statDate);
        return daily;
    }

    private WishEventAnalyticsMonthly newMonthly(Long userId, YearMonth statMonth) {
        WishEventAnalyticsMonthly monthly = new WishEventAnalyticsMonthly();
        monthly.setUserId(userId);
        monthly.setStatYear(statMonth.getYear());
        monthly.setStatMonth(statMonth.getMonthValue());
        return monthly;
    }

    private WishEventAnalyticsCheckpoint newCheckpoint(String eventId, WishDomainEventType eventType, LocalDateTime processedAt) {
        WishEventAnalyticsCheckpoint checkpoint = new WishEventAnalyticsCheckpoint();
        checkpoint.setEventId(eventId);
        checkpoint.setEventType(eventType.name());
        checkpoint.setProcessedAt(processedAt);
        return checkpoint;
    }
}
