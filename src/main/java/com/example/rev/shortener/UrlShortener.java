package com.example.rev.shortener;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UrlShortener {

    private static final Integer MAX_AWAIT_MS = 1000;
    private final Map<String, String> shortToOriginal;
    private final Map<String, String> originalToShort;
    private final MessageDigest md5;
    private final ReentrantReadWriteLock lock;

    public UrlShortener() {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        shortToOriginal = new HashMap<>();
        originalToShort = new HashMap<>();
        lock = new ReentrantReadWriteLock();
    }

    public String getShort(String url) {
        try {
            if (!lock.writeLock().tryLock(MAX_AWAIT_MS, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Can't acquire the write lock");
            }
            try {
                if (originalToShort.containsKey(url)) {
                    return originalToShort.get(url);
                }
                String shortUrl = getHash(url);
                shortToOriginal.put(shortUrl, url);
                originalToShort.put(url, shortUrl);
                return shortUrl;
            } finally {
                lock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /*
    [A-Za-z0-9] - base62 62^7 (3,5 * 10^12) = 56bit (1char=1byte=8bit -> 7 * 8bit = 56 bit)
    e.g. http://shortener.org/AzbN4h1
     */
    private String getHash(String originalUrl) {
        final var buffer = new StringBuilder(originalUrl);
        byte[] digestResult = md5.digest(buffer.toString().getBytes(StandardCharsets.UTF_8));
        final var digestHex = new BigInteger(1, digestResult).toString(32);
        String hash = digestHex.length() < 7 ? digestHex : digestHex.substring(0, 7);
        int idx = 0;
        while (shortToOriginal.containsKey(hash)) {
            buffer.append(idx);
            digestResult = md5.digest(buffer.toString().getBytes(StandardCharsets.UTF_8));
            hash = new String(digestResult).substring(0, 7);
        }
        return hash;
    }

    public String getOriginal(String shortUrl) {
        try {
            if (!lock.readLock().tryLock(MAX_AWAIT_MS, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Can't acquire the read lock");
            }
            try {
                if (!shortToOriginal.containsKey(shortUrl)) {
                    throw new IllegalArgumentException("No such url");
                }
                return shortToOriginal.get(shortUrl);
            } finally {
                lock.readLock().unlock();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
