package com.example.rev.limiter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/*
    Validates tokens on request, without background thread
 */
public class RateLimiterLazy {

    private final long limit;
    private final Supplier<Long> periodMs;
    private final Map<String, Token> map;

    public RateLimiterLazy(long limit, long periodMs) {
        this(limit, () -> periodMs);
        if (periodMs <= 0) {
            throw new IllegalArgumentException();
        }
    }

    RateLimiterLazy(long limit, Supplier<Long> periodMs) {
        if (limit <= 0 || periodMs == null) {
            throw new IllegalArgumentException();
        }
        this.limit = limit;
        this.periodMs = periodMs;
        this.map = new HashMap<>();
    }

    public boolean tryAcquire(String userIp) {
        if (userIp == null || userIp.isBlank()) {
            throw new IllegalArgumentException();
        }
        final var now = System.currentTimeMillis();
        final var period = periodMs.get();
        final AtomicBoolean result = new AtomicBoolean(true);
//        map.compute(
//            userIp,
//            (k, t) -> {
//                if (t == null) {
//                    result.set(true);
//                    return new Token(1, now);
//                }
//                if (t.count < limit) {
//                    result.set(true);
//                    return new Token(t.count + 1, t.startTime);
//                }
//                if (now - t.startTime >= period) {
//                    result.set(true);
//                    return t;
//                }
//                result.set(false);
//                return t;
//            }
//        );
        map.merge(
            userIp,
            new Token(1, now),
            (oldValue, newValue) -> {
                if (now - oldValue.startTime >= period) {
                    result.set(true);
                    return newValue;
                }
                if (oldValue.count < limit) {
                    result.set(true);
                    return new Token(oldValue.count + 1, oldValue.startTime);
                }
                result.set(false);
                return oldValue;
            }
        );
        return result.get();
    }

    private record Token(long count, long startTime) {
    }
}
