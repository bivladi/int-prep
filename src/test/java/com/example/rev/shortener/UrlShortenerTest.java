package com.example.rev.shortener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerTest {

    private UrlShortener shortener;

    @BeforeEach
    void setUp() {
        shortener = new UrlShortener();
    }

    @Test
    public void getShortShouldReturnFixedLength() {
        final var url1 = "http://some-long-url-1.com/aa/bb";
        final var url2 = "http://some-long-url-2.com/aa/bb";
        String shortUrl1 = shortener.getShort(url1);
        String shortUrl2 = shortener.getShort(url2);
        assertNotEquals(shortUrl1, shortUrl2);
        assertEquals(shortUrl1.length(), shortUrl2.length());
    }

    @Test
    public void getShortLengthShouldBeLessThanOriginal() {
        final var url1 = "http://some-long-url-1.com/aa/bb";
        String shortUrl1 = shortener.getShort(url1);
        assertFalse(shortUrl1.isBlank());
        assertTrue(shortUrl1.length() < url1.length());
    }

    @Test
    public void getShortShouldNotEqualToOriginal() {
        final var url1 = "http://some-long-url-1.com/aa/bb";
        String shortUrl1 = shortener.getShort(url1);
        assertNotEquals(url1, shortUrl1);
    }

    @Test
    public void getShortShouldNotBeBlank() {
        final var url1 = "http://some-long-url-1.com/aa/bb";
        String shortUrl1 = shortener.getShort(url1);
        assertFalse(shortUrl1.isBlank());
    }

    @Test
    public void getShortShouldReturnSame() {
        final var url = "http://some-long-url-1.com/aa/bb";
        final var shortUrl = shortener.getShort(url);
        for (int i = 0; i < 5000; i++) {
            final var shortUrlTmp = shortener.getShort(url);
            assertEquals(shortUrl, shortUrlTmp);
        }
    }

    @Test
    public void getOriginal() {
        final var url1 = "http://some-long-url-1.com/aa/bb";
        String shortUrl1 = shortener.getShort(url1);
        String original = shortener.getOriginal(shortUrl1);
        assertEquals(url1, original);
    }

    @Test
    public void getOriginalShouldThrowWhenDoesNotExist() {
        assertThrows(IllegalArgumentException.class, () -> shortener.getOriginal("url"));
    }

    @Test
    public void concurrencyTest() throws InterruptedException {
        final var nThreads = 10;
        final var nIterations = 100;
        List<String> shortened = Collections.synchronizedList(new LinkedList<>());
        List<String> original = Collections.synchronizedList(new LinkedList<>());
        List<Exception> exceptions = Collections.synchronizedList(new LinkedList<>());
        Runnable r = () -> {
            for (int i = 0; i < nIterations; i++) {
                try {
                    final var shortUrl = shortener.getShort("/concurrency-test-url/" + i);
                    final var originalUrl = shortener.getOriginal(shortUrl);
                    shortened.add(shortUrl);
                    original.add(originalUrl);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        };
        final var executorService = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            executorService.submit(r);
        }
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(1000, TimeUnit.MILLISECONDS));
        assertTrue(exceptions.isEmpty());
        assertTrue(shortened.size() <= nThreads * nIterations);
        assertTrue(original.size() <= nThreads * nIterations);
    }
}