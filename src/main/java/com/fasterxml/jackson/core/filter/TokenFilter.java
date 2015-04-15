package com.fasterxml.jackson.core.filter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Strategy class that can be implemented to specify actual inclusion/exclusion
 * criteria for filtering, used by {@link FilteringGeneratorDelegate}.
 *
 * @since 2.6
 */
public class TokenFilter
{

    // // Marker values

    /**
     * Marker value that should be used to indicate inclusion of a structured
     * value (sub-tree representing Object or Array), or value of a named
     * property (regardless of type).
     * Note that if this instance is returned, it will used as a marker, and 
     * no actual callbacks need to be made. For this reason, it is more efficient
     * to return this instance if the whole sub-tree is to be included, instead
     * of implementing similar filter functionality explicitly.
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
     * Default implementation returns <code>this</code>, which means that checks
     * are made recursively for properties of the Object to determine possible inclusion.
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
     *<p>
     * Default implementation returns <code>this</code>, which means that checks
     * are made recursively for elements of the array to determine possible inclusion.
     * 
     * @return TokenFilter to use for further calls within Array, unless return value
     *   is <code>null</code> or {@link #INCLUDE_ALL} (which have simpler semantics)
     */
    public TokenFilter filterStartArray() {
        return this;
    }

    /**
     * Method called to indicate that output of non-filtered Object (one that may
     * have been included either completely, or in part) is completed,
     * in cases where filter other that {@link #INCLUDE_ALL} was returned.
     * This occurs when {@link JsonGenerator#writeEndObject()} is called.
     */
    public void filterFinishObject() { }

    /**
     * Method called to indicate that output of non-filtered Array (one that may
     * have been included either completely, or in part) is completed,
     * in cases where filter other that {@link #INCLUDE_ALL} was returned.
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
    /* API, scalar values (being read)
    /**********************************************************
     */

    /**
     * Call made when verifying whether a scaler value is being
     * read from a parser.
     *<p>
     * Default action is to call <code>_includeScalar()</code> and return
     * whatever it indicates.
     */
    public boolean includeValue(JsonParser p) throws IOException {
        return _includeScalar();
    }

    /*
    /**********************************************************
    /* API, scalar values (being written)
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
    /* Overrides
    /**********************************************************
     */

    @Override
    public String toString() {
        if (this == INCLUDE_ALL) {
            return "TokenFilter.INCLUDE_ALL";
        }
        return super.toString();
    }

    /*
    /**********************************************************
    /* Other methods
    /**********************************************************
     */
    
    /**
     * Overridable default implementation delegated to all scalar value
     * inclusion check methods.
     * The default implementation simply includes all leaf values.
     */
    protected boolean _includeScalar() {
        return true;
    }
}
