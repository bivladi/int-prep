package com.example.demo;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*

1. Load balancer
- allows to register 10 instances
- should be thread safe
- tests

 */
public class LoadBalancer<T> {

    private final List<T> items;
    private final AtomicInteger index;
    private final int sizeLimit;
    private final ReadWriteLock lock;
    private final long awaitMs;

    public LoadBalancer(int sizeLimit, long awaitMs) {
        this.sizeLimit = sizeLimit;
        this.index = new AtomicInteger(0);
        this.items = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.awaitMs = awaitMs;
    }

    public boolean register(T item) {
        try {
            if (!lock.writeLock().tryLock(awaitMs, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timeout exceeded");
            }
            if (items.size() >= sizeLimit) {
                return false;
            }
            return items.add(item);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean unregister(T item) {
        try {
            if (!lock.writeLock().tryLock(awaitMs, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timeout exceeded");
            }
            return items.remove(item);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public T next() {
        try {
            if (!lock.readLock().tryLock(awaitMs, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timeout exceeded");
            }
            if (items.isEmpty()) {
                throw new RuntimeException("List is empty");
            }
            final var idx = index.get() == Integer.MAX_VALUE ? index.getAndIncrement() : index.getAndIncrement() % items.size();
            return items.get(idx);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            lock.readLock().unlock();
        }
    }
}
