package io.github.mystagogy.savepocket.notification.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "notification.rabbitmq",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class NoopNotificationEventPublisher implements NotificationEventPublisher {

    @Override
    public void publishPriceDropAfterCommit(PriceDropNotificationMessage message) {
        // RabbitMQ가 비활성화된 환경에서는 알림 이벤트 발행을 건너뛴다.
    }
}
