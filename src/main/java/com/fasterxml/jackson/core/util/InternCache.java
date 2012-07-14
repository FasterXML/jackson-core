package com.fasterxml.jackson.core.util;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Singleton class that adds a simple first-level cache in front of
 * regular String.intern() functionality. This is done as a minor
 * performance optimization, to avoid calling native intern() method
 * in cases where same String is being interned multiple times.
 *<p>
 * Note: that this class extends {@link LinkedHashMap} is an implementation
 * detail -- no code should ever directly call Map methods.
 */
@SuppressWarnings("serial")
public final class InternCache
    extends LinkedHashMap<String,String>
{
    /**
     * Size to use is somewhat arbitrary, so let's choose something that's
     * neither too small (low hit ratio) nor too large (waste of memory).
     *<p>
     * 11-Jul-2012, tatu: Also, consider the nasty case of String hashCode()
     *    collisions; size needs to be small enough to survive linear list
     *    lookup... so let's go down a notch (from 192 to 100)
     */
    private final static int MAX_ENTRIES = 100;

    public final static InternCache instance = new InternCache();

    private InternCache() {
        super(MAX_ENTRIES, 0.8f, true);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String,String> eldest)
    {
        return size() > MAX_ENTRIES;
    }

    public synchronized String intern(String input)
    {
        String result = get(input);
        if (result == null) {
            result = input.intern();
            put(result, result);
        }
        return result;
    }


}

