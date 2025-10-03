package com.example.rev.cb;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class SimpleCircuitBreaker<T> {
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;          // напр. 5
    private final Duration openDuration;         // напр. 30s
    private final Clock clock;

    private volatile State state = State.CLOSED;
    private volatile long openUntil = 0L;
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicInteger halfOpenAllowed = new AtomicInteger(1); // 1 проба в HALF_OPEN

    public SimpleCircuitBreaker(int failureThreshold, Duration openDuration) {
        this(failureThreshold, openDuration, Clock.systemUTC());
    }

    SimpleCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        if (failureThreshold <= 0) throw new IllegalArgumentException("failureThreshold");
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    public T call(Supplier<T> supplier, Supplier<T> fallback) {
        long now = clock.millis();

        // переход из OPEN в HALF_OPEN по таймеру
        if (state == State.OPEN) {
            if (now < openUntil) {
                return fallback.get();
            }
            state = State.HALF_OPEN;
            failures.set(0);
            halfOpenAllowed.set(1);
        }

        // в HALF_OPEN разрешаем одну «пробную» попытку
        if (state == State.HALF_OPEN && halfOpenAllowed.getAndDecrement() <= 0) {
            return fallback.get();
        }

        try {
            T res = supplier.get();
            onSuccess();
            return res;
        } catch (RuntimeException e) {
            onFailure(now);
            if (fallback != null) return fallback.get();
            throw e;
        }
    }

    private void onSuccess() {
        if (state == State.HALF_OPEN) {
            // удачная проба — закрываем
            state = State.CLOSED;
        }
        failures.set(0);
    }

    private void onFailure(long now) {
        int f = failures.incrementAndGet();
        if (state == State.HALF_OPEN || f >= failureThreshold) {
            state = State.OPEN;
            openUntil = now + openDuration.toMillis();
        }
    }

    public State state() { return state; }
}
