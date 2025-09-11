package com.example.rev.lb;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LoadBalancer {

    private static final long MAX_AWAIT_TIME_MS = 10_000L;
    private final List<String> list;
    private final int maxElements;
    private final RollingStrategy rollingStrategy;
    private final ReadWriteLock lock;

    public LoadBalancer(RollingStrategy strategy, int maxElements) {
        this.maxElements = maxElements;
        this.rollingStrategy = strategy;
        this.list = new LinkedList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public boolean register(String url) {
        try {
            tryWriteLock();
            if (list.size() >= maxElements) {
                return false;
            }
            if (list.contains(url)) {
                return false;
            }
            return list.add(url);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlockWriteLock();
        }
    }
    public boolean unregister(String url) {
        try {
            tryWriteLock();
            if (!list.contains(url)) {
                return false;
            }
            return list.remove(url);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlockWriteLock();
        }
    }

    public String next() {
        try {
            tryReadLock();
            return rollingStrategy.next(list);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlockReadLock();
        }
    }

    private void unlockWriteLock() {
        lock.writeLock().unlock();
    }

    private void tryWriteLock() throws InterruptedException {
        if (!lock.writeLock().tryLock(MAX_AWAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Could not acquire write lock");
        }
    }

    private void unlockReadLock() {
        lock.readLock().unlock();
    }

    private void tryReadLock() throws InterruptedException {
        if (!lock.readLock().tryLock(MAX_AWAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Could not acquire read lock");
        }
    }
}
