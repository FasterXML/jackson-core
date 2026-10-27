package tools.jackson.core.unittest.sym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import tools.jackson.core.sym.BinaryNameMatcher;
import tools.jackson.core.util.Named;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

// [core#1798]: names with leading NULs in last partial quad must not
// match shorter names, when using padded quads
public class BinaryNameMatcherPadding1798Test extends JacksonCoreTestBase
{
    private final List<String> NAMES = Arrays.asList("x", "abcde", "abcdefghi",
            "abcdefghijklm", "abcd", "abcdefgh");

    @Test
    public void testPaddedMatching()
    {
        BinaryNameMatcher matcher = BinaryNameMatcher.construct(NAMES, true);
        for (int i = 0; i < NAMES.size(); ++i) {
            assertEquals(i, _match(matcher, NAMES.get(i), true));
        }
    }

    @Test
    public void testPaddedNoMatchForNulPrefixed()
    {
        BinaryNameMatcher matcher = BinaryNameMatcher.construct(NAMES, true);
        assertEquals(-1, _match(matcher, "\0x", true));
        assertEquals(-1, _match(matcher, "\0\0x", true));
        assertEquals(-1, _match(matcher, "abcd\0e", true));
        assertEquals(-1, _match(matcher, "abcdefgh\0\0i", true));
        assertEquals(-1, _match(matcher, "abcdefghijkl\0m", true));
        // full last quad: not padded, but differs in length (in quads)
        assertEquals(-1, _match(matcher, "\0\0\0x", true));
        assertEquals(-1, _match(matcher, "abcd\0\0\0e", true));
    }

    @Test
    public void testPaddedConstructFrom()
    {
        _verifyPadded(BinaryNameMatcher.constructFrom(_named(), false, true));
    }

    @Test
    public void testPaddedCaseInsensitive()
    {
        _verifyPadded(BinaryNameMatcher.constructCaseInsensitive(Locale.ROOT,
                _named(), false, true));
    }

    // Enough names to also use secondary/tertiary/spillover areas
    @Test
    public void testPaddedLarge()
    {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 1000; ++i) {
            names.add(("n"+i).intern());
        }
        BinaryNameMatcher matcher = BinaryNameMatcher.construct(names, true);
        for (int i = 0; i < names.size(); ++i) {
            assertEquals(i, _match(matcher, names.get(i), true));
            assertEquals(-1, _match(matcher, "\0"+names.get(i), true));
        }
    }

    private List<Named> _named() {
        List<Named> result = new ArrayList<>();
        for (String name : NAMES) {
            result.add(Named.fromString(name));
        }
        return result;
    }

    private void _verifyPadded(BinaryNameMatcher matcher)
    {
        for (int i = 0; i < NAMES.size(); ++i) {
            assertEquals(i, _match(matcher, NAMES.get(i), true));
        }
        assertEquals(-1, _match(matcher, "\0x", true));
        assertEquals(-1, _match(matcher, "abcd\0e", true));
    }

    // Default (unpadded) mode still matches unpadded quads (but cannot
    // distinguish NUL-prefixed variants)
    @Test
    public void testUnpaddedDefault()
    {
        BinaryNameMatcher matcher = BinaryNameMatcher.construct(NAMES);
        for (int i = 0; i < NAMES.size(); ++i) {
            assertEquals(i, _match(matcher, NAMES.get(i), false));
        }
        assertEquals(0, _match(matcher, "\0x", false));
    }

    private int _match(BinaryNameMatcher matcher, String name, boolean pad)
    {
        int[] quads = BinaryNameMatcher._quads(name, pad);
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
