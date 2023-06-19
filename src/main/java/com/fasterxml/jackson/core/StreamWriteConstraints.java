package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.exc.StreamConstraintsException;

/**
 * The constraints to use for streaming writes: used to guard against problematic
 * output by preventing processing of "too big" output constructs (values,
 * structures).
 * Constraints are registered with {@code TokenStreamFactory} (such as
 * {@code JsonFactory}); if nothing explicitly specified, default
 * constraints are used.
 *<p>
 * Currently constrained aspects, with default settings, are:
 * <ul>
 *  <li>Maximum Nesting depth: default 1000 (see {@link #DEFAULT_MAX_DEPTH})
 *   </li>
 * </ul>
 *
 * @since 2.16
 */
public class StreamWriteConstraints
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Default setting for maximum depth: see {@link Builder#maxNestingDepth(int)} for details.
     */
    public static final int DEFAULT_MAX_DEPTH = 1000;

    protected final int _maxNestingDepth;

    private static final StreamWriteConstraints DEFAULT =
        new StreamWriteConstraints(DEFAULT_MAX_DEPTH);

    public static final class Builder {
        private int maxNestingDepth;

        /**
         * Sets the maximum nesting depth. The depth is a count of objects and arrays that have not
         * been closed, `{` and `[` respectively.
         *
         * @param maxNestingDepth the maximum depth
         *
         * @return this builder
         * @throws IllegalArgumentException if the maxNestingDepth is set to a negative value
         */
        public Builder maxNestingDepth(final int maxNestingDepth) {
            if (maxNestingDepth < 0) {
                throw new IllegalArgumentException("Cannot set maxNestingDepth to a negative value");
            }
            this.maxNestingDepth = maxNestingDepth;
            return this;
        }

        Builder() {
            this(DEFAULT_MAX_DEPTH);
        }

        Builder(final int maxNestingDepth) {
            this.maxNestingDepth = maxNestingDepth;
        }

        Builder(StreamWriteConstraints src) {
            maxNestingDepth = src._maxNestingDepth;
        }

        public StreamWriteConstraints build() {
            return new StreamWriteConstraints(maxNestingDepth);
        }
    }

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected StreamWriteConstraints(final int maxNestingDepth) {
        _maxNestingDepth = maxNestingDepth;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StreamWriteConstraints defaults() {
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
     * Accessor for maximum depth.
     * see {@link Builder#maxNestingDepth(int)} for details.
     *
     * @return Maximum allowed depth
     */
    public int getMaxNestingDepth() {
        return _maxNestingDepth;
    }

    /*
    /**********************************************************************
    /* Convenience methods for validation, document limits
    /**********************************************************************
     */

    /**
     * Convenience method that can be used to verify that the
     * nesting depth does not exceed the maximum specified by this
     * constraints object: if it does, a
     * {@link StreamConstraintsException}
     * is thrown.
     *
     * @param depth count of unclosed objects and arrays
     *
     * @throws StreamConstraintsException If depth exceeds maximum
     */
    public void validateNestingDepth(int depth) throws StreamConstraintsException
    {
        if (depth > _maxNestingDepth) {
            throw _constructException(
                    "Document nesting depth (%d) exceeds the maximum allowed (%d, from %s)",
                    depth, _maxNestingDepth,
                    _constrainRef("getMaxNestingDepth"));
        }
    }

    /*
    /**********************************************************************
    /* Error reporting
    /**********************************************************************
     */

    // @since 2.16
    protected StreamConstraintsException _constructException(String msgTemplate, Object... args) throws StreamConstraintsException {
        throw new StreamConstraintsException(String.format(msgTemplate, args));
    }

    // @since 2.16
    protected String _constrainRef(String method) {
        return "`StreamWriteConstraints."+method+"()`";
    }
}
