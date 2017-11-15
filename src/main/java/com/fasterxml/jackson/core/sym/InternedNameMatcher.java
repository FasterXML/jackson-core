package com.fasterxml.jackson.core.sym;

import java.util.*;

import com.fasterxml.jackson.core.sym.FieldNameMatcher;
import com.fasterxml.jackson.core.util.Named;

/**
 * Specialized {@link FieldNameMatcher} implementation that operates on field names
 * that are {@link String#intern}ed.
 *
 * @since 3.0
 */
public final class InternedNameMatcher
    extends NameMatcherBase
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private InternedNameMatcher(String[] names, int[] offsets, int mask) {
        super(names, offsets, mask);
    }

    public static FieldNameMatcher constructFrom(List<Named> fields) {
        return construct(stringsFromNames(fields));
    }
    
    public static FieldNameMatcher construct(List<String> fieldNames)
    {
        final int fieldCount = fieldNames.size();
        if (fieldCount <= Small.MAX_FIELDS) {
            return Small.construct(fieldNames);
        }
        final int hashSize = findSize(fieldCount);
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
            int ix = name.hashCode() & mask;
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
            if (names.length >= spillPtr) {
                int newLength = names.length + 4;
                names = Arrays.copyOf(names, newLength);
                offsets = Arrays.copyOf(offsets, newLength);
            }
            names[spillPtr] = name;
            offsets[spillPtr] = i;
            ++spillPtr;
        }
        return new InternedNameMatcher(names, offsets, mask);
    }
    
    @Override
    public int matchName(String toMatch) {
        int ix = toMatch.hashCode() & _mask;
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
            if (name == toMatch) {
                return _offsets[ix];
            }
            if (name == null) {
                break;
            }
        }
        return MATCH_UNKNOWN_NAME;
    }

    /*
    /**********************************************************************
    /* Specialized matcher for small number of fields
    /**********************************************************************
     */

    /**
     * Compact implementation for small lookups: threshold chosen to balance costlier
     * lookup (must check equality for all) with more compact representation and
     * avoidance of hash code access, usage.
     */
    private final static class Small extends FieldNameMatcher
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        final static int MAX_FIELDS = 3;

        protected final String a, b, c, d;

        private Small(String f1, String f2, String f3, String f4) {
            a = f1;
            b = f2;
            c = f3;
            d = f4;
        }

        public static Small construct(List<String> fields) {
                return new Small(_get(fields, 0),  _get(fields, 1),
                    _get(fields, 2), _get(fields, 3));
        }

        private static String _get(List<String> fields, int index) {
            return (index < fields.size()) ? fields.get(index) : null;
        }

        @Override
        public int matchName(String name) {
            if (name == a) return 0;
            if (name == b) return 1;
            if (name == c) return 2;
            if (name == d) return 3;
            return FieldNameMatcher.MATCH_UNKNOWN_NAME;
        }
    }
}
