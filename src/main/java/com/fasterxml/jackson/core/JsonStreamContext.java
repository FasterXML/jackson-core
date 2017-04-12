/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

/**
 * Shared base class for streaming processing contexts used during
 * reading and writing of Json content using Streaming API.
 * This context is also exposed to applications:
 * context object can be used by applications to get an idea of
 * relative position of the parser/generator within json content
 * being processed. This allows for some contextual processing: for
 * example, output within Array context can differ from that of
 * Object context.
 */
public abstract class JsonStreamContext
{
    // // // Type constants used internally

    protected final static int TYPE_ROOT = 0;
    protected final static int TYPE_ARRAY = 1;
    protected final static int TYPE_OBJECT = 2;

    protected int _type;

    /**
     * Index of the currently processed entry. Starts with -1 to signal
     * that no entries have been started, and gets advanced each
     * time a new entry is started, either by encountering an expected
     * separator, or with new values if no separators are expected
     * (the case for root context).
     */
    protected int _index;
    
    protected Object _value;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected JsonStreamContext() { }

    /*
    /**********************************************************
    /* Public API, accessors
    /**********************************************************
     */

    /**
     * Accessor for finding parent context of this context; will
     * return null for root context.
     */
    public abstract JsonStreamContext getParent();

    /**
     * Method that returns true if this context is an Array context;
     * that is, content is being read from or written to a Json Array.
     */
    public final boolean inArray() { return _type == TYPE_ARRAY; }

    /**
     * Method that returns true if this context is a Root context;
     * that is, content is being read from or written to without
     * enclosing array or object structure.
     */
    public final boolean inRoot() { return _type == TYPE_ROOT; }

    /**
     * Method that returns true if this context is an Object context;
     * that is, content is being read from or written to a Json Object.
     */
    public final boolean inObject() { return _type == TYPE_OBJECT; }

    /**
     * Method for accessing simple type description of current context;
     * either ROOT (for root-level values), OBJECT (for field names and
     * values of JSON Objects) or ARRAY (for values of JSON Arrays)
     *
     * @deprecated Since 2.8 use {@link #typeDesc} instead
     */
    @Deprecated // since 2.8
    public final String getTypeDesc() {
        switch (_type) {
        case TYPE_ROOT: return "ROOT";
        case TYPE_ARRAY: return "ARRAY";
        case TYPE_OBJECT: return "OBJECT";
        }
        return "?";
    }

    /**
     * @since 2.8
     */
    public String typeDesc() {
        switch (_type) {
        case TYPE_ROOT: return "root";
        case TYPE_ARRAY: return "Array";
        case TYPE_OBJECT: return "Object";
        }
        return "?";
    }

    /**
     * @return Number of entries that are complete and started.
     */
    public final int getEntryCount() { return _index + 1; }

    /**
     * @return Index of the currently processed entry, if any
     */
    public final int getCurrentIndex() { return (_index < 0) ? 0 : _index; }

    /**
     * Method that may be called to verify whether this context has valid index:
     * will return `false` before the first entry of Object context or before
     * first element of Array context; otherwise returns `true`.
     *
     * @since 2.9
     */
    public boolean hasCurrentIndex() { return _index >= 0; }

    /**
     * Method that may be called to check if this context is either:
     *<ul>
     * <li>Object, with at least one entry written (partially or completely)
     *  </li>
     * <li>Array, with at least one entry written (partially or completely)
     *  </li>
     *</ul>
     * and if so, return `true`; otherwise return `false`. Latter case includes
     * Root context (always), and Object/Array contexts before any entries/elements
     * have been read or written.
     *<p>
     * Method is mostly used to determine whether this context should be used for
     * constructing {@link JsonPointer}
     *
     * @since 2.9
     */
    public boolean hasPathSegment() {
        if (_type == TYPE_OBJECT) {
            return hasCurrentName();
        } else if (_type == TYPE_ARRAY) {
            return hasCurrentIndex();
        }
        return false;
    }
    
    /**
     * Method for accessing name associated with the current location.
     * Non-null for <code>FIELD_NAME</code> and value events that directly
     * follow field names; null for root level and array values.
     */
    public abstract String getCurrentName();

    /**
     * @since 2.9
     */
    public boolean hasCurrentName() { return getCurrentName() != null; }

    /**
     * Method for accessing currently active value being used by data-binding
     * (as the source of streaming data to write, or destination of data being
     * read), at this level in hierarchy.
     *<p>
     * Note that "current value" is NOT populated (or used) by Streaming parser or generator;
     * it is only used by higher-level data-binding functionality.
     * The reason it is included here is that it can be stored and accessed hierarchically,
     * and gets passed through data-binding.
     * 
     * @return Currently active value, if one has been assigned.
     * 
     * @since 2.5
     */
    public Object getCurrentValue() {
        return _value;
    }

    /**
     * Method to call to pass value to be returned via {@link #getCurrentValue}; typically
     * called indirectly through {@link JsonParser#setCurrentValue}
     * or {@link JsonGenerator#setCurrentValue}).
     * 
     * @since 2.5
     */
    public void setCurrentValue(Object v) { _value = v; }

    /**
     * Factory method for constructing a {@link JsonPointer} that points to the current
     * location within the stream that this context is for, excluding information about
     * "root context" (only relevant for multi-root-value cases)
     *
     * @since 2.9
     */
    public JsonPointer pathAsPointer() {
        return JsonPointer.forPath(this, false);
    }

    /**
     * Factory method for constructing a {@link JsonPointer} that points to the current
     * location within the stream that this context is for, optionally including
     * "root value index"
     *
     * @param includeRoot Whether root-value offset is included as the first segment or not;
     *    
     *
     * @since 2.9
     */
    public JsonPointer pathAsPointer(boolean includeRoot) {
        return JsonPointer.forPath(this, includeRoot);
    }
}
