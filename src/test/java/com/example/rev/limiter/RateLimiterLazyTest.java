package com.example.rev.limiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterLazyTest {

    private static class PeriodSupplier implements Supplier<Long> {

        private Long value;

        PeriodSupplier(long value) {
            this.value = value;
        }

        @Override
        public Long get() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }

    @Test
    public void tryAcquireShouldReturnFalseWhenExceedLimit() {
        final var limiter = new RateLimiterLazy(10, new PeriodSupplier(1000));
        final var userIp = "localhost";
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
            assertTrue(limiter.tryAcquire(userIp));
        }
        assertFalse(limiter.tryAcquire(userIp));
    }

    @Test
    public void tryAcquireShouldReturnTrueAfterPeriod() {
        final var period = new PeriodSupplier(1000);
        final var limit = 10;
        final var limiter = new RateLimiterLazy(limit, period);
        final var userIp = "userIp";
        for (int j = 0; j < limit; j++) {
            assertTrue(limiter.tryAcquire(userIp));
        }
        assertFalse(limiter.tryAcquire(userIp));
        period.setValue(0);
        assertTrue(limiter.tryAcquire(userIp));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "\t", "\n", "  "})
    public void tryAcquireShouldFailOnBlank(String value) {
        final var limiter = new RateLimiterLazy(10L, 1000);
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(value));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 100",
            "100, 0",
            "0, 0",
            "-10, 10",
            "10, -10",
            "-10, -10"
    })
    public void shouldFailOnLessOrEqualToZeroInputs(long limit, long periodMs) {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiterLazy(limit, periodMs));
    }
}