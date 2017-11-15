package com.fasterxml.jackson.core.sym;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.util.Named;

/**
 * Base class for various hash-based name matchers.
 *
 * @since 3.0
 */
abstract class NameMatcherBase
    extends FieldNameMatcher
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final int _mask;
    final int BOGUS_PADDING = 0; // for funsies
    protected final String[] _names;
    protected final int[] _offsets;

    protected NameMatcherBase(String[] names, int[] offsets, int mask) {
        _names = names;
        _offsets = offsets;
        _mask = mask;
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

    protected static List<String> stringsFromNames(List<Named> fields) {
        return fields.stream()
                .map(f -> (f == null) ? null : f.getName())
                .collect(Collectors.toList());
    }
    
}
