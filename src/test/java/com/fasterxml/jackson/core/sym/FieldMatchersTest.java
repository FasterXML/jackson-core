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
    public void testSmallMatching()
    {
        // First small (1 - 4)
        _testMatching("single");
        _testMatching("1", "2a");
        _testMatching("first", "second", "third");
        // ... with nulls
        _testMatching(null, "b", null);
    }

    public void testMediumMatching()
    {
        _testMatching("a", "bcd", "Fittipaldi", "goober");
        _testMatching("a", "bcd", null, "goober");
        // important: non-null size still small, but full size big(ger)
        _testMatching("a", null, null, "goober", "xyz");

        // then bit larger
        _testMatching("foo", "bar", "foobar", "fubar", "bizzbah", "grimagnoefwemp");

        _testMatching("a", "b", "c", "d", "E", "f", "G", "h");
        _testMatching("a", "b", null, "d", "E", "f", "G", null);
    }
        
    public void testLargeMatching()
    {
        // And then generate even bigger
        _testMatching(generate("base", 39));
        _testMatching(generate("Of ", 139));
        _testMatching(generate("ACE-", 499));

        List<String> names = generate("ACE-", 250);
        names.set(27, null);
        names.set(111, null);
    }

    // Simple test to try to see if we can tweak hashing to limit overflow
    public void testSpillEfficiency()
    {
        // 14-Nov-2017, tatu: Slightly optimized hashing with shifting, to reduce
        //   default collision counts

        _testSpillEfficiency(generate("", 99), 4);
        _testSpillEfficiency(generate("base", 39), 4);
        _testSpillEfficiency(generate("Of ", 139), 8);
        _testSpillEfficiency(generate("ACE-", 499), 32);

        _testSpillEfficiency(generate2("", 99), 4);
        _testSpillEfficiency(generate2("base", 39), 0);
        _testSpillEfficiency(generate2("Of ", 139), 4);
        _testSpillEfficiency(generate2("ACE-", 499), 4);
    }

    private void _testSpillEfficiency(List<String> names, int expSpills) {
        FieldNameMatcher matcher = SimpleNameMatcher.construct(names);
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

    private List<String> generate2(String base, int count) {
        List<String> result = new ArrayList<>(count);
        while (--count >= 0) {
            String name = ""+ count + base;
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
        _testCaseInsensitive(names);
        _testInterned(names);
    }

    private void _testCaseSensitive(List<String> names)
    {
        FieldNameMatcher matcher = SimpleNameMatcher.construct(names);
        for (int i = 0; i < names.size(); ++i) {
            String name = names.get(i);
            if (name != null) {
                _expectAnyMatch(matcher, names, i);
                // similarly, if different string
                _expectAnyMatch(matcher, names, i, new String(name));
                // but not with suffix
                _expectNonMatch(matcher, name+"FOOBAR");
            }
        }
    }

    private void _testInterned(List<String> names)
    {
        FieldNameMatcher matcher = SimpleNameMatcher.construct(names);
        for (int i = 0; i < names.size(); ++i) {
            String name = names.get(i);
            if (name != null) {
                // should match both ways really
                _expectAnyMatch(matcher, names, i);
                _expectInternedMatch(matcher, names, i);

                // no match when passing non-interned, or different
                _expectInternedNonMatch(matcher, new String(name));
                _expectNonMatch(matcher, name+"FOOBAR");
            }
        }
    }

    private void _testCaseInsensitive(List<String> names)
    {
        FieldNameMatcher matcher = CaseInsensitiveNameMatcher.constructFrom(named(names), true);
        for (int i = 0; i < names.size(); ++i) {
            String name = names.get(i);
            if (name != null) {
                _expectAnyMatch(matcher, names, i);
                _expectAnyMatch(matcher, names, i, new String(name));
                _expectAnyMatch(matcher, names, i, name.toLowerCase());
                _expectAnyMatch(matcher, names, i, name.toUpperCase());
    
                // but not if different
                _expectNonMatch(matcher, name+"FOOBAR");
            }
        }
    }

    private void _expectAnyMatch(FieldNameMatcher matcher, List<String> names, int index)
    {     
        String name = names.get(index);
        if (name != null) {
            _expectAnyMatch(matcher, names, index, name);
        }
    }

    private void _expectAnyMatch(FieldNameMatcher matcher, List<String> names, int index,
            String name)
    {
        if (name == null) {
            return;
        }
        int match = matcher.matchAnyName(name);
        if (match != index) {
            fail("Should have any-matched #"+index+" (of "+names.size()+") for '"+name+"', did not, got: "+match);
        }
    }

    private void _expectInternedMatch(FieldNameMatcher matcher, List<String> names, int index)
    {
        String name = names.get(index);
        if (name != null) {
            _expectInternedMatch(matcher, names, index, name);
        }
    }

    private void _expectInternedMatch(FieldNameMatcher matcher, List<String> names, int index,
            String name)
    {
        if (name == null) {
            return;
        }
        int match = matcher.matchInternedName(name);
        if (match != index) {
            fail("Should have intern-matched #"+index+" (of "+names.size()+"; matcher "+
                    matcher.getClass().getName()+") for '"+name+"', did not, got: "+match);
        }
    }

    private void _expectNonMatch(FieldNameMatcher matcher, String name)
    {
        if (name == null) {
            return;
        }
        // make sure to test both intern() and non-intern paths
        int match = matcher.matchAnyName(name);
        if (match != FieldNameMatcher.MATCH_UNKNOWN_NAME) {
            fail("Should NOT have any-matched '"+name+"'; did match with index #"+match);
        }
        _expectInternedNonMatch(matcher, name);
    }

    private void _expectInternedNonMatch(FieldNameMatcher matcher, String name)
    {
        if (name != null) {
            // make sure to test both intern() and non-intern paths
            int match = matcher.matchInternedName(name);
            if (match != FieldNameMatcher.MATCH_UNKNOWN_NAME) {
                fail("Should NOT have intern-matched '"+name+"'; did match with index #"+match);
            }
        }
    }
    
    private List<Named> named(List<String> names) {
        return names.stream().map(StringAsNamed::construct)
                .collect(Collectors.toList());
    }

    static class StringAsNamed implements Named {
        private final String name;

        public StringAsNamed(String n) {
            name = n;
        }

        public static StringAsNamed construct(String str){
            if (str == null) {
                return null;
            }
            return new StringAsNamed(str);
        }

        @Override
        public String getName() { return name; }
    }
}
