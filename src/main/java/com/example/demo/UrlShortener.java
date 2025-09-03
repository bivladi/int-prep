package com.example.demo;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UrlShortener {

    private static final int WAIT_TIMEOUT = 100;
    private final MessageDigest md5;
    private final Map<String, String> map;
    private final ReentrantReadWriteLock lock;

    public UrlShortener() {
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.map = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public String getShort(String originalUrl) {
        try {
            if (!lock.writeLock().tryLock(WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timeout exceeded");
            }
            var hashInput = new StringBuilder(originalUrl);
            var hashOutput = hashFunction(hashInput.toString());
            var count = 0;
            while (map.containsKey(hashOutput)) {
                hashInput.append(count);
                hashOutput = hashFunction(hashInput.toString());
                count++;
            }
            map.put(hashOutput, originalUrl);
            return hashOutput;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<String> getOriginal(String shortUrl) {
        try {
            if (!lock.readLock().tryLock(WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timeout exceeded");
            }
            return Optional.ofNullable(map.get(shortUrl));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            lock.readLock().unlock();
        }
    }

    private String hashFunction(String input) {
        final var digest = md5.digest(input.getBytes(StandardCharsets.UTF_8));
        final var value = new BigInteger(1, digest);
        final var hash = value.toString(32);
        return hash.length() < 6 ? hash : hash.substring(0, 6);
    }

}
