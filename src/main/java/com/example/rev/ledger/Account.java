package com.example.rev.ledger;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    private BigDecimal balance;
    private final ReentrantLock lock;

    public Account() {
        this(BigDecimal.ZERO);
    }

    public Account(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException();
        }
        this.balance = balance;
        lock = new ReentrantLock();
    }

    public synchronized boolean withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        try {
            if (!lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
                return false;
            }
            if (balance.compareTo(amount) < 0) {
                return false;
            }
            this.balance = this.balance.subtract(amount);
            return true;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public synchronized boolean deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        try {
            if (!lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
                return false;
            }
            this.balance = this.balance.add(amount);
            return true;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public Lock getLock() {
        return this.lock;
    }
}
