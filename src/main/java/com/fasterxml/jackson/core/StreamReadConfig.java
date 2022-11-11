package com.fasterxml.jackson.core;

/**
 * The configuration for streaming reads.
 *
 * @since 2.15
 */
public class StreamReadConfig {

    private static final int DEFAULT_MAX_NUM_LEN = 1000;

    private int _maxNumLen = DEFAULT_MAX_NUM_LEN;

    /**
     * Sets the maximum number length (in chars). The default is 1000 (since Jackson 2.14)
     * @param maxNumLen the maximum number length (in chars)
     * @return this config
     * @since 2.15
     */
    public StreamReadConfig maxNumberLength(int maxNumLen) {
        _maxNumLen = maxNumLen;
        return this;
    }

    public int getMaxNumberLength() {
        return _maxNumLen;
    }
}
