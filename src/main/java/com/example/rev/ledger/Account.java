package com.example.rev.ledger;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    public int id;
    public ReentrantLock lock;
    private BigDecimal balance;

    public Account(int id) {
        this(id, BigDecimal.ZERO);
    }

    public Account(int id, BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException();
        }
        this.id = id;
        this.lock = new ReentrantLock();
        this.balance = balance;
    }

    public boolean withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        lock.lock();
        try {
            if (balance.compareTo(amount) < 0) {
                return false;
            }
            this.balance = this.balance.subtract(amount);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        lock.lock();
        try {
            this.balance = this.balance.add(amount);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public BigDecimal getBalance() {
        return this.balance;
    }
}
