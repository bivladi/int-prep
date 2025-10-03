package com.example.rev.cb;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class CircuitBreaker<T> {

    public enum State {
        OPEN, CLOSED
    }

    private final Duration duration;
    private final long failureThreshold;
    private volatile State state;
    private volatile long openUntil;
    private final AtomicLong failures;

    public CircuitBreaker(Duration duration, long failureThreshold) {
        // todo check inputs
        this.state = State.CLOSED;
        this.duration = duration;
        this.openUntil = 0L;
        this.failureThreshold = failureThreshold;
        this.failures = new AtomicLong(0);
    }

    public T execute(
            Supplier<T> supplier,
            Supplier<T> fallback
    ) {
        if (supplier == null || fallback == null) {
            throw new IllegalArgumentException("supplier/fallback must not be null");
        }
        final var now = System.currentTimeMillis();
        if (state == State.OPEN) {
            if (now < openUntil) {
                return fallback.get();
            }
            state = State.CLOSED;
            openUntil = 0L;
        }
        try {
            final var result = supplier.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(now);
            return fallback.get();
        }
    }

    private void onFailure(long now) {
        if (failures.addAndGet(1) == failureThreshold) {
            state = State.OPEN;
            openUntil = now + duration.getSeconds();
        }
    }

    private void onSuccess() {
        this.state = State.CLOSED;
        this.failures.set(0);
        this.openUntil = 0L;
    }
}
