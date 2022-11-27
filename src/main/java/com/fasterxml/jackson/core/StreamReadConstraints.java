package com.fasterxml.jackson.core;

/**
 * The constraints for streaming reads.
 *
 * @since 2.15
 */
public class StreamReadConstraints {

    private static final int DEFAULT_MAX_NUM_LEN = 1000;

    private final int _maxNumLen;

    public static final StreamReadConstraints UNLIMITED = new StreamReadConstraints(Integer.MAX_VALUE);

    public static final class Builder {
        private int _maxNumLen = StreamReadConstraints.DEFAULT_MAX_NUM_LEN;

        /**
         * Sets the maximum number length (in chars or bytes, depending on input context).
         * The default is 1000 (since Jackson 2.14).
         * @param maxNumLen the maximum number length (in chars or bytes, depending on input context)
         * @return this builder
         * @since 2.15
         */
        public StreamReadConstraints.Builder withMaxNumberLength(int maxNumLen) {
            _maxNumLen = maxNumLen;
            return this;
        }

        public StreamReadConstraints build() {
            return new StreamReadConstraints(_maxNumLen);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private StreamReadConstraints(int maxNumLen) {
        _maxNumLen = maxNumLen;
    }

    public int getMaxNumberLength() {
        return _maxNumLen;
    }
}
