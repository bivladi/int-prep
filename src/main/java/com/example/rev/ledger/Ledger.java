package com.example.rev.ledger;

import java.math.BigDecimal;
import java.util.Objects;

public class Ledger {

    public boolean transfer(Account from, Account to, BigDecimal amount) {
        if (Objects.isNull(from) || Objects.isNull(to) || Objects.isNull(amount)) {
            throw new IllegalArgumentException();
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        var fromLock = from.id < to.id ? from.lock : to.lock;
        var toLock = from.id < to.id ? to.lock : from.lock;
        fromLock.lock();
        toLock.lock();
        try {
            if (!from.withdraw(amount)) {
                return false;
            }
            to.deposit(amount);
            return true;
        } finally {
            toLock.unlock();
            fromLock.unlock();
        }
    }
}
