package io.github.mystagogy.savepocket.report.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ReportCacheService {

    public static final String MONTHLY_SAVINGS_CACHE = "monthlySavings";

    private final CacheManager cacheManager;

    public ReportCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictMonthlySavings(Long userId, int year, int month) {
        Cache cache = cacheManager.getCache(MONTHLY_SAVINGS_CACHE);
        if (cache == null) {
            return;
        }
        cache.evict(monthlyKey(userId, year, month));
    }

    public void evictMonthlySavingsAfterCommit(Long userId, int year, int month) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictMonthlySavings(userId, year, month);
                }
            });
            return;
        }

        evictMonthlySavings(userId, year, month);
    }

    public static String monthlyKey(Long userId, int year, int month) {
        return userId + ":" + year + ":" + month;
    }
}
