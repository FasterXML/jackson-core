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
public class ErrorReportConfiguration
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

    private static ErrorReportConfiguration DEFAULT =
            new ErrorReportConfiguration(DEFAULT_MAX_ERROR_TOKEN_LENGTH);

    public static void overrideDefaultErrorReportConfiguration(final ErrorReportConfiguration errorReportConfiguration) {
        if (errorReportConfiguration == null) {
            DEFAULT = new ErrorReportConfiguration(DEFAULT_MAX_ERROR_TOKEN_LENGTH);
        } else {
            DEFAULT = errorReportConfiguration;
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

        Builder(ErrorReportConfiguration src) {
            this.maxErrorTokenLength = src._maxErrorTokenLength;
        }

        public ErrorReportConfiguration build() {
            return new ErrorReportConfiguration(maxErrorTokenLength);
        }
    }
    
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected ErrorReportConfiguration(final int maxErrorTokenLength) {
        _maxErrorTokenLength = maxErrorTokenLength;
    }

    public static ErrorReportConfiguration.Builder builder() {
        return new ErrorReportConfiguration.Builder();
    }

    /**
     * @return the default {@link ErrorReportConfiguration} (when none is set on the {@link JsonFactory} explicitly)
     * @see #overrideDefaultErrorReportConfiguration(ErrorReportConfiguration)
     */
    public static ErrorReportConfiguration defaults() {
        return DEFAULT;
    }

    /**
     * @return New {@link ErrorReportConfiguration.Builder} initialized with settings of configuration
     * instance
     */
    public ErrorReportConfiguration.Builder rebuild() {
        return new ErrorReportConfiguration.Builder(this);
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
