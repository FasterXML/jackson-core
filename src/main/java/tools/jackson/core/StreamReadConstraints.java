package tools.jackson.core;

/*
 * The constraints to use for streaming reads: used to guard against malicious
 * input by preventing processing of "too big" input constructs (values,
 * structures).
 *
 * @since 2.15
 */
public class StreamReadConstraints
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /**
     * Default setting for maximum length: see {@link Builder#maxNumberLength(int)} for details.
     */
    public static final int DEFAULT_MAX_NUM_LEN = 1000;

    final int _maxNumLen;

    private static final StreamReadConstraints DEFAULT =
        new StreamReadConstraints(DEFAULT_MAX_NUM_LEN);

    public static final class Builder {
        private int maxNumLen;

        /**
         * Sets the maximum number length (in chars or bytes, depending on input context).
         * The default is 1000.
         *
         * @param maxNumLen the maximum number length (in chars or bytes, depending on input context)
         *
         * @return this builder
         */
        public Builder maxNumberLength(int maxNumLen) {
            this.maxNumLen = maxNumLen;
            return this;
        }

        Builder() {
            this(DEFAULT_MAX_NUM_LEN);
        }

        Builder(int maxNumLen) {
            this.maxNumLen = maxNumLen;
        }

        Builder(StreamReadConstraints src) {
            maxNumLen = src._maxNumLen;
        }

        public StreamReadConstraints build() {
            return new StreamReadConstraints(maxNumLen);
        }
    }

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    StreamReadConstraints(int maxNumLen) {
        _maxNumLen = maxNumLen;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StreamReadConstraints defaults() {
        return DEFAULT;
    }

    /**
     * @return New {@link Builder} initialized with settings of this constraints
     *   instance
     */
    public Builder rebuild() {
        return new Builder(this);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    /**
     * Accessor for maximum length of numbers to decode.
     * see {@link Builder#maxNumberLength(int)} for details.
     *
     * @return Maximum allowed number length
     */
    public int getMaxNumberLength() {
        return _maxNumLen;
    }

    /*
    /**********************************************************************
    /* Convenience methods for validation
    /**********************************************************************
     */

    /**
     * Convenience method that can be used to verify that a floating-point
     * number of specified length does not exceed maximum specific by this
     * constraints object: if it does, a
     * {@link NumberFormatException}
     * is thrown.
     *
     * @param length Length of number in input units
     *
     * @throws NumberFormatException If length exceeds maximum
     */
    public void validateFPLength(int length) throws NumberFormatException
    {
        if (length > _maxNumLen) {
            throw new NumberFormatException(String.format("Number length (%d) exceeds the maximum length (%d)",
                    length, _maxNumLen));
        }
    }

    /**
     * Convenience method that can be used to verify that an integer
     * number of specified length does not exceed maximum specific by this
     * constraints object: if it does, a
     * {@link NumberFormatException}
     * is thrown.
     *
     * @param length Length of number in input units
     *
     * @throws NumberFormatException If length exceeds maximum
     */
    public void validateIntegerLength(int length) throws NumberFormatException
    {
        if (length > _maxNumLen) {
            throw new NumberFormatException(String.format("Number length (%d) exceeds the maximum length (%d)",
                    length, _maxNumLen));
        }
    }
}
