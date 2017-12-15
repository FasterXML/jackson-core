package com.fasterxml.jackson.core.sym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.BaseTest;

public class TextualNameHashTest extends BaseTest
{
    public void testSuffix1() {
        // 14-Nov-2017, tatu: Slightly optimized hashing with shifting, to reduce
        //   default collision counts
        _testSpillEfficiency(generate("", 99), 0, 0);
    }

    public void testSuffix2() {
        _testSpillEfficiency(generate("base", 39), 7, 1);
    }

    public void testSuffix3() {
        _testSpillEfficiency(generate("Of ", 139), 14, 6);
    }

    public void testSuffix4() {
        _testSpillEfficiency(generate("ACE-", 499), 71, 34);
    }

    public void testSuffix5() {
        // similarly, not so great...
        _testSpillEfficiency(generate("SlartiBartFast#", 3000), 479, 291);
    }
        
    public void testPrefix1() {
        _testSpillEfficiency(generate2("", 99), 0, 0);
    }
    public void testPrefix2() {
        _testSpillEfficiency(generate2("base", 39), 5, 0);
    }
    public void testPrefix3() {
        _testSpillEfficiency(generate2("Of ", 139), 15, 0);
    }
    public void testPrefix4() {
        _testSpillEfficiency(generate2("ACE-", 499), 54, 3);
    }

    public void testMisc11() {
        _testSpillEfficiency(Arrays.asList(
                "player", "uri", "title", "width",
                "height", "format", "duration", "size",
                "bitrate", "copyright", "persons"),
                1, 0);
    }

    public void testMisc5() {
        _testSpillEfficiency(Arrays.asList("uri", "title", "width", "height", "size"),
                2, 0);
    }

    public void testMisc2() {
        _testSpillEfficiency(Arrays.asList("content", "images"), 0, 0);
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

    private void _testSpillEfficiency(List<String> names,
            int expSecondary, int expSpills) {
        SimpleNameMatcher matcher = SimpleNameMatcher.construct(names);
        int sec = matcher.secondaryCount();
        int spills = matcher.spillCount();

        if ((expSecondary != sec) || (expSpills != spills)) {
            fail("Expected "+expSecondary+" secondary, "+expSpills+" spills; got "+sec+" / "+spills);
        }
    }
}
