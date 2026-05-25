package io.github.mystagogy.savepocket.wish.events;

import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaWishDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, WishDomainEvent> kafkaTemplate;

    @Test
    void publishAfterCommitPublishesImmediatelyWhenTransactionIsNotActive() {
        KafkaWishDomainEventPublisher publisher = new KafkaWishDomainEventPublisher(
                kafkaTemplate,
                "wish.events.v1"
        );
        WishDomainEvent event = new WishDomainEvent(
                "evt-1",
                WishDomainEventType.WISH_CREATED,
                1,
                LocalDateTime.of(2026, 5, 25, 11, 0),
                10L,
                1L,
                "WAITING",
                null,
                120000L
        );

        publisher.publishAfterCommit(event);

        verify(kafkaTemplate).send("wish.events.v1", "10", event);
    }
}
