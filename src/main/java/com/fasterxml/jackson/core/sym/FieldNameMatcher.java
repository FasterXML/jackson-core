package com.fasterxml.jackson.core.sym;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.util.InternCache;
import com.fasterxml.jackson.core.util.Named;

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

    private final static InternCache INTERNER = InternCache.instance;
    /*
    /**********************************************************************
    /* API: lookup by String
    /**********************************************************************
     */

    /**
     * Lookup method when caller does not guarantee that name to match has been
     * {@link String#intern}ed
     */
    public abstract int matchAnyName(String name);

    /**
     * Lookup method when caller guarantees that name to match has been
     * {@link String#intern}ed
     */
    public abstract int matchInternedName(String name);

    /*
    /**********************************************************************
    /* API: lookup by quad-bytes
    /**********************************************************************
     */

    public int matchByQuad(int q1) { throw new UnsupportedOperationException(); }

    public int matchByQuad(int q1, int q2) { throw new UnsupportedOperationException(); }

    public int matchByQuad(int q1, int q2, int q3) { throw new UnsupportedOperationException(); }

    public int matchByQuad(int[] q, int qlen) { throw new UnsupportedOperationException(); }

    /*
    /**********************************************************************
    /* Helper methods for sub-classes
    /**********************************************************************
     */

    public static List<String> stringsFromNames(List<Named> fields,
            final boolean alreadyInterned) {
        return fields.stream()
                .map(n -> fromName(n, alreadyInterned))
                .collect(Collectors.toList());
    }

    protected static String fromName(Named n, boolean alreadyInterned) {
        if (n == null) return null;
        String name = n.getName();
        return alreadyInterned ? name : INTERNER.intern(name);
    }
}
