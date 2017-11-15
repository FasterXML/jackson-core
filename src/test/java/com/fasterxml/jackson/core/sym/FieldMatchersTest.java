package com.fasterxml.jackson.core.sym;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.util.Named;

/**
 * Tests for {@link FieldNameMatcher} implementations
 */
public class FieldMatchersTest extends BaseTest
{
    public void testMatching()
    {
        // First small (1 - 4)
        _testMatching("single");
        _testMatching("1", "2a");
        _testMatching("first", "second", "third");
        _testMatching("a", "bcd", "Fittipaldi", "goober");

        // then bit larger
        _testMatching("foo", "bar", "foobar", "fubar", "bizzbah", "grimagnoefwemp");
        // And then generate even bigger
        _testMatching(generate("base", 39));
        _testMatching(generate("Of ", 139));
        _testMatching(generate("ACE-", 499));
    }

    // Simple test to try to see if we can tweak hashing to limit overflow
    public void testSpillEfficiency()
    {
        _testSpillEfficiency(generate("base", 39), 4);
        _testSpillEfficiency(generate("Of ", 139), 24);
        _testSpillEfficiency(generate("ACE-", 499), 160);
    }

    private void _testSpillEfficiency(List<String> names, int expSpills) {
        FieldNameMatcher matcher = SimpleNameMatcher.construct(names, names.size());
        assertEquals(expSpills, ((SimpleNameMatcher) matcher).spillCount());
    }
    
    private List<String> generate(String base, int count) {
        List<String> result = new ArrayList<>(count);
        while (--count >= 0) {
            String name = base + count;
            // important for interned case
            result.add(name.intern());
        }
        return result;
    }

    private void _testMatching(String... nameArray) {
        _testMatching(Arrays.asList(nameArray));
    }

    private void _testMatching(List<String> names) {
        _testCaseSensitive(names);
        _testInterned(names);
        _testCaseInsensitive(names);
    }

    private void _testCaseSensitive(List<String> names)
    {
        FieldNameMatcher matcher = SimpleNameMatcher.construct(names, names.size());
        for (int i = 0; i < names.size(); ++i) {
            String name = names.get(i);
            _expectMatch(matcher, names, i);
            // similarly, if different string
            _expectMatch(matcher, names, i, new String(name));
            // but not with suffix
            _expectNonMatch(matcher, name+"FOOBAR");
        }
    }


    private void _testInterned(List<String> names)
    {
        FieldNameMatcher matcher = InternedNameMatcher.construct(names, names.size());
        for (int i = 0; i < names.size(); ++i) {
            String name = names.get(i);
            _expectMatch(matcher, names, i);
            // no match when passing non-interned, or different
            _expectNonMatch(matcher, new String(name));
            _expectNonMatch(matcher, name+"FOOBAR");
        }
    }

    private void _testCaseInsensitive(List<String> names)
    {
        FieldNameMatcher matcher = CaseInsensitiveNameMatcher.constructFrom(named(names));
        for (int i = 0; i < names.size(); ++i) {
            String name = names.get(i);
            _expectMatch(matcher, names, i);
            _expectMatch(matcher, names, i, new String(name));
            _expectMatch(matcher, names, i, name.toLowerCase());
            _expectMatch(matcher, names, i, name.toUpperCase());

            // but not if different
            _expectNonMatch(matcher, name+"FOOBAR");
        }
    }

    private void _expectMatch(FieldNameMatcher matcher, List<String> names, int index)
    {
        _expectMatch(matcher, names, index, names.get(index));
    }

    private void _expectMatch(FieldNameMatcher matcher, List<String> names, int index,
            String name)
    {
        int match = matcher.matchName(name);
        if (match != index) {
            fail("Should have matched #"+index+" (of "+names.size()+") for '"+name+"', did not, got: "+match);
        }
    }

    private void _expectNonMatch(FieldNameMatcher matcher, String name)
    {
        int match = matcher.matchName(name);
        if (match != FieldNameMatcher.MATCH_UNKNOWN_NAME) {
            fail("Should NOT have matched '"+name+"'; did match with index #"+match);
        }
    }

    private List<Named> named(List<String> names) {
        return names.stream().map(str -> new StringAsNamed(str))
                .collect(Collectors.toList());
    }

    static class StringAsNamed implements Named {
        private final String name;

        public StringAsNamed(String n) {
            name = n;
        }

        @Override
        public String getName() { return name; }
    }
}
