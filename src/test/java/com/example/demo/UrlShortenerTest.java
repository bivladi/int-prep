package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerTest {

    private UrlShortener urlShortener;

    @BeforeEach
    void setUp() {
        urlShortener = new UrlShortener();
    }

    @Test
    public void shortLengthShouldBeLessOrEqualToSixSymbols() {
        final var shorted = urlShortener.getShort("original url");
        assertTrue(shorted.length() <= 6);
    }

    @Test
    public void shouldGetOriginalByShort() {
        final var expected = "original url";
        final var shorted = urlShortener.getShort(expected);
        final var actual = urlShortener.getOriginal(shorted);
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    public void shouldReturnDifferentValuesForSameInput() {
        final var original = "original url";
        final var shorted1 = urlShortener.getShort(original);
        final var shorted2 = urlShortener.getShort(original);
        final var shorted3 = urlShortener.getShort(original);

        assertNotEquals(shorted1, shorted2);
        assertNotEquals(shorted2, shorted3);
        assertNotEquals(shorted1, shorted3);
    }

    @Test
    public void shouldNotContainOriginal() {
        final var original = "abc";
        final var shorted = urlShortener.getShort(original);
        assertFalse(original.contains(shorted));
        assertFalse(shorted.contains(original));
    }

    @Test
    public void concurrencyTest() throws InterruptedException {
        final var nThread = 100;
        final var urlCount = 10;
        final var executors = Executors.newFixedThreadPool(nThread);
        final var shortedResult = Collections.synchronizedSet(new HashSet<>());
        final var originalResult = Collections.synchronizedSet(new HashSet<>());
        final var exceptions = Collections.synchronizedList(new LinkedList<>());
        final Runnable runnable = () -> {
            for (int i = 0; i < urlCount; i++) {
                try {
                    final var shorted = urlShortener.getShort("" + i);
                    shortedResult.add(shorted);
                    originalResult.add(urlShortener.getOriginal(shorted));
                } catch (Exception ex) {
                    exceptions.add(ex);
                    throw ex;
                }
            }
        };
        for (int i = 0; i < nThread; i++) {
            executors.execute(runnable);
        }
        executors.shutdown();
        assertTrue(executors.awaitTermination(10000, TimeUnit.MILLISECONDS));
        assertEquals(0, exceptions.size());
        assertTrue(shortedResult.size() <= nThread * urlCount);
        assertTrue(originalResult.size() <= nThread * urlCount);
    }

}