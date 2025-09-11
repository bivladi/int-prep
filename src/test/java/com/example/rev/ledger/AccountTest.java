package com.example.rev.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    public void accountBalanceHaveToBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Account(new BigDecimal(-10)));
    }

    @Test
    public void withdrawShouldDecreaseBalance() {
        final var account = new Account(new BigDecimal(100));
        assertTrue(account.withdraw(new BigDecimal(50)));
        assertEquals(new BigDecimal(50), account.getBalance());
    }

    @Test
    public void withdrawShouldReturnFalseWhenNegativeAmount() {
        final var account = new Account(new BigDecimal(100));
        assertFalse(account.withdraw(new BigDecimal(-50)));
        assertEquals(new BigDecimal(100), account.getBalance());
    }

    @Test
    public void withdrawShouldReturnFalseWhenAmountIsBiggerThanBalance() {
        final var account = new Account(new BigDecimal(10));
        assertFalse(account.withdraw(new BigDecimal(50)));
        assertEquals(new BigDecimal(10), account.getBalance());
    }

    @Test
    public void depositShouldIncreaseBalance() {
        final var account = new Account();
        assertTrue(account.deposit(new BigDecimal(100)));
        assertEquals(new BigDecimal(100), account.getBalance());
    }

    @Test
    public void depositShouldReturnFalseWhenAmountIsNegative() {
        final var account = new Account();
        assertFalse(account.deposit(new BigDecimal(-10)));
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    public void concurrentDepositWithdraw() {
        final var account = new Account(new BigDecimal(1000));
        final var executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            for (int i = 0; i < 10000; i++) {
                account.deposit(new BigDecimal(10));
            }
        });
        executorService.submit(() -> {
            for (int i = 0; i < 10000; i++) {
                account.withdraw(new BigDecimal(10));
            }
        });
        assertEquals(new BigDecimal(1000), account.getBalance());
    }
}