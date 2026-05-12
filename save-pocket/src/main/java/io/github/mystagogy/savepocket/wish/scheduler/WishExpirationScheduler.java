package io.github.mystagogy.savepocket.wish.scheduler;

import io.github.mystagogy.savepocket.wish.service.WishService;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WishExpirationScheduler {

    private final WishService wishService;

    public WishExpirationScheduler(WishService wishService) {
        this.wishService = wishService;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void expireDueWishes() {
        wishService.expireDueWishes(LocalDateTime.now());
    }
}
