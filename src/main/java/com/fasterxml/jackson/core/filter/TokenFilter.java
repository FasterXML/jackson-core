package com.fasterxml.jackson.core.filter;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @since 2.6
 */
public abstract class TokenFilter
{
    // Constants

    /**
     * Value that indicates that the current token (and its children, if any)
     * should be (or is being) skipped and NOT to be passed on delegate.
     */
    public final static int FILTER_SKIP = 1;

    /**
     * Value that indicates that it is not yet certain whether the current token
     * should or should not be included. If returned for leaf node it will
     * usually be taken to mean same as {@link #FILTER_SKIP}; for container nodes
     * and property names it means that traversal needs to check contents,
     * and inclusion will be based on those.
     */
    public final static int FILTER_CHECK = 2;

    /**
     * Value that indicates that the current token (and its children, if any)
     * should be (or is being) included and to be passed on delegate.
     * As state for container states this means that the start token has been
     * passed, and matching end token must also be passed.
     */
    public final static int FILTER_INCLUDE = 3;


    // API, container values

    public int filterStartObject() {
        return FILTER_CHECK;
    }

    public int filterStartArray() {
        return FILTER_CHECK;
    }
    
    public void filterFinishObject() { }
    public void filterFinishArray() { }

    // API, properties/elements

    public int filterProperty(String name) {
        return FILTER_CHECK;
    }

    public int filterElement(int index) {
        return FILTER_CHECK;
    }

    // API, scalar

    public boolean includeBoolean(boolean value) {
        return false;
    }

    public boolean includeNull() {
        return false;
    }

    public boolean includeString(String value) {
        return false;
    }

    /**
     * NOTE: also called for `short`, `byte`
     */
    public boolean includeNumber(int i) {
        return false;
    }

    public boolean includeNumber(long l) {
        return false;
    }

    public boolean includeNumber(BigDecimal v) {
        return false;
    }

    public boolean includeNumber(BigInteger v) {
        return false;
    }

    /**
     * NOTE: no binary payload passed; assumption is this won't be of much
     * use.
     */
    public boolean includeBinary() {
        return false;
    }

    /**
     * NOTE: value itself not passed since it may come on multiple forms
     * and is unlikely to be of much use in determining inclusion
     * criteria.
     */
    public boolean includeRawValue() {
        return false;
    }
    
    public boolean includeEmbeddedValue(Object ob) {
        return false;
    }
}
