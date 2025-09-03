package com.example.demo;

import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerTest {

    public static final int AWAIT_MS = 100;
    private LoadBalancer<String> loadBalancer;
    private int sizeLimit;

    @BeforeEach
    void setUp() {
        sizeLimit = 5;
        loadBalancer = new LoadBalancer<>(sizeLimit, AWAIT_MS);
    }

    @Test
    void shouldRegisterItem() {
        assertTrue(loadBalancer.register("item"));
    }

    @Test
    void shouldUnregisterItem() {
        assertTrue(loadBalancer.register("item"));
        assertTrue(loadBalancer.unregister("item"));
    }

    @Test
    void shouldNotUnregisterItemTwice() {
        assertTrue(loadBalancer.register("item"));
        assertTrue(loadBalancer.unregister("item"));
        assertFalse(loadBalancer.unregister("item"));
    }

    @Test
    void shouldRegisterNoMoreThanSizeLimitItems() {
        for (int i = 0; i < sizeLimit; i++) {
            assertTrue(loadBalancer.register("item" + i));
        }
        assertFalse(loadBalancer.register("one more"));
    }

    @Test
    void shouldCycleThroughItems() {
        final var one = "1";
        final var two = "2";
        loadBalancer.register(one);
        loadBalancer.register(two);

        assertEquals(one, loadBalancer.next());
        assertEquals(two, loadBalancer.next());
        assertEquals(one, loadBalancer.next());
        assertEquals(two, loadBalancer.next());
    }

    @Test
    void concurrencyTest() throws InterruptedException {
        var size = 10;
        final var nThreads = 1000;
        var subject = new LoadBalancer<String>(size, AWAIT_MS);
        final var executors = Executors.newFixedThreadPool(nThreads);
        final var barrier = new CyclicBarrier(nThreads);
        final var items = Collections.synchronizedSet(new HashSet<>());
        for (int i = 0; i < nThreads; i++) {
            final var item = "" + i;
            executors.submit(() -> {
                try {
                    barrier.await();
                    Thread.sleep(5L);
                    subject.register(item);
                    items.add(subject.next());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
        executors.shutdown();
        assertTrue(executors.awaitTermination(AWAIT_MS, TimeUnit.MILLISECONDS));
        assertTrue(items.size() <= size);
    }
}