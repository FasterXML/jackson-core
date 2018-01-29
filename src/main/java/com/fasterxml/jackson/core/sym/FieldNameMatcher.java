package com.fasterxml.jackson.core.sym;

import java.util.ArrayList;
import java.util.List;

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

    private final static InternCache INTERNER = InternCache.instance;
    
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

    // // // Original indexed Strings (dense) iff preserved

    protected final String[] _nameLookup;

    // // // Backup index, mostly for case-insensitive lookups

    protected final FieldNameMatcher _backupMatcher;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    protected FieldNameMatcher(FieldNameMatcher backup, String[] nameLookup)
    {
        _backupMatcher = backup;
        _nameLookup = nameLookup;
    }

    /*
    /**********************************************************************
    /* API: lookup by String
    /**********************************************************************
     */

    /**
     * Lookup method that does not assume name to be matched to be
     * {@link String#intern}ed (although passing interned String is likely
     * to result in more efficient matching).
     */
    public abstract int matchName(String toMatch);

    /*
    /**********************************************************************
    /* API: lookup by quad-bytes
    /**********************************************************************
     */

    public abstract int matchByQuad(int q1);

    public abstract int matchByQuad(int q1, int q2);

    public abstract int matchByQuad(int q1, int q2, int q3);

    public abstract int matchByQuad(int[] q, int qlen);

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
    protected int matchSecondary(String toMatch) {
        if (_backupMatcher == null) {
            return MATCH_UNKNOWN_NAME;
        }
        // 04-Dec-2017, tatu: Note that we absolutely MUST do another lookup even if
        //   key does not change; thing being that we are now using secondary index,
        //   contents of which MAY be different from primary one. Specifically, if original
        //   keys are not all lower-case, we would induce a miss if skipping lookup here.
        return _backupMatcher.matchName(toMatch.toLowerCase());
    }

    /*
    /**********************************************************************
    /* Helper methods for sub-classes
    /**********************************************************************
     */

    protected final static int _hash(int h, int mask) {
        // for some reason, slight shuffle with add (not xor!) works quite well
        return (h + (h >> 3)) & mask;
    }

    protected static int _findSize(int size) {
        if (size <= 5) return 8;
        if (size <= 11) return 16;
        if (size <= 23) return 32;
        int needed = size + (size >> 2) + (size >> 4); // at most 75% full
        int result = 64;
        while (result < needed) {
            result += result;
        }
        return result;
    }

    public static List<String> stringsFromNames(List<Named> fields,
            final boolean alreadyInterned) {
        // 29-Jan-2018, tatu: With seemingly simple definition (commented out) getting
        //   strange "java.lang.NoClassDefFoundError: Could not initialize class java.util.stream.StreamOpFlag"
        //   so having to replace with bit different
        /*
        return fields.stream()
                .map(n -> _fromName(n, alreadyInterned))
                .collect(Collectors.toList());
                */
        ArrayList<String> result = new ArrayList<String>(fields.size());
        for (Named n : fields) {
            result.add(_fromName(n, alreadyInterned));
        }
        return result;
    }

    protected static String _fromName(Named n, boolean alreadyInterned) {
        if (n == null) return null;
        String name = n.getName();
        return alreadyInterned ? name : INTERNER.intern(name);
    }

    protected static List<String> _lc(List<String> src) {
        List<String> lcd = new ArrayList<>(src.size());
        for (String n : src) {
            lcd.add((n == null) ? null : n.toLowerCase());
        }
        return lcd;
    }
}
