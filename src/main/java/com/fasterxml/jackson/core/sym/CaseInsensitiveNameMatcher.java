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

    public static CaseInsensitiveNameMatcher constructFrom(List<Named> fields,
            boolean alreadyInterned)
    {
        return new CaseInsensitiveNameMatcher(SimpleNameMatcher.constructFrom(fields, alreadyInterned),
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
        // NOTE! We do NOT intern() secondary entries so make sure not to assume
        // inter()ing for secondary lookup
        return SimpleNameMatcher.construct(lcd);
    }

    @Override
    public final String[] nameLookup() {
        return null;
    }

    @Override
    public int matchAnyName(String name) {
        // First: see if non-modified name is matched by base implementation
        int match = _mainMatcher.matchAnyName(name);
        if (match >= 0) {
            return match;
        }
        // Important! May need to secondary lookup even if key does not change
        // since original name may have been lower-cases
        // (could try optimizing for case where all input was already lower case
        // and key too... but seems unlikely to be particularly common case)
        return _lowerCaseMatcher.matchAnyName(name.toLowerCase());
    }

    @Override
    public int matchInternedName(String name) {
        // 15-Nov-2017, tatu: we can try intern-based matching for primary one
        int match = _mainMatcher.matchInternedName(name);
        if (match >= 0) {
            return match;
        }
        // but not to secondary: neither are entries to-match intern()ed nor lower-cased here
        // (unless lower-casing makes difference)
        return _lowerCaseMatcher.matchAnyName(name.toLowerCase());
    }
}
