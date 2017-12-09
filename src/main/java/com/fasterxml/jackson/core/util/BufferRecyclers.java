package com.fasterxml.jackson.core.util;

import java.lang.ref.SoftReference;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

/**
 * Helper entity used to control access to simple buffer recyling scheme used for
 * some encoding, decoding tasks.
 * 
 * @see BufferRecycler
 * @see JsonStringEncoder
 *
 * @since 2.9.2
 */
public class BufferRecyclers
{
    /*
    /**********************************************************
    /* BufferRecyclers for parsers, generators
    /**********************************************************
     */

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final protected static ThreadLocal<SoftReference<BufferRecycler>> _recyclerRef
        = new ThreadLocal<SoftReference<BufferRecycler>>();

    public static BufferRecycler getBufferRecycler()
    {
        SoftReference<BufferRecycler> ref = _recyclerRef.get();
        BufferRecycler br = (ref == null) ? null : ref.get();

        if (br == null) {
            br = new BufferRecycler();
            _recyclerRef.set(new SoftReference<BufferRecycler>(br));
        }
        return br;
    }

    /*
    /**********************************************************
    /* JsonStringEncoder
    /**********************************************************
     */

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final protected static ThreadLocal<SoftReference<JsonStringEncoder>> _encoderRef
        = new ThreadLocal<SoftReference<JsonStringEncoder>>();

    public static JsonStringEncoder getJsonStringEncoder() {
        SoftReference<JsonStringEncoder> ref = _encoderRef.get();
        JsonStringEncoder enc = (ref == null) ? null : ref.get();

        if (enc == null) {
            enc = new JsonStringEncoder();
            _encoderRef.set(new SoftReference<JsonStringEncoder>(enc));
        }
        return enc;
    }

    /**
     * Helper method for encoding given String as UTF-8 encoded
     *
     * @since 2.9.4
     */
    public static byte[] encodeAsUTF8(String text) {
        return getJsonStringEncoder().encodeAsUTF8(text);
    }

    /**
     * @since 2.9.4
     */
    public static char[] quoteAsJsonText(String rawText) {
        return getJsonStringEncoder().quoteAsString(rawText);
    }

    /**
     * @since 2.9.4
     */
    public static void quoteAsJsonText(CharSequence input, StringBuilder output) {
        getJsonStringEncoder().quoteAsString(input, output);
    }

    /**
     * @since 2.9.4
     */
    public static byte[] quoteAsJsonUTF8(String rawText) {
        return getJsonStringEncoder().quoteAsUTF8(rawText);
    }
}
