package com.fasterxml.jackson.core.sym;

import java.util.*;

import com.fasterxml.jackson.core.util.Named;

/**
 * Basic {@link FieldNameMatcher} that uses case-sensitive match and does
 * not require (or expect) that names passed as arguments have been
 * {@link String#intern}ed.
 */
public class SimpleNameMatcher
    extends FieldNameMatcher
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final int _mask;
    final int BOGUS_PADDING = 0; // for funsies
    protected final String[] _names;
    protected final int[] _offsets;

    private SimpleNameMatcher(String[] names, int[] offsets, int mask) {
        _names = names;
        _offsets = offsets;
        _mask = mask;
    }

    protected SimpleNameMatcher(SimpleNameMatcher base) {
        _mask = base._mask;
        _names = base._names;
        _offsets = base._offsets;
    }

    public static SimpleNameMatcher constructFrom(List<Named> fields,
            boolean alreadyInterned) {
        return construct(stringsFromNames(fields, alreadyInterned));
    }

    protected static int findSize(int size) {
        if (size <= 6) return 8;
        if (size <= 12) return 16;
        int needed = size + (size >> 2); // at most 80% full
        int result = 32;
        while (result < needed) {
            result += result;
        }
        return result;
    }

    public static SimpleNameMatcher construct(List<String> fieldNames)
    {
        final int fieldCount = fieldNames.size();
        /*
        if (fieldCount <= Small.MAX_FIELDS) {
            return Small.construct(fieldNames);
        }
        */
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
        return new SimpleNameMatcher(names, offsets, mask);
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    @Override
    public final String[] indexedStrings() {
        return _names;
    }

    @Override
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
        return MATCH_UNKNOWN_NAME;
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
        return MATCH_UNKNOWN_NAME;
    }

    @Override
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
        return MATCH_UNKNOWN_NAME;
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
        return MATCH_UNKNOWN_NAME;
    }

    // For tests; gives rought count (may have slack at the end)
    public int spillCount() {
        int spillStart = (_mask+1) + ((_mask+1) >> 1);
        return _names.length - spillStart;
    }

    private final static int _hash(int h, int mask) {
        return (h ^ (h >> 3)) & mask;
    }
    /*
    private final static int _hash(String str, int mask) {
        int h = str.hashCode();
        return (h ^ (h >> 3)) & mask;
    }
    */

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
    /*
    private final static class Small extends FieldNameMatcher
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        final static int MAX_FIELDS = 3;
        
        protected final String _f1, _f2, _f3;

        private Small(String f1, String f2, String f3) {
            _f1 = f1;
            _f2 = f2;
            _f3 = f3;
        }

        public static Small construct(List<String> fields) {
            return new Small(_get(fields, 0), _get(fields, 1), _get(fields, 2));
        }

        private static String _get(List<String> fields, int index) {
            return (index < fields.size()) ? fields.get(index) : null;
        }

        @Override
        public int matchAnyName(String name) {
            if (name.equals(_f1)) return 0;
            if (name.equals(_f2)) return 1;
            if (name.equals(_f3)) return 2;
            return FieldNameMatcher.MATCH_UNKNOWN_NAME;
        }

        @Override
        public int matchInternedName(String name) {
            if (name == _f1) return 0;
            if (name == _f2) return 1;
            if (name == _f3) return 2;
            return FieldNameMatcher.MATCH_UNKNOWN_NAME;
        }
    }
    */
}
