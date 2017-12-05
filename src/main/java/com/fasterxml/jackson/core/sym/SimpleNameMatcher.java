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

    private SimpleNameMatcher(String[] names, int[] offsets, int mask) {
        super(names, offsets, mask, null);
    }

    protected SimpleNameMatcher(SimpleNameMatcher base, String[] nameLookup) {
        super(base, nameLookup);
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

    @Override
    protected int matchSecondary(String toMatch) {
        // Default implementation has no fallback so:
        return MATCH_UNKNOWN_NAME;
    }
}
