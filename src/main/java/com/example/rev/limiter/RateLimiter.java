package com.example.rev.limiter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class RateLimiter {

    private final ConcurrentHashMap<String, Long> ipToCountMap;
    private final long limit;
    private final ScheduledExecutorService executorService;

    public RateLimiter(Long limit, Long period, TimeUnit timeUnit) {
        if (Objects.isNull(limit) || Objects.isNull(period) || Objects.isNull(timeUnit)) {
            throw new IllegalArgumentException("Arguments must be not null");
        }
        if (limit <= 0 || period <= 0) {
            throw new IllegalArgumentException("Limit and period must be more than zero");
        }
        this.limit = limit;
        ipToCountMap = new ConcurrentHashMap<>();
        this.executorService = Executors.newScheduledThreadPool(1);
        this.executorService.scheduleAtFixedRate(
                this::resetCounts,
                0,
                period,
                timeUnit
        );
    }

    private void resetCounts() {
        ipToCountMap.clear();
    }

    public boolean hasAccess(String userIp) {
        if (Objects.isNull(userIp) || userIp.isBlank()) {
            throw new IllegalArgumentException("parameter must be not blank");
        }
        final var count = ipToCountMap.merge(userIp, 1L, Long::sum);
        return count <= limit;
    }

    public void shutdown() {
        this.executorService.shutdown();
    }
}
