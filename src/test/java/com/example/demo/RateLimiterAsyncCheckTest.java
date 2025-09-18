package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

class RateLimiterAsyncCheckTest {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterAsyncCheckTest.class);

    @Test
    public void test() throws InterruptedException {
        final var rateLimiter = new RateLimiter(3, 100, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(2);
            final var userIp = new RateLimiter.Ip("" + (i % 5));
            final var result = rateLimiter.request(
                    userIp,
                    new RateLimiter.Resource("url")
            );
            log.info("{} - {}", userIp, result);
        }
    }
}