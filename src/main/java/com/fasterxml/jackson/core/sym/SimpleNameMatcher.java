package com.fasterxml.jackson.core.sym;

import java.util.*;

import com.fasterxml.jackson.core.util.Named;

/**
 * Basic {@link FieldNameMatcher} that uses case-sensitive match and does
 * not require (or expect) that names passed as arguments have been
 * {@link String#intern}ed.
 */
public class SimpleNameMatcher
    extends HashedMatcherBase
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private SimpleNameMatcher(Locale locale, String[] names, int[] offsets, int mask) {
        super(locale, names, offsets, mask, null, null);
    }

    protected SimpleNameMatcher(SimpleNameMatcher base, String[] nameLookup) {
        super(base, nameLookup);
    }

    protected SimpleNameMatcher(SimpleNameMatcher primary, SimpleNameMatcher secondary) {
        super(primary, secondary);
    }

    /**
     * Factory method for constructing case-sensitive matcher that only supports
     * matching from `String`.
     */
    public static SimpleNameMatcher constructFrom(Locale locale,
            List<Named> fields, boolean alreadyInterned) {
        return construct(locale, stringsFromNames(fields, alreadyInterned));
    }

    /**
     * Factory method for constructing case-sensitive matcher that only supports
     * matching from `String`.
     */
    public static SimpleNameMatcher construct(Locale locale, List<String> fieldNames)
    {
        final int fieldCount = fieldNames.size();
        final int hashSize = _findSize(fieldCount);
        final int allocSize = hashSize + (hashSize>>1);

        String[] names = new String[allocSize];
        int[] offsets = new int[allocSize];
        // 20-Dec-2017, tatu: Let's initialize to value "not found" just in case, since `0` would
        //    be valid value, indicating first entry
        Arrays.fill(offsets, FieldNameMatcher.MATCH_UNKNOWN_NAME); // since we are never called if there's no name involved

        // Alas: can not easily extract out without tuples or such since names/offsets need resizing...
        final int mask = hashSize-1;
        int spillPtr = names.length;

        for (int i = 0, fcount = fieldNames.size(); i < fcount; ++i) {
            String name = fieldNames.get(i);
            if (name == null) {
                continue;
            }
            int ix = _hash(name.hashCode(), mask);
            if (names[ix] == null) {
                names[ix] = name;
                offsets[ix] = i;
                continue;
            }
            ix = (mask+1) + (ix >> 1);
            if (names[ix] == null) {
                names[ix] = name;
                offsets[ix] = i;
                continue;
            }
            if (names.length == spillPtr) {
                int newLength = names.length + 4;
                names = Arrays.copyOf(names, newLength);
                offsets = Arrays.copyOf(offsets, newLength);
            }
            names[spillPtr] = name;
            offsets[spillPtr] = i;
            ++spillPtr;
        }
        return new SimpleNameMatcher(locale, names, offsets, mask);
    }

    public static SimpleNameMatcher constructCaseInsensitive(Locale locale,
            List<Named> fields, boolean alreadyInterned) {
        return constructCaseInsensitive(locale, stringsFromNames(fields, alreadyInterned));
    }

    public static SimpleNameMatcher constructCaseInsensitive(Locale locale,
            List<String> names)
    {
        return new SimpleNameMatcher(SimpleNameMatcher.construct(locale, names),
                SimpleNameMatcher.construct(locale, _lc(locale, names)));
    }

    // // // Not implemented by this matcher, but not an error to call (caller won't know)

    @Override
    public int matchByQuad(int q1) { return MATCH_UNKNOWN_NAME; }

    @Override
    public int matchByQuad(int q1, int q2) { return MATCH_UNKNOWN_NAME; }

    @Override
    public int matchByQuad(int q1, int q2, int q3) { return MATCH_UNKNOWN_NAME; }

    @Override
    public int matchByQuad(int[] q, int qlen) { return MATCH_UNKNOWN_NAME; }
}
