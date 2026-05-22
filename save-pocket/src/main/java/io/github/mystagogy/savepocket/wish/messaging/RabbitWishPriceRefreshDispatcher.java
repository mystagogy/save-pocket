package io.github.mystagogy.savepocket.wish.messaging;

import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "wish.price-refresh.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitWishPriceRefreshDispatcher implements WishPriceRefreshDispatcher {

    private final ProductWishRepository productWishRepository;
    private final RabbitTemplate rabbitTemplate;
    private final WishPriceRefreshRabbitProperties properties;

    public RabbitWishPriceRefreshDispatcher(
            ProductWishRepository productWishRepository,
            RabbitTemplate rabbitTemplate,
            WishPriceRefreshRabbitProperties properties
    ) {
        this.productWishRepository = productWishRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public PriceRefreshDispatchResult dispatch(LocalDateTime requestedAt) {
        List<Long> waitingWishIds = productWishRepository.findIdsByStatus(WishStatus.WAITING);

        int scannedCount = waitingWishIds.size();
        int updatedCount = 0;
        int failedCount = 0;

        for (Long wishId : waitingWishIds) {
            try {
                rabbitTemplate.convertAndSend(
                        properties.exchange(),
                        properties.routingKey(),
                        new PriceRefreshMessage(wishId, requestedAt)
                );
                updatedCount++;
            } catch (RuntimeException ex) {
                failedCount++;
            }
        }

        return new PriceRefreshDispatchResult(scannedCount, updatedCount, 0, failedCount);
    }
}
