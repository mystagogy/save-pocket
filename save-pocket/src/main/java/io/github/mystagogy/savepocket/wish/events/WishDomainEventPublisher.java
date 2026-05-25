package io.github.mystagogy.savepocket.wish.events;

public interface WishDomainEventPublisher {

    void publishAfterCommit(WishDomainEvent event);
}
