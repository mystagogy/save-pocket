package io.github.mystagogy.savepocket.wish.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitWishPriceRefreshDispatcherTest {

    @Mock
    private ProductWishRepository productWishRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void dispatchPublishesMessagesForWaitingWishIds() {
        WishPriceRefreshRabbitProperties properties = new WishPriceRefreshRabbitProperties(
                "wish.price-refresh.exchange",
                "wish.price-refresh.queue",
                "wish.price-refresh.dlq",
                "wish.price-refresh.request"
        );
        RabbitWishPriceRefreshDispatcher dispatcher = new RabbitWishPriceRefreshDispatcher(
                productWishRepository,
                rabbitTemplate,
                properties
        );
        LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 22, 9, 0);

        when(productWishRepository.findIdsByStatus(WishStatus.WAITING)).thenReturn(List.of(11L, 22L));

        PriceRefreshDispatchResult result = dispatcher.dispatch(requestedAt);

        assertThat(result.scannedCount()).isEqualTo(2);
        assertThat(result.updatedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(0);

        verify(rabbitTemplate).convertAndSend(
                "wish.price-refresh.exchange",
                "wish.price-refresh.request",
                new PriceRefreshMessage(11L, requestedAt)
        );
        verify(rabbitTemplate).convertAndSend(
                "wish.price-refresh.exchange",
                "wish.price-refresh.request",
                new PriceRefreshMessage(22L, requestedAt)
        );
    }
}
