package io.github.mystagogy.savepocket.wish.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@ConditionalOnProperty(prefix = "wish.events.kafka", name = "enabled", havingValue = "true")
public class KafkaWishDomainEventPublisher implements WishDomainEventPublisher {

    private final KafkaTemplate<String, WishDomainEvent> kafkaTemplate;
    private final String topic;

    public KafkaWishDomainEventPublisher(
            KafkaTemplate<String, WishDomainEvent> kafkaTemplate,
            @Value("${wish.events.kafka.topic:wish.events.v1}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publishAfterCommit(WishDomainEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(event);
                }
            });
            return;
        }

        publish(event);
    }

    private void publish(WishDomainEvent event) {
        String key = event.wishId() != null
                ? String.valueOf(event.wishId())
                : event.eventId();
        kafkaTemplate.send(topic, key, event);
    }
}
