package io.github.mystagogy.savepocket.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishEventAnalyticsServiceTest {

    @Mock
    private WishEventAnalyticsDailyRepository dailyRepository;

    @Mock
    private WishEventAnalyticsMonthlyRepository monthlyRepository;

    @Mock
    private WishEventAnalyticsCheckpointRepository checkpointRepository;

    @Mock
    private ProductWishRepository productWishRepository;

    @Test
    void aggregateFromExpiredEventUpdatesDailyMonthlyAndCheckpoint() {
        WishEventAnalyticsService service = new WishEventAnalyticsService(
                dailyRepository,
                monthlyRepository,
                checkpointRepository,
                productWishRepository
        );
        WishDomainEvent event = new WishDomainEvent(
                "evt-11",
                WishDomainEventType.WISH_EXPIRED,
                1,
                LocalDateTime.of(2026, 5, 26, 10, 30),
                101L,
                1L,
                "EXPIRED",
                null,
                12000L
        );

        ProductWish wish = new ProductWish();
        wish.setSavedAmount(12000L);

        when(checkpointRepository.existsById("evt-11")).thenReturn(false);
        when(productWishRepository.findById(101L)).thenReturn(Optional.of(wish));
        when(dailyRepository.findByUserIdAndStatDate(1L, LocalDate.of(2026, 5, 26))).thenReturn(Optional.empty());
        when(monthlyRepository.findByUserIdAndStatYearAndStatMonth(1L, 2026, 5)).thenReturn(Optional.empty());

        service.aggregateFromEvent(event);

        ArgumentCaptor<WishEventAnalyticsDaily> dailyCaptor = ArgumentCaptor.forClass(WishEventAnalyticsDaily.class);
        ArgumentCaptor<WishEventAnalyticsMonthly> monthlyCaptor = ArgumentCaptor.forClass(WishEventAnalyticsMonthly.class);
        ArgumentCaptor<WishEventAnalyticsCheckpoint> checkpointCaptor = ArgumentCaptor.forClass(WishEventAnalyticsCheckpoint.class);

        verify(dailyRepository).save(dailyCaptor.capture());
        verify(monthlyRepository).save(monthlyCaptor.capture());
        verify(checkpointRepository).save(checkpointCaptor.capture());

        WishEventAnalyticsDaily savedDaily = dailyCaptor.getValue();
        assertThat(savedDaily.getUserId()).isEqualTo(1L);
        assertThat(savedDaily.getStatDate()).isEqualTo(LocalDate.of(2026, 5, 26));
        assertThat(savedDaily.getExpiredCount()).isEqualTo(1);
        assertThat(savedDaily.getExpiredAmount()).isEqualTo(12000L);
        assertThat(savedDaily.getNetAmount()).isEqualTo(12000L);

        WishEventAnalyticsMonthly savedMonthly = monthlyCaptor.getValue();
        assertThat(savedMonthly.getUserId()).isEqualTo(1L);
        assertThat(savedMonthly.getStatYear()).isEqualTo(2026);
        assertThat(savedMonthly.getStatMonth()).isEqualTo(5);
        assertThat(savedMonthly.getExpiredCount()).isEqualTo(1);
        assertThat(savedMonthly.getExpiredAmount()).isEqualTo(12000L);
        assertThat(savedMonthly.getNetAmount()).isEqualTo(12000L);

        WishEventAnalyticsCheckpoint checkpoint = checkpointCaptor.getValue();
        assertThat(checkpoint.getEventId()).isEqualTo("evt-11");
        assertThat(checkpoint.getEventType()).isEqualTo("WISH_EXPIRED");
        assertThat(checkpoint.getProcessedAt()).isNotNull();
    }

    @Test
    void aggregateFromEventSkipsWhenEventAlreadyProcessed() {
        WishEventAnalyticsService service = new WishEventAnalyticsService(
                dailyRepository,
                monthlyRepository,
                checkpointRepository,
                productWishRepository
        );
        WishDomainEvent event = new WishDomainEvent(
                "evt-dup",
                WishDomainEventType.WISH_PURCHASED,
                1,
                LocalDateTime.of(2026, 5, 26, 10, 30),
                102L,
                1L,
                "PURCHASED",
                null,
                9000L
        );

        when(checkpointRepository.existsById("evt-dup")).thenReturn(true);

        service.aggregateFromEvent(event);

        verify(productWishRepository, never()).findById(102L);
        verify(dailyRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(monthlyRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(checkpointRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void aggregateFromExpiredEventUsesEventSnapshotBeforeMutableWishState() {
        WishEventAnalyticsService service = new WishEventAnalyticsService(
                dailyRepository,
                monthlyRepository,
                checkpointRepository,
                productWishRepository
        );
        WishDomainEvent event = new WishDomainEvent(
                "evt-expired-snapshot",
                WishDomainEventType.WISH_EXPIRED,
                1,
                LocalDateTime.of(2026, 5, 26, 12, 0),
                103L,
                1L,
                "EXPIRED",
                null,
                12000L
        );

        ProductWish wish = new ProductWish();
        wish.setReferencePrice(30000L);

        when(checkpointRepository.existsById("evt-expired-snapshot")).thenReturn(false);
        when(productWishRepository.findById(103L)).thenReturn(Optional.of(wish));
        when(dailyRepository.findByUserIdAndStatDate(1L, LocalDate.of(2026, 5, 26))).thenReturn(Optional.empty());
        when(monthlyRepository.findByUserIdAndStatYearAndStatMonth(1L, 2026, 5)).thenReturn(Optional.empty());

        service.aggregateFromEvent(event);

        ArgumentCaptor<WishEventAnalyticsDaily> dailyCaptor = ArgumentCaptor.forClass(WishEventAnalyticsDaily.class);
        verify(dailyRepository).save(dailyCaptor.capture());
        assertThat(dailyCaptor.getValue().getExpiredAmount()).isEqualTo(12000L);
    }
}
