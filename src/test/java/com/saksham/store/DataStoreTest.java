package com.saksham.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataStoreTest {

    @Test
    void setThenGet() {
        DataStore store = new DataStore();
        store.set("name", "jay");
        assertEquals("jay", store.get("name"));
        assertNull(store.get("missing"));
    }

    @Test
    void deleteReportsWhetherKeyExisted() {
        DataStore store = new DataStore();
        store.set("a", "1");
        assertTrue(store.delete("a"));
        assertFalse(store.delete("a"));      // already gone
    }

    @Test
    void ttlCodes() {
        DataStore store = new DataStore();
        store.set("a", "1");
        assertEquals(-1, store.ttl("a"));        // no expiry
        assertEquals(-2, store.ttl("missing"));  // no such key
    }

    @Test
    void keyExpiresLazily() throws InterruptedException {
        DataStore store = new DataStore();
        store.setWithTtl("otp", "4821", 50);     // 50 ms
        assertEquals("4821", store.get("otp"));
        Thread.sleep(100);
        assertNull(store.get("otp"));            // expired on read
    }

    @Test
    void cleanerRemovesExpiredKeys() throws InterruptedException {
        DataStore store = new DataStore();
        store.setWithTtl("a", "1", 50);
        store.set("b", "2");
        Thread.sleep(100);
        store.removeExpired();                   // what the background task calls
        assertEquals(1, store.size());           // only "b" left, without anyone reading "a"
    }
}