package com.fasterxml.jackson.core;

import java.io.Serializable;

/**
 * Container for configuration values used when handling errorneous token inputs. 
 * For example, unquoted text segments.
 * <p>
 * Currently default settings are
 * <ul>
 *     <li>Maximum length of token to include in error messages : default 256 (see {@link #_maxErrorTokenLength})
 * </ul>
 *
 * @since 2.16
 */
public class ErrorTokenConfiguration
        implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Default value for {@link #_maxErrorTokenLength}.
     */
    public final static int DEFAULT_MAX_ERROR_TOKEN_LENGTH = 256;

    /**
     * Maximum length of token to include in error messages
     *
     * @see Builder#maxErrorTokenLength(int)
     */
    protected final int _maxErrorTokenLength;

    private static ErrorTokenConfiguration DEFAULT =
            new ErrorTokenConfiguration(DEFAULT_MAX_ERROR_TOKEN_LENGTH);

    public static void overrideDefaultErrorTokenConfiguration(final ErrorTokenConfiguration errorTokenConfiguration) {
        if (errorTokenConfiguration == null) {
            DEFAULT = new ErrorTokenConfiguration(DEFAULT_MAX_ERROR_TOKEN_LENGTH);
        } else {
            DEFAULT = errorTokenConfiguration;
        }
    }
    
    /*
    /**********************************************************************
    /* Builder
    /**********************************************************************
     */

    public static final class Builder {
        private int maxErrorTokenLength;

        /**
         * @param maxErrorTokenLength Constraints
         * @return This factory instance (to allow call chaining)
         * @throws IllegalArgumentException if {@code maxErrorTokenLength} is less than 0
         */
        public Builder maxErrorTokenLength(final int maxErrorTokenLength) {
            validateMaxErrorTokenLength(maxErrorTokenLength);
            this.maxErrorTokenLength = maxErrorTokenLength;
            return this;
        }

        Builder() {
            this(DEFAULT_MAX_ERROR_TOKEN_LENGTH);
        }

        Builder(final int maxErrorTokenLength) {
            this.maxErrorTokenLength = maxErrorTokenLength;
        }

        Builder(ErrorTokenConfiguration src) {
            this.maxErrorTokenLength = src._maxErrorTokenLength;
        }

        public ErrorTokenConfiguration build() {
            return new ErrorTokenConfiguration(maxErrorTokenLength);
        }
    }
    
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected ErrorTokenConfiguration(final int maxErrorTokenLength) {
        _maxErrorTokenLength = maxErrorTokenLength;
    }

    public static ErrorTokenConfiguration.Builder builder() {
        return new ErrorTokenConfiguration.Builder();
    }

    /**
     * @return the default {@link ErrorTokenConfiguration} (when none is set on the {@link JsonFactory} explicitly)
     * @see #overrideDefaultErrorTokenConfiguration(ErrorTokenConfiguration)
     */
    public static ErrorTokenConfiguration defaults() {
        return DEFAULT;
    }

    /**
     * @return New {@link ErrorTokenConfiguration.Builder} initialized with settings of configuration
     * instance
     */
    public ErrorTokenConfiguration.Builder rebuild() {
        return new ErrorTokenConfiguration.Builder(this);
    }

    
    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    /**
     * Accessor for {@link #_maxErrorTokenLength}
     *
     * @return Maximum length of token to include in error messages
     * @see Builder#maxErrorTokenLength(int)
     */
    public int getMaxErrorTokenLength() {
        return _maxErrorTokenLength;
    }

    /*
    /**********************************************************************
    /* Convenience methods for validation
    /**********************************************************************
     */

    /**
     * Convenience method that can be used to verify that the
     * max error token length does not exceed the maximum specified by this object: if it does, a
     * {@link IllegalArgumentException} is thrown.
     *
     * @param maxErrorTokenLength count of unclosed objects and arrays
     */
    private static void validateMaxErrorTokenLength(int maxErrorTokenLength) throws IllegalArgumentException {
        if (maxErrorTokenLength < 0) {
            throw new IllegalArgumentException(
                    String.format("Value of maxErrorTokenLength (%d) cannot be negative", maxErrorTokenLength));
        }
    }
}
