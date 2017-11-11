package com.fasterxml.jackson.core.sym;

/**
 * Interface for implementations used for efficient matching of field names from
 * input stream (via parser) to higher-level abstractions like properties that
 * databind uses. Used to avoid two-phase lookups -- first from input stream to
 * strings; then from strings to entities -- but details may heavily depend on
 * format parser (some formats can optimize better than others).
 *
 * @since 3.0
 */
public abstract class FieldNameMatcher
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Marker for case where <code>JsonToken.END_OBJECT</code> encountered.
     */
    public final static int MATCH_END_OBJECT = -1;

    /**
     * Marker for case where field name encountered but not one of matches.
     */
    public final static int MATCH_UNKNOWN_NAME = -2;

    /**
     * Marker for case where token encountered is neither <code>FIELD_NAME</code>
     * nor <code>END_OBJECT</code>.
     */
    public final static int MATCH_ODD_TOKEN = -3;

    // // // API
    
    public abstract int matchName(String name);
}
