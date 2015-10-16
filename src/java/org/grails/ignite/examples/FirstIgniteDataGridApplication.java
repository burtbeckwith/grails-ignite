package org.grails.ignite.examples;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.transactions.Transaction;

import java.util.concurrent.locks.Lock;

/**
 * Example data grid class taken from the Ignite wiki.
 *
 * @author Dan Stieglitz
 */
public class FirstIgniteDataGridApplication extends AbstractIgniteApplication implements Runnable {
    IgniteCache<Integer, String> cache;

    public FirstIgniteDataGridApplication(Ignite ignite) {
        super(ignite);
    }

    private void putAndGet() {
        cache = ignite.getOrCreateCache("testCache");

        // Store keys in cache (values will end up on different cache nodes).
        for (int i = 0; i < 10; i++) {
            cache.put(i, Integer.toString(i));
         }

        for (int i = 0; i < 10; i++) {
            System.out.println("Got [key=" + i + ", val=" + cache.get(i) + ']');
         }
    }

    private void atomicOperations() {
        // Put-if-absent which returns previous value.
        String oldVal = cache.getAndPutIfAbsent(11, "Hello");
        assert oldVal == "Hello";

        // Put-if-absent which returns boolean success flag.
        boolean success = cache.putIfAbsent(22, "World");
        assert success == true;

        // Replace-if-exists operation (opposite of getAndPutIfAbsent), returns previous value.
        oldVal = cache.getAndReplace(11, "Hello");
        assert oldVal == "Hello";

        // Replace-if-exists operation (opposite of putIfAbsent), returns boolean success flag.
        success = cache.replace(22, "World");
        assert success == true;

        // Replace-if-matches operation.
        success = cache.replace(22, "Mundo", "World");
        assert success == true;

        // Remove-if-matches operation.
        success = cache.remove(1, "Hello");
        assert success == true;
    }

    private void transactions() {
        Transaction tx = ignite.transactions().txStart();
        try {
            String hello = cache.get(11);

            if (hello == "Hello") {
                cache.put(11, "Hello");
            }

            cache.put(22, "World");

            tx.commit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void distributedLocks() {
        // Lock cache key "Hello".
        Lock lock = cache.lock(11);

        lock.lock();

        try {
            cache.put(11, "Hello");
            cache.put(22, "World");
        } finally {
            lock.unlock();
        }
    }

    public void run() {
        putAndGet();
        atomicOperations();
        transactions();
        distributedLocks();
    }
}
