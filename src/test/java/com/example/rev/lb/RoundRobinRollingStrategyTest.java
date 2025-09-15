package com.example.rev.lb;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinRollingStrategyTest {

    @Test
    public void testRoll() {
        final var strategy = new RoundRobinRollingStrategy();
        final var elementsAmount = List.of("1", "2", "3");
        for (int i = 0; i < 5; i++) {
            assertEquals("1", strategy.next(elementsAmount));
            assertEquals("2", strategy.next(elementsAmount));
            assertEquals("3", strategy.next(elementsAmount));
        }
    }

    @Test
    public void nextShouldFail() {
        try {
            final var strategy = new RoundRobinRollingStrategy();
            strategy.next(Collections.emptyList());
            fail("Not expected here");
        } catch (Exception e) {
            assertInstanceOf(IllegalArgumentException.class, e);
        }
    }
}