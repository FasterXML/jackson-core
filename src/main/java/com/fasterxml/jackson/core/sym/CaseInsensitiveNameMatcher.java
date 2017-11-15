package com.fasterxml.jackson.core.sym;

import java.util.*;

import com.fasterxml.jackson.core.util.Named;

/**
 * {@link FieldNameMatcher} that uses case-insensitive match. This is implemented
 * by adding both original name as-is, and its lower-case version (if different),
 * and doing lookups for both.
 */
public class CaseInsensitiveNameMatcher
    extends FieldNameMatcher
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final FieldNameMatcher _mainMatcher;
    protected final FieldNameMatcher _lowerCaseMatcher;
    
    protected CaseInsensitiveNameMatcher(FieldNameMatcher mainMatcher,
            FieldNameMatcher lcMatcher) {
        _mainMatcher = mainMatcher;
        _lowerCaseMatcher = lcMatcher;
    }

    public static CaseInsensitiveNameMatcher constructFrom(List<Named> fields)
    {
        return new CaseInsensitiveNameMatcher(SimpleNameMatcher.constructFrom(fields),
                _constructWithLC(fields));
    }

    private static FieldNameMatcher _constructWithLC(List<Named> fields)
    {
        List<String> lcd = new ArrayList<>(fields.size());
        for (Named n : fields) {
            // Important! MUST include even if not different because lookup
            // key may be lower-cased after primary access
            lcd.add((n == null) ? null : n.getName().toLowerCase());
        }
        return SimpleNameMatcher.construct(lcd);
    }

    @Override
    public int matchName(String name) {
        // First: see if non-modified name is matched by base implementation
        int match = _mainMatcher.matchName(name);
        if (match >= 0) {
            return match;
        }
        // Important! May need to secondary lookup even if key does not change
        // since original name may have been lower-cases
        // (could try optimizing for case where all input was already lower case
        // and key too... but seems unlikely to be particularly common case)
        return _lowerCaseMatcher.matchName(name.toLowerCase());
    }
}
