package com.example.rev.limiter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private RateLimiter rateLimiter;
    private final static long LIMIT = 10L;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(LIMIT, 10L, TimeUnit.SECONDS);
    }

    @AfterEach
    void shutdown() {
        rateLimiter.shutdown();
    }

    @Test
    public void hasAccessShouldBlockMoreThanRateRequest() {
        final var userIp = "localhost";
        for (int i = 0; i < LIMIT; i++) {
            assertTrue(rateLimiter.hasAccess(userIp));
        }
        assertFalse(rateLimiter.hasAccess(userIp));
    }

    @Test
    public void hasAccessShouldUpdateAccessPeriodically() throws InterruptedException {
        final var rateLimiter = new RateLimiter(10L, 10L, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiter.hasAccess("localhost"));
            Thread.sleep(20L);
        }
    }

    @Test
    public void hasAccessShouldFailOnBlank() {
        assertThrows(IllegalArgumentException.class, () -> rateLimiter.hasAccess(null));
        assertThrows(IllegalArgumentException.class, () -> rateLimiter.hasAccess(""));
    }

    @Test
    public void shouldFailIfLimitIsLessOrEqualZero() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0L, 1L, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(-1L, 1L, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldFailIfPeriodIsLessOrEqualZero() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(10L, 0L, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(10L, -1L, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldFailOnNull() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(null, 10L, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(10L, null, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(10L, 10L, null));
    }

    @Test
    public void hasAccessInConcurrency() throws InterruptedException {
        final var nThreads = 10;
        final var nRequests = 100;
        final var rateLimiter = new RateLimiter(1000L, 10L, TimeUnit.SECONDS);
        final var count = new AtomicInteger(0);
        final var result = new AtomicBoolean(true);
        final var executorService = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            final var idx = i;
            executorService.submit(() -> {
                for (int j = 0; j < nRequests; j++) {
                    result.compareAndSet(true, rateLimiter.hasAccess("localhost"));
                    count.incrementAndGet();
                }
            });
        }
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(100, TimeUnit.MILLISECONDS));
        assertTrue(result.get());
        assertEquals(nRequests * nThreads, count.get());
    }
}