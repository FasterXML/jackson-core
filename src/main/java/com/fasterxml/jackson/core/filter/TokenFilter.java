package com.fasterxml.jackson.core.filter;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Strategy class that can be implemented to specify actual inclusion/exclusion
 * criteria for filtering, used by {@link FilteringGeneratorDelegate}.
 *
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

    /**
     * Called to see if Object property with specified name (of any type)
     * should be included or not
     */
    public int includeProperty(String name) {
        return FILTER_CHECK;
    }

    /**
     * Called to see if Array element with specified index (of any type)
     * should be included or not
     */
    public int includeElement(int index) {
        return FILTER_CHECK;
    }

    /**
     * Called to see if root value about to be written should be included or not
     */
    public int includeRootValue(int index) {
        return FILTER_CHECK;
    }
    
    // API, scalar

    public boolean includeBoolean(boolean value) {
        return _includeScalar();
    }

    public boolean includeNull() {
        return _includeScalar();
    }

    public boolean includeString(String value) {
        return _includeScalar();
    }

    /**
     * NOTE: also called for `short`, `byte`
     */
    public boolean includeNumber(int v) {
        return _includeScalar();
    }

    public boolean includeNumber(long v) {
        return _includeScalar();
    }

    public boolean includeNumber(float v) {
        return _includeScalar();
    }

    public boolean includeNumber(double v) {
        return _includeScalar();
    }
    
    public boolean includeNumber(BigDecimal v) {
        return _includeScalar();
    }

    public boolean includeNumber(BigInteger v) {
        return _includeScalar();
    }

    /**
     * NOTE: no binary payload passed; assumption is this won't be of much
     * use.
     */
    public boolean includeBinary() {
        return _includeScalar();
    }

    /**
     * NOTE: value itself not passed since it may come on multiple forms
     * and is unlikely to be of much use in determining inclusion
     * criteria.
     */
    public boolean includeRawValue() {
        return _includeScalar();
    }
    
    public boolean includeEmbeddedValue(Object ob) {
        return _includeScalar();
    }

    /**
     * Overridable default implementation delegated to all scalar value
     * inclusion check methods
     */
    protected boolean _includeScalar() {
        return false;
    }
}
