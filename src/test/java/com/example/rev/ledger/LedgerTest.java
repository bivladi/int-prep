package com.example.rev.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LedgerTest {

    @Test
    public void transferShouldDecreaseFromBalanceAndIncreaseToBalance() {
        final var from = new Account(new BigDecimal(100));
        final var to = new Account();
        final var ledger = new Ledger();
        assertTrue(ledger.transfer(from, to, new BigDecimal(50)));
        assertEquals(new BigDecimal(50), from.getBalance());
        assertEquals(new BigDecimal(50), to.getBalance());
    }

    @Test
    public void transferShouldReturnFalseWhenFromBalanceIsLessThanAmount() {
        final var from = new Account(new BigDecimal(100));
        final var to = new Account(new BigDecimal(50));
        final var ledger = new Ledger();
        assertFalse(ledger.transfer(from, to, new BigDecimal(200)));
        assertEquals(new BigDecimal(100), from.getBalance());
        assertEquals(new BigDecimal(50), to.getBalance());
    }

    @Test
    public void transferShouldReturnFalseWhenAmountIsZero() {
        final var from = new Account(new BigDecimal(100));
        final var to = new Account(new BigDecimal(100));
        final var ledger = new Ledger();
        assertFalse(ledger.transfer(from, to, BigDecimal.ZERO));
        assertEquals(new BigDecimal(100), from.getBalance());
        assertEquals(new BigDecimal(100), to.getBalance());
    }

    @Test
    public void transferShouldReturnFalseWhenAmountIsNegative() {
        final var from = new Account(new BigDecimal(100));
        final var to = new Account(new BigDecimal(100));
        final var ledger = new Ledger();
        assertFalse(ledger.transfer(from, to, new BigDecimal(-100)));
        assertEquals(new BigDecimal(100), from.getBalance());
        assertEquals(new BigDecimal(100), to.getBalance());
    }

    @Test
    public void transferShouldFailOnNulls() {
        final var ledger = new Ledger();
        assertThrows(IllegalArgumentException.class, () -> ledger.transfer(new Account(), new Account(), null));
        assertThrows(IllegalArgumentException.class, () -> ledger.transfer(new Account(), null, new BigDecimal(100)));
        assertThrows(IllegalArgumentException.class, () -> ledger.transfer(null, new Account(), new BigDecimal(100)));
        assertThrows(IllegalArgumentException.class, () -> ledger.transfer(null, null, null));
    }

    @Test
    public void concurrencyShouldHoldInvariants() throws InterruptedException {
        final var acc1 = new Account(new BigDecimal(1000));
        final var acc2 = new Account(new BigDecimal(1000));
        final var acc3 = new Account(new BigDecimal(1000));
        final var ledger = new Ledger();
        final var executorService = Executors.newFixedThreadPool(3);
        executorService.submit(() -> {
            for (int i = 0; i < 100; i++) {
                ledger.transfer(acc1, acc2, new BigDecimal(5));
            }
        });
        executorService.submit(() -> {
            for (int i = 0; i < 100; i++) {
                ledger.transfer(acc2, acc3, new BigDecimal(5));
            }
        });
        executorService.submit(() -> {
            for (int i = 0; i < 100; i++) {
                ledger.transfer(acc3, acc1, new BigDecimal(5));
            }
        });
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(10000, TimeUnit.MILLISECONDS));
        assertEquals(new BigDecimal(3000), acc1.getBalance().add(acc2.getBalance()).add(acc3.getBalance()));
    }

}