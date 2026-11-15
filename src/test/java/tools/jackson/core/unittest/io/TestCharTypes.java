package tools.jackson.core.unittest.io;

import org.junit.jupiter.api.Test;

import tools.jackson.core.SerializableString;
import tools.jackson.core.io.CharTypes;
import tools.jackson.core.io.CharacterEscapes;

import static org.junit.jupiter.api.Assertions.*;

class TestCharTypes
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    @Test
    void appendQuoted031()
    {
        final String[] inputs =    { "\u0000",  "\u001F",  "abcd", "\u0001ABCD\u0002",   "WX\u000F\u0010YZ"   };
        final String[] expecteds = { "\\u0000", "\\u001F", "abcd", "\\u0001ABCD\\u0002", "WX\\u000F\\u0010YZ" };
        assert inputs.length == expecteds.length;

        for (int i = 0; i < inputs.length; i++) {
            final String input = inputs[i];
            final String expected = expecteds[i];

            final StringBuilder sb = new StringBuilder();
            CharTypes.appendQuoted(sb, input);
            final String actual = sb.toString();

            assertEquals(expected, actual);
        }
    }

    @Test
    void hexOutOfRange()
    {
        // [core#1787]: non-Latin1 chars whose low byte is an ASCII hex digit
        final int[] inputs = {0, -1, 1, 129, -129, 0x2042, 0x141, 0x130, 0x1F600};
        for (int input : inputs) {
            assertEquals(-1, CharTypes.charToHex(input));
        }
    }

    // Custom escapes need quote char (and slash, if enabled) added; result
    // is cached on CharacterEscapes instance
    @Test
    void withRequiredEscapesCached()
    {
        final CharacterEscapes esc = new StdBasedEscapes();
        final int[] base = esc.getEscapeCodesForAscii();

        // Already escapes '"', no slash: no copy needed
        assertSame(base, CharTypes.withRequiredEscapes(esc, '"', false));

        // Slash escaping needed: copy, then reused
        int[] withSlash = CharTypes.withRequiredEscapes(esc, '"', true);
        assertNotSame(base, withSlash);
        assertEquals('/', withSlash['/']);
        assertEquals(0, base['/']);
        assertSame(withSlash, CharTypes.withRequiredEscapes(esc, '"', true));

        // Different settings: new table
        int[] apos = CharTypes.withRequiredEscapes(esc, '\'', false);
        assertNotSame(withSlash, apos);
        assertEquals('\'', apos['\'']);
        assertEquals(0, apos['/']);
        assertSame(apos, CharTypes.withRequiredEscapes(esc, '\'', false));

        // Non-"natural" quote char gets standard escape
        int[] backtick = CharTypes.withRequiredEscapes(esc, '`', true);
        assertEquals(CharacterEscapes.ESCAPE_STANDARD, backtick['`']);
        assertEquals('/', backtick['/']);
    }

    @SuppressWarnings("serial")
    static class StdBasedEscapes extends CharacterEscapes
    {
        private final int[] _asciiEscapes = standardAsciiEscapesForJSON();

        @Override
        public int[] getEscapeCodesForAscii() {
            return _asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            return null;
        }
    }
}
