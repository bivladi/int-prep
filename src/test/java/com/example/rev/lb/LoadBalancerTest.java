package com.example.rev.lb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerTest {

    private static final int MAX_ELEMENTS = 10;
    private LoadBalancer loadBalancer;

    @BeforeEach
    void setUp() {
        loadBalancer = new LoadBalancer(new RoundRobinRollingStrategy(), MAX_ELEMENTS);
    }

    @Test
    public void register() {
        assertTrue(loadBalancer.register("1"));
    }

    @Test
    public void registerShouldNotAcceptMoreThanMaxElement() {
        for (int i = 0; i < MAX_ELEMENTS; i++) {
            assertTrue(loadBalancer.register(Integer.toString(i)));
        }
        assertFalse(loadBalancer.register("11"));
    }

    @Test
    public void registerShouldReturnFalseIfAlreadyRegistered() {
        loadBalancer.register("1");
        assertFalse(loadBalancer.register("1"));
    }

    @Test
    public void unregister() {
        loadBalancer.register("1");
        assertTrue(loadBalancer.unregister("1"));
    }

    @Test
    public void unregisterShouldReturnFalseIfNotRegistered() {
        loadBalancer.register("1");
        assertFalse(loadBalancer.unregister("2"));
    }

    @Test
    public void next() {
        final var one = "1";
        loadBalancer.register(one);
        assertEquals(one, loadBalancer.next());
    }

    @Test
    public void nextShouldRoll() {
        final var one = "1";
        final var two = "2";
        final var three = "3";
        loadBalancer.register(one);
        loadBalancer.register(two);
        loadBalancer.register(three);

        for (int i = 0; i < 10; i++) {
            assertEquals(one, loadBalancer.next());
            assertEquals(two, loadBalancer.next());
            assertEquals(three, loadBalancer.next());
        }
    }

    @Test
    public void nextShouldRollAfterUnregister() {
        final var one = "1";
        final var two = "2";
        final var three = "3";
        final var four = "4";
        final var rolls = 5;
        loadBalancer.register(one);
        loadBalancer.register(two);
        loadBalancer.register(three);
        loadBalancer.register(four);
        for (int i = 0; i < rolls; i++) {
            assertEquals(one, loadBalancer.next());
            assertEquals(two, loadBalancer.next());
            assertEquals(three, loadBalancer.next());
            assertEquals(four, loadBalancer.next());
        }
        loadBalancer.unregister(two);
        loadBalancer.unregister(three);
        for (int i = 0; i < rolls; i++) {
            assertEquals(one, loadBalancer.next());
            assertEquals(four, loadBalancer.next());
        }
        loadBalancer.unregister(four);
        for (int i = 0; i < rolls; i++) {
            assertEquals(one, loadBalancer.next());
        }
    }

    @Test
    public void nextShouldHandleConcurrency() throws InterruptedException {
        final var maxElements = 40;
        loadBalancer = new LoadBalancer(new RoundRobinRollingStrategy(), maxElements);
        for (int i = 0; i < maxElements; i++) {
            loadBalancer.register(Integer.toString(i));
        }
        final var threadN = 10;
        CountDownLatch countDownLatch = new CountDownLatch(threadN);
        final var list = Collections.synchronizedList(new ArrayList<String>());
        final var executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < threadN; i++) {
            executorService.submit(getRunnable(list, countDownLatch));
        }
        assertTrue(countDownLatch.await(10, TimeUnit.SECONDS));
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(100, TimeUnit.MILLISECONDS));

        assertEquals(30, list.size());
        list.stream().collect(Collectors.groupingBy(it -> it, Collectors.counting())).forEach((k, v) -> {
            assertEquals(1, v);
        });
    }

    private Runnable getRunnable(List<String> list, CountDownLatch countDownLatch) {
        return () -> {
            for (int i = 0; i < 3; i++) {
                final var next = loadBalancer.next();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                list.add(next);
            }
            countDownLatch.countDown();
        };
    }

    @Test
    public void registerAndUnregisterShouldHandleConcurrency() throws InterruptedException {
        loadBalancer = new LoadBalancer(new RoundRobinRollingStrategy(), 40);
        final var executorService = Executors.newFixedThreadPool(3);
        AtomicBoolean success = new AtomicBoolean(true);
        executorService.submit(() -> {
            for (int i = 0; i < 20; i++) {
                sleep();
                success.compareAndSet(true, loadBalancer.register(Integer.toString(i)));
            }
            for (int i = 0; i < 20; i++) {
                sleep();
                success.compareAndSet(true, loadBalancer.unregister(Integer.toString(i)));
            }
        });
        executorService.submit(() -> {
            for (int i = 20; i < 40; i++) {
                sleep();
                success.compareAndSet(true, loadBalancer.register(Integer.toString(i)));
            }
            for (int i = 20; i < 40; i++) {
                sleep();
                success.compareAndSet(true, loadBalancer.unregister(Integer.toString(i)));
            }
        });
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(success.get());
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
    }
}