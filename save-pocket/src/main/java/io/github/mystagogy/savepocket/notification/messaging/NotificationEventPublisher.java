package io.github.mystagogy.savepocket.notification.messaging;

public interface NotificationEventPublisher {

    void publishPriceDropAfterCommit(PriceDropNotificationMessage message);
}
