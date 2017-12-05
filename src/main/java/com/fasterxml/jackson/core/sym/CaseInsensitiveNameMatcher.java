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

    protected final FieldNameMatcher _lowerCaseMatcher;
    
    protected CaseInsensitiveNameMatcher(FieldNameMatcher mainMatcher,
            FieldNameMatcher lcMatcher) {
        super(mainMatcher, null);
        _lowerCaseMatcher = lcMatcher;
    }

    public static CaseInsensitiveNameMatcher constructFrom(List<Named> fields,
            boolean alreadyInterned)
    {
        SimpleNameMatcher primary = SimpleNameMatcher.constructFrom(fields, alreadyInterned);
        return new CaseInsensitiveNameMatcher(primary, _constructWithLC(fields));
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
    protected int matchSecondary(String toMatch) {
        final String key = toMatch.toLowerCase();
        // 04-Dec-2017, tatu: Note that we absolutely MUST do another lookup even if
        //   key does not change; thing being that we are now using secondary index,
        //   contents of which MAY be different from primary one. Specifically, if original
        //   keys are not all lower-case, we would induce a miss if skipping lookup here.
        return _lowerCaseMatcher.matchAnyName(key);
    }
}
