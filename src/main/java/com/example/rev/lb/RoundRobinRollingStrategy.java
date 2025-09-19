package com.example.rev.lb;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinRollingStrategy implements RollingStrategy {

    private final AtomicInteger index;

    public RoundRobinRollingStrategy() {
        this.index = new AtomicInteger(0);
    }

    @Override
    public String next(List<String> elements) {
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("elementsAmount is empty");
        }
        return elements.get(Math.floorMod(index.getAndIncrement(), elements.size()));
    }
}
