package com.fasterxml.jackson.core.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton class that adds a simple first-level cache in front of
 * regular String.intern() functionality. This is done as a minor
 * performance optimization, to avoid calling native intern() method
 * in cases where same String is being interned multiple times.
 */
public final class InternCache
    extends ConcurrentHashMap<String,String> // since 2.3
{
    private static final long serialVersionUID = 1L;

    /**
     * Size to use is somewhat arbitrary, so let's choose something that's
     * neither too small (low hit ratio) nor too large (waste of memory).
     *<p>
     * One consideration is possible attack via colliding {@link String#hashCode};
     * because of this, limit to reasonably low setting.
     */
    private final static int MAX_ENTRIES = 180;

    public final static InternCache instance = new InternCache();

    /**
     * As minor optimization let's try to avoid "flush storms",
     * cases where multiple threads might try to concurrently
     * flush the map.
     */
    private final ReentrantLock lock = new ReentrantLock();

    public InternCache() { this(MAX_ENTRIES, 0.8f, 4); }

    public InternCache(int maxSize, float loadFactor, int concurrency) {
        super(maxSize, loadFactor, concurrency);
    }

    public String intern(String input) {
        String result = get(input);
        if (result != null) { return result; }

        /* 18-Sep-2013, tatu: We used to use LinkedHashMap, which has simple LRU
         *   method. No such functionality exists with CHM; and let's use simplest
         *   possible limitation: just clear all contents. This because otherwise
         *   we are simply likely to keep on clearing same, commonly used entries.
         */
        if (size() >= MAX_ENTRIES) {
            /* As of 2.18, the limit is not strictly enforced, but we do try to
             * clear entries if we have reached the limit. We do not expect to
             * go too much over the limit, and if we do, it's not a huge problem.
             * If some other thread has the lock, we will not clear but the lock should
             * not be held for long, so another thread should be able to clear in the near future.
             */
            if (lock.tryLock()) {
                try {
                    if (size() >= MAX_ENTRIES) {
                        clear();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        result = input.intern();
        put(result, result);
        return result;
    }
}

