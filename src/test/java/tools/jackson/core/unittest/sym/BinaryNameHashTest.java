package tools.jackson.core.unittest.sym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tools.jackson.core.sym.BinaryNameMatcher;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Specific tests that try to verify proper hashing goodness
public class BinaryNameHashTest extends JacksonCoreTestBase
{
    public void testSuffix1() {
        // 14-Nov-2017, tatu: Slightly optimized hashing with shifting, to reduce
        //   default collision counts
        _testSpillEfficiency(generate("", 99), 77, 16, 6, 0);
    }

    public void testSuffix2() {
        _testSpillEfficiency(generate("base", 39), 33, 6, 0, 0);
    }

    public void testSuffix3() {
        _testSpillEfficiency(generate("Of ", 139), 122, 16, 1, 0);
    }

    public void testSuffix4() {
        _testSpillEfficiency(generate("ACE-", 499), 422, 66, 11, 0);
    }

    public void testSuffix5() {
        // similarly, not so great...
        _testSpillEfficiency(generate("SlartiBartFast#", 3000), 1112, 761, 897, 230);
    }

    public void testPrefix1() {
        _testSpillEfficiency(generate2("", 99), 77, 16, 6, 0);
    }
    public void testPrefix2() {
        _testSpillEfficiency(generate2("base", 39), 29, 8, 2, 0);
    }
    public void testPrefix3() {
        _testSpillEfficiency(generate2("Of ", 139), 116, 16, 7, 0);
    }
    public void testPrefix4() {
        _testSpillEfficiency(generate2("ACE-", 499), 384, 92, 23, 0);
    }

    public void testMisc11() {
        _testSpillEfficiency(Arrays.asList(
                "player", "uri", "title", "width",
                "height", "format", "duration", "size",
                "bitrate", "copyright", "persons"),
                11, 0, 0, 0);
    }

    public void testMisc5() {
        _testSpillEfficiency(Arrays.asList("uri", "title", "width", "height", "size"),
                5, 0, 0, 0);
    }

    public void testMisc2() {
        _testSpillEfficiency(Arrays.asList("content", "images"),
                2, 0, 0, 0);
    }

    private void _testSpillEfficiency(List<String> names,
            int prim, int sec, int ter,
            int expSpills)
    {
        BinaryNameMatcher quads = _construct(names);
        assertEquals(names.size(), quads.totalCount());

        assertEquals(prim, quads.primaryQuadCount(), "Primary count not matching");
        assertEquals(sec, quads.secondaryQuadCount(), "Secondary count not matching");
        assertEquals(ter, quads.tertiaryQuadCount(), "Tertiary count not matching");
        assertEquals(expSpills, quads.spilloverQuadCount(), "Spill count not matching");
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

    private BinaryNameMatcher _construct(List<String> names) {
        return BinaryNameMatcher.construct(names);
        /*
        BinaryNameMatcher m = BinaryNameMatcher.construct(names);
        System.err.println("DEBUG: matcher == "+m);
        for (String n : names) {
            System.err.println(" '"+n+"'");
        }
        return m;
        */
    }
}
