package com.fasterxml.jackson.core.sym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.BaseTest;

public class BinaryNameMatcherTest extends BaseTest
{
    public void testSmallMatching()
    {
        // First small (1 - 4)
        _testMatching("single");
        _testMatching("1", "2a");
        _testMatching("first", "secondlong", "3rd");
    }

    public void testMediumMatching()
    {
        _testMatching("a", "bcd", "Fittipaldi", "goober");

        // then bit larger
        _testMatching("foo", "bar", "foobar", "fubar", "bizzbah", "grimagnoefwemp");

        _testMatching("a", "b", "c", "d", "E", "f", "G", "h");
        _testMatching("a", "b", "d", "E", "f", "G");
        _testMatching("areaNames", "audienceSubCategoryNames", "blockNames", "seatCategoryNames", "subTopicNames",
                "subjectNames", "topicNames", "topicSubTopics", "venueNames", "events", "performances");
    }

    public void testLargeMatching()
    {
        // And then generate even bigger
        _testMatching(generate("base", 39));
        _testMatching(generate("Of ", 139));
        _testMatching(generate("ACE-", 499));

        // but ALSO make sure to use longer strings -- need longer pre/suffix
        _testMatching(generate2("-longer", 999)); // 9-12 bytes -> 3 quads
        _testMatching(generate("slartibartfast-", 3000));
    }

    private List<String> generate(String base, int count) {
        List<String> result = new ArrayList<>(count);
        while (--count >= 0) {
            String name = base + count;
            result.add(name.intern());
        }
        return result;
    }

    private List<String> generate2(String base, int count) {
        List<String> result = new ArrayList<>(count);
        while (--count >= 0) {
            String name = ""+ count + base;
            result.add(name.intern());
        }
        return result;
    }
    
    private void _testMatching(String... nameArray) {
        _testMatching(Arrays.asList(nameArray));
    }

    private void _testMatching(List<String> names)
    {
        BinaryNameMatcher matcher = _construct(names);
        for (int i = 0; i < names.size(); ++i) {
            _expectMatch(matcher, names, i);
            // but not with suffix
            _expectNonMatch(matcher, names.get(i)+"FOOBAR");
        }
    }

    private BinaryNameMatcher _construct(List<String> names)
    {
        // note: strings MUST be intern()ed already
        BinaryNameMatcher matcher = BinaryNameMatcher.construct(names);
        // ensure `toString()` is legal (just for funsies)
        assertNotNull(matcher.toString());
        return matcher;
    }

    private void _expectMatch(BinaryNameMatcher matcher, List<String> names, int index)
    {     
        String name = names.get(index);
        _expectMatch(matcher, names, index, name);
    }

    private void _expectMatch(BinaryNameMatcher matcher, List<String> names, int index,
            String name)
    {
        int match = _match(matcher, name);
        if (match != index) {
            fail("Should have found '"+name+"' (index #"+index+" of total of "+names.size()+"), didn't: got "+match);
        }
        // secondary: needs to work via backup-lookup
        int match2 = matcher.matchName(name);
        if (match2 != index) {
            fail("Should have found '"+name+"' (index #"+index+" of total of "+names.size()+") via String lookup: instead got "+match2);
        }
    }

    private void _expectNonMatch(BinaryNameMatcher matcher, String name)
    {
       int match = _match(matcher, name);
        if (match != -1) {
            fail("Should NOT have any-matched '"+name+"'; did match as: "+match);
        }
    }

    private int _match(BinaryNameMatcher matcher, String name)
    {
        int[] quads = BinaryNameMatcher._quads(name);
        switch (quads.length) {
        case 1:
            return matcher.matchByQuad(quads[0]);
        case 2:
            return matcher.matchByQuad(quads[0], quads[1]);
        case 3:
            return matcher.matchByQuad(quads[0], quads[1], quads[2]);
        default:
            return matcher.matchByQuad(quads, quads.length);
        }
    }
}
