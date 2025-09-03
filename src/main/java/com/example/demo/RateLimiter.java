package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
    private final Map<Ip, Long> ipToRequestCountMap;
    private final Map<Ip, Long> ipToLastUpdateMap;
    private final Long limit;

    public RateLimiter(long limit, long period, TimeUnit timeUnit) {
        ipToRequestCountMap = new ConcurrentHashMap<>();
        ipToLastUpdateMap = new ConcurrentHashMap<>();
        this.limit = limit;
        ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
        service.scheduleAtFixedRate(
            () -> {
                log.debug("Update all tokens in map");
                ipToRequestCountMap.forEach((key, value) -> ipToRequestCountMap.replace(key, value, 0L));
            },
            period,
            period,
            timeUnit
        );
        final var currentTimeMillis = System.currentTimeMillis();
        service.scheduleAtFixedRate(
            () -> {
                log.debug("Update lastUpdated time per IP");
                final var keysToRemove = ipToLastUpdateMap.entrySet().stream()
                        .filter(it -> currentTimeMillis - it.getValue() >= 3_00)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                log.debug("Keys to remove: {}", keysToRemove);
                keysToRemove.forEach(it -> {
                    ipToLastUpdateMap.remove(it);
                    ipToRequestCountMap.remove(it);
                });
            },
            0L,
            30,
            TimeUnit.SECONDS
        );
    }

    public boolean request(Ip userIp, Resource resource) {
        final var result = ipToRequestCountMap.merge(userIp, 1L, Long::sum);
        if (result > limit) {
            return false;
        }
        ipToLastUpdateMap.put(userIp, System.currentTimeMillis());
        return true;
    }

    public record Ip(String address) {}

    public record Resource(String url) {}

    public record TokenBucket(Ip ip, Long lastUpdated) {}
}
