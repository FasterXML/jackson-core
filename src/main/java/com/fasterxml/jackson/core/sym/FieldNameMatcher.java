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

    /**
     * Mask used to get index from raw hash code, within hash area.
     */
    protected final int _mask;

    final int BOGUS_PADDING = 0; // just for aligning

    // // // Main hash area (ints) along with Strings it maps (sparse)
    
    protected final int[] _offsets;
    protected final String[] _names;

    // // // Original indexed Strings (dense) iff preserved

    protected final String[] _nameLookup;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    protected FieldNameMatcher(String[] names, int[] offsets, int mask,
            String[] nameLookup) {
        _names = names;
        _offsets = offsets;
        _mask = mask;
        _nameLookup = nameLookup;
    }

    protected FieldNameMatcher(FieldNameMatcher base, String[] nameLookup) {
        this(base._names, base._offsets, base._mask, nameLookup);
    }

    /*
    /**********************************************************************
    /* API: lookup by String
    /**********************************************************************
     */

    /**
     * Lookup method when caller does not guarantee that name to match has been
     * {@link String#intern}ed
     */
    public final int matchAnyName(String toMatch) {
        int ix = _hash(toMatch.hashCode(), _mask);
        String name = _names[ix];
        if (toMatch == name) {
            return _offsets[ix];
        }
        if (name != null) {
            if (toMatch.equals(name)) {
                return _offsets[ix];
            }
            // check secondary slot
            ix = (_mask + 1) + (ix >> 1);
            name = _names[ix];
            if (toMatch.equals(name)) {
                return _offsets[ix];
            }
            // or spill-over if need be
            if (name != null) {
                return _matchAnySpill(toMatch);
            }
        }
        return matchSecondary(toMatch);
    }

    private final int _matchAnySpill(String toMatch) {
        int ix = (_mask+1);
        ix += (ix>>1);

        for (int end = _names.length; ix < end; ++ix) {
            String name = _names[ix];

            if (toMatch.equals(name)) {
                return _offsets[ix];
            }
            if (name == null) {
                break;
            }
        }
        return matchSecondary(toMatch);
    }

    /**
     * Lookup method when caller guarantees that name to match has been
     * {@link String#intern}ed
     */
    public final int matchInternedName(String toMatch) {
        int ix = _hash(toMatch.hashCode(), _mask);
        String name = _names[ix];
        if (name == toMatch) {
            return _offsets[ix];
        }
        if (name != null) {
            // check secondary slot
            ix = (_mask + 1) + (ix >> 1);
            name = _names[ix];
            if (name == toMatch) {
                return _offsets[ix];
            }
            // or spill-over if need be
            if (name != null) {
                return _matchInternedSpill(toMatch);
            }
        }
        return matchSecondary(toMatch);
    }

    private final int _matchInternedSpill(String toMatch) {
        int ix = (_mask+1);
        ix += (ix>>1);

        for (int end = _names.length; ix < end; ++ix) {
            String name = _names[ix];
            if (name == toMatch) {
                return _offsets[ix];
            }
            if (name == null) {
                break;
            }
        }
        return matchSecondary(toMatch);
    }

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
    /* API: optional access to indexed Strings
    /**********************************************************************
     */

    /**
     * Accessor to names matching indexes, iff passed during construction.
     */
    public final String[] nameLookup() {
        return _nameLookup;
    }

    /*
    /**********************************************************************
    /* Methods for sub-classes to implement
    /**********************************************************************
     */

    /**
     * Secondary lookup method used for matchers that operate with more complex
     * matching rules, such as case-insensitive matchers.
     */
    protected abstract int matchSecondary(String toMatch);

    /*
    /**********************************************************************
    /* Helper methods for sub-classes
    /**********************************************************************
     */

    // For tests; gives rought count (may have slack at the end)
    public int spillCount() {
        int spillStart = (_mask+1) + ((_mask+1) >> 1);
        return _names.length - spillStart;
    }

    protected final static int _hash(int h, int mask) {
        return (h ^ (h >> 3)) & mask;
    }    

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
