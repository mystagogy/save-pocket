package io.github.mystagogy.savepocket.wish.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "wish.events.kafka",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class NoopWishDomainEventPublisher implements WishDomainEventPublisher {

    @Override
    public void publishAfterCommit(WishDomainEvent event) {
        // Kafka가 비활성화된 환경에서는 도메인 이벤트 발행을 건너뛴다.
    }
}
