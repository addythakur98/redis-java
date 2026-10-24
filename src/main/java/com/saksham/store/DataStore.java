package com.saksham.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {

    // value + the moment it expires (-1 = never)
    private record Entry(String value, long expiresAt) {
        boolean isExpired(long now) {
            return expiresAt != -1 && now >= expiresAt;
        }
    }

    private final Map<String, Entry> map = new ConcurrentHashMap<>();

    // LAZY EXPIRY: every read goes through here
    private Entry getAlive(String key) {
        Entry e = map.get(key);
        if (e == null) return null;
        if (e.isExpired(System.currentTimeMillis())) {
            map.remove(key, e);          // remove only if it's still this old entry
            return null;
        }
        return e;
    }

    public void setWithExpireAt(String key, String value, long expiresAtMs) {
        map.put(key, new Entry(value, expiresAtMs));
    }

    public boolean expireAt(String key, long expiresAtMs) {
        Entry e = getAlive(key);
        if (e == null) return false;
        return map.replace(key, e, new Entry(e.value(), expiresAtMs));
    }

    public int size() {
        return map.size();
    }

    public void set(String key, String value) {
        map.put(key, new Entry(value, -1));               // plain SET clears any old TTL
    }

    public void setWithTtl(String key, String value, long ttlMillis) {
        setWithExpireAt(key, value, System.currentTimeMillis() + ttlMillis);
    }

    public String get(String key) {
        Entry e = getAlive(key);
        return e == null ? null : e.value();
    }

    public boolean delete(String key) {
        Entry e = map.remove(key);
        return e != null && !e.isExpired(System.currentTimeMillis());   // expired keys don't count
    }

    public boolean exists(String key) {
        return getAlive(key) != null;
    }

    // EXPIRE key seconds → true if the key exists and the timeout was set
    public boolean expire(String key, long seconds) {
        return expireAt(key, System.currentTimeMillis() + seconds * 1000);
    }

    // TTL key → seconds left, -1 if no expiry, -2 if the key doesn't exist
    public long ttl(String key) {
        Entry e = getAlive(key);
        if (e == null) return -2;
        if (e.expiresAt() == -1) return -1;
        long msLeft = e.expiresAt() - System.currentTimeMillis();
        return (msLeft + 500) / 1000;                     // round to the nearest second
    }

    // ACTIVE EXPIRY: called every 100 ms by a background task
    public void removeExpired() {
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(en -> en.getValue().isExpired(now));
    }
}