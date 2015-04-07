package com.fasterxml.jackson.core.filter;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Strategy class that can be implemented to specify actual inclusion/exclusion
 * criteria for filtering, used by {@link FilteringGeneratorDelegate}.
 *
 * @since 2.6
 */
public class TokenFilter
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

    // // Marker values

    /**
     * Marker value that should be used to indicate inclusion of a structured
     * value (sub-tree representing Object or Array), or value of a named
     * property (regardless of type).
     */
    public final static TokenFilter INCLUDE_ALL = new TokenFilter();

    // Life-cycle

    protected TokenFilter() { }

    /*
    /**********************************************************
    /* API, structured values
    /**********************************************************
     */

    /**
     * Method called to check whether Object value at current output
     * location should be included in output.
     * Three kinds of return values may be used as follows:
     *<ul>
     * <li><code>null</code> to indicate that the Object should be skipped
     *   </li>
     * <li>{@link #INCLUDE_ALL} to indicate that the Object should be included
     * completely in output
     *   </li>
     * <li>Any other {@link TokenFilter} implementation (possibly this one) to mean
     *  that further inclusion calls on return filter object need to be made
     *  on contained properties, as necessary. {@link #filterFinishObject()} will
     *  also be called on returned filter object
     *   </li>
     * </ul>
     *<p>
     * The default implementation simply returns <code>this</code> to continue calling
     * methods on this filter object, without full inclusion or exclusion.
     * 
     * @return TokenFilter to use for further calls within Array, unless return value
     *   is <code>null</code> or {@link #INCLUDE_ALL} (which have simpler semantics)
     */
    public TokenFilter filterStartObject() {
        return this;
    }

    /**
     * Method called to check whether Array value at current output
     * location should be included in output.
     * Three kinds of return values may be used as follows:
     *<ul>
     * <li><code>null</code> to indicate that the Array should be skipped
     *   </li>
     * <li>{@link #INCLUDE_ALL} to indicate that the Array should be included
     * completely in output
     *   </li>
     * <li>Any other {@link TokenFilter} implementation (possibly this one) to mean
     *  that further inclusion calls on return filter object need to be made
     *  on contained element values, as necessary. {@link #filterFinishArray()} will
     *  also be called on returned filter object
     *   </li>
     * </ul>
     * 
     * @return TokenFilter to use for further calls within Array, unless return value
     *   is <code>null</code> or {@link #INCLUDE_ALL} (which have simpler semantics)
     */
    public TokenFilter filterStartArray() {
        return this;
    }

    /**
     * Method called to indicate that output of non-filtered Object (one that may
     * have been included either completely, or in part) is completed.
     * This occurs when {@link JsonGenerator#writeEndObject()} is called.
     */
    public void filterFinishObject() { }

    /**
     * Method called to indicate that output of non-filtered Array (one that may
     * have been included either completely, or in part) is completed.
     * This occurs when {@link JsonGenerator#writeEndArray()} is called.
     */
    public void filterFinishArray() { }

    /*
    /**********************************************************
    /* API, properties/elements
    /**********************************************************
     */

    /**
     * Method called to check whether property value with specified name,
     * at current output location, should be included in output.
     * Three kinds of return values may be used as follows:
     *<ul>
     * <li><code>null</code> to indicate that the property and its value should be skipped
     *   </li>
     * <li>{@link #INCLUDE_ALL} to indicate that the property and its value should be included
     * completely in output
     *   </li>
     * <li>Any other {@link TokenFilter} implementation (possibly this one) to mean
     *  that further inclusion calls on returned filter object need to be made
     *  as necessary, to determine inclusion.
     *   </li>
     * </ul>
     *<p>
     * The default implementation simply returns <code>this</code> to continue calling
     * methods on this filter object, without full inclusion or exclusion.
     * 
     * @return TokenFilter to use for further calls within property value, unless return value
     *   is <code>null</code> or {@link #INCLUDE_ALL} (which have simpler semantics)
     */
    public TokenFilter includeProperty(String name) {
        return this;
    }

    /**
     * Method called to check whether array element with specified index (zero-based),
     * at current output location, should be included in output.
     * Three kinds of return values may be used as follows:
     *<ul>
     * <li><code>null</code> to indicate that the Array element should be skipped
     *   </li>
     * <li>{@link #INCLUDE_ALL} to indicate that the Array element should be included
     * completely in output
     *   </li>
     * <li>Any other {@link TokenFilter} implementation (possibly this one) to mean
     *  that further inclusion calls on returned filter object need to be made
     *  as necessary, to determine inclusion.
     *   </li>
     * </ul>
     *<p>
     * The default implementation simply returns <code>this</code> to continue calling
     * methods on this filter object, without full inclusion or exclusion.
     * 
     * @return TokenFilter to use for further calls within element value, unless return value
     *   is <code>null</code> or {@link #INCLUDE_ALL} (which have simpler semantics)
     */
    public TokenFilter includeElement(int index) {
        return this;
    }

    /**
     * Method called to check whether root-level value,
     * at current output location, should be included in output.
     * Three kinds of return values may be used as follows:
     *<ul>
     * <li><code>null</code> to indicate that the root value should be skipped
     *   </li>
     * <li>{@link #INCLUDE_ALL} to indicate that the root value should be included
     * completely in output
     *   </li>
     * <li>Any other {@link TokenFilter} implementation (possibly this one) to mean
     *  that further inclusion calls on returned filter object need to be made
     *  as necessary, to determine inclusion.
     *   </li>
     * </ul>
     *<p>
     * The default implementation simply returns <code>this</code> to continue calling
     * methods on this filter object, without full inclusion or exclusion.
     * 
     * @return TokenFilter to use for further calls within root value, unless return value
     *   is <code>null</code> or {@link #INCLUDE_ALL} (which have simpler semantics)
     */
    public TokenFilter includeRootValue(int index) {
        return this;
    }
    
    /*
    /**********************************************************
    /* API, scalas values
    /**********************************************************
     */

    /**
     * Call made to verify whether leaf-level
     * boolean value
     * should be included in output or not.
     */
    public boolean includeBoolean(boolean value) {
        return _includeScalar();
    }

    /**
     * Call made to verify whether leaf-level
     * null value
     * should be included in output or not.
     */
    public boolean includeNull() {
        return _includeScalar();
    }

    /**
     * Call made to verify whether leaf-level
     * String value
     * should be included in output or not.
     */
    public boolean includeString(String value) {
        return _includeScalar();
    }

    /**
     * Call made to verify whether leaf-level
     * <code>int</code> value
     * should be included in output or not.
     * 
     * NOTE: also called for `short`, `byte`
     */
    public boolean includeNumber(int v) {
        return _includeScalar();
    }

    /**
     * Call made to verify whether leaf-level
     * <code>long</code> value
     * should be included in output or not.
     */
    public boolean includeNumber(long v) {
        return _includeScalar();
    }

    /**
     * Call made to verify whether leaf-level
     * <code>float</code> value
     * should be included in output or not.
     */
    public boolean includeNumber(float v) {
        return _includeScalar();
    }

    /**
     * Call made to verify whether leaf-level
     * <code>double</code> value
     * should be included in output or not.
     */
    public boolean includeNumber(double v) {
        return _includeScalar();
    }
    
    /**
     * Call made to verify whether leaf-level
     * {@link BigDecimal} value
     * should be included in output or not.
     */
    public boolean includeNumber(BigDecimal v) {
        return _includeScalar();
    }

    /**
     * Call made to verify whether leaf-level
     * {@link BigInteger} value
     * should be included in output or not.
     */
    public boolean includeNumber(BigInteger v) {
        return _includeScalar();
    }

    /**
     * Call made to verify whether leaf-level
     * Binary value
     * should be included in output or not.
     *<p>
     * NOTE: no binary payload passed; assumption is this won't be of much use.
     */
    public boolean includeBinary() {
        return _includeScalar();
    }

    /**
     * Call made to verify whether leaf-level
     * raw (pre-encoded, not quoted by generator) value
     * should be included in output or not.
     *<p>
     * NOTE: value itself not passed since it may come on multiple forms
     * and is unlikely to be of much use in determining inclusion
     * criteria.
     */
    public boolean includeRawValue() {
        return _includeScalar();
    }
    
    /**
     * Call made to verify whether leaf-level
     * embedded (Opaque) value
     * should be included in output or not.
     */
    public boolean includeEmbeddedValue(Object ob) {
        return _includeScalar();
    }

    /*
    /**********************************************************
    /* Other methods
    /**********************************************************
     */
    
    /**
     * Overridable default implementation delegated to all scalar value
     * inclusion check methods
     */
    protected boolean _includeScalar() {
        return false;
    }
}
