package com.example.rev.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    /**
     * Коротко: твой тест делает ассерт до того, как потоки закончат работу. Отсюда и «неработает»/флаки. Нужно дождаться завершения задач (и тем самым получить корректную видимость памяти), а уже потом читать баланс.
     *
     * Минимальная починка теста:
     *
     * @Test
     * public void concurrentDepositWithdraw() throws Exception {
     *     final var account = new Account(new BigDecimal("1000"));
     *     final var executor = Executors.newFixedThreadPool(2);
     *
     *     var f1 = executor.submit(() -> {
     *         for (int i = 0; i < 10_000; i++) {
     *             account.deposit(BigDecimal.TEN);
     *         }
     *         return null;
     *     });
     *     var f2 = executor.submit(() -> {
     *         for (int i = 0; i < 10_000; i++) {
     *             account.withdraw(BigDecimal.TEN);
     *         }
     *         return null;
     *     });
     *
     *     // Гарантируем завершение и HB-отношение (happens-before)
     *     f1.get();
     *     f2.get();
     *     executor.shutdown();
     *
     *     assertEquals(new BigDecimal("1000"), account.getBalance());
     * }
     *
     *
     * Почему это важно:
     *
     * Без get()/awaitTermination() ассерт может выполниться, пока операции ещё идут, либо кэш/регистр не «протолкнул» обновления (нет happens-before).
     *
     * В твоём Account геттер возвращает поле напрямую, без синхронизации; после Future.get() это ок (есть HB), но без ожидания результат может быть устаревшим.
     *
     * Проверь ещё два момента (на случай, если флаки останутся):
     *
     * Внутри deposit/withdraw не должно быть логики, которая иногда «пропускает» операцию при неудачном tryLock(timeout). Если такое есть — либо лочим без tryLock, либо повторяем попытку в цикле.
     *
     * Используй константы BigDecimal.TEN/строки (new BigDecimal("1000")) — это не причина бага, но хороший тон для денежной математики.
     *
     * С этой правкой тест станет детерминированным и зелёным при корректной реализации Account.
     * @throws InterruptedException
     */
    @Test
    public void concurrentDepositWithdraw() throws InterruptedException {
        final var account = new Account(new BigDecimal(1000));
        final var executorService = Executors.newFixedThreadPool(2);
        final Future<?> f1 = executorService.submit(() -> {
            for (int i = 0; i < 10000; i++) {
                account.deposit(new BigDecimal(10));
            }
        });
        final Future<?> submit = executorService.submit(() -> {
            for (int i = 0; i < 10000; i++) {
                account.withdraw(new BigDecimal(10));
            }
        });
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(1000, TimeUnit.MILLISECONDS));
        assertEquals(new BigDecimal(1000), account.getBalance());
    }
}