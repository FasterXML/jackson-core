package com.fasterxml.jackson.core.sym;

import java.util.*;

import com.fasterxml.jackson.core.util.Named;

/**
 * Basic {@link FieldNameMatcher} that uses case-sensitive match and does
 * not require (or expect) that names passed as arguments have been
 * {@link String#intern}ed.
 */
public final class SimpleNameMatcher
    extends NameMatcherBase
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private SimpleNameMatcher(String[] names, int[] offsets, int mask) {
        super(names, offsets, mask);
    }

    public static FieldNameMatcher constructFrom(List<Named> fields) {
        List<String> names = new ArrayList<>(fields.size());
        int count = 0;
        for (Named n : fields) {
            if (n != null) {
                names.add(n.getName());
                ++count;
            } else {
                names.add(null);
            }
        }
        return construct(names, count);
    }

    public static FieldNameMatcher construct(List<String> fieldNames,
            int nonNullCount)
    {
        if (nonNullCount <= 4) {
            return Small.construct(fieldNames);
        }
        final int hashSize = findSize(nonNullCount);
        final int allocSize = hashSize + (hashSize>>1);

        String[] names = new String[allocSize];
        int[] offsets = new int[allocSize];

        // Alas: can not easily extract out without tuples or such since names/offsets need resizing...
        final int mask = hashSize-1;
        int spillPtr = names.length;

        for (int i = 0, fcount = fieldNames.size(); i < fcount; ++i) {
            String name = fieldNames.get(i);
            if (name == null) {
                continue;
            }
            int ix = _hash(name, mask);
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
        return new SimpleNameMatcher(names, offsets, mask);
    }

private final static int _hash(String str, int mask) {
    int h = str.hashCode();
    return (h ^ (h >> 3)) & mask;
}

    @Override
    public int matchName(String toMatch) {
        int ix = _hash(toMatch, _mask);
        String name = _names[ix];
        if ((toMatch == name) || toMatch.equals(name)) {
            return _offsets[ix];
        }
        if (name != null) {
            // check secondary slot
            ix = (_mask + 1) + (ix >> 1);
            name = _names[ix];
            if (toMatch.equals(name)) {
                return _offsets[ix];
            }
            // or spill-over if need be
            if (name != null) {
                return _matchSpill(toMatch);
            }
        }
        return MATCH_UNKNOWN_NAME;
    }

    private final int _matchSpill(String toMatch) {
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
        return MATCH_UNKNOWN_NAME;
    }

    // For tests; gives rought count (may have slack at the end)
    public int spillCount() {
        int spillStart = (_mask+1) + ((_mask+1) >> 1);
        return _names.length - spillStart;
    }
    
    /*
    /**********************************************************************
    /* Specialized matcher for small number of fields
    /**********************************************************************
     */

    private final static class Small extends FieldNameMatcher
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final String a, b, c, d;

        private Small(String f1, String f2, String f3, String f4) {
            a = f1;
            b = f2;
            c = f3;
            d = f4;
        }

        public static Small construct(List<String> fields) {
            return new Small(_get(fields, 0), _get(fields, 1),
                    _get(fields, 2), _get(fields, 3));
        }

        private static String _get(List<String> fields, int index) {
            return (index < fields.size()) ? fields.get(index) : null;
        }

        @Override
        public int matchName(String name) {
            if (name.equals(a)) {
                return 0;
            }
            if (name.equals(b)) {
                return 1;
            }
            if (name.equals(c)) {
                return 2;
            }
            if (name.equals(d)) {
                return 3;
            }
            return FieldNameMatcher.MATCH_UNKNOWN_NAME;
        }
    }
}
