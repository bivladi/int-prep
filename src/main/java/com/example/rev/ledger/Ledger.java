package com.example.rev.ledger;

import java.math.BigDecimal;
import java.util.Objects;

public class Ledger {

    public Ledger() {
    }

    public boolean transfer(Account from, Account to, BigDecimal amount) {
        if (Objects.isNull(from) || Objects.isNull(to) || Objects.isNull(amount)) {
            throw new IllegalArgumentException();
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (!from.withdraw(amount)) {
            return false;
        }
        to.deposit(amount);
        return true;
    }
}
