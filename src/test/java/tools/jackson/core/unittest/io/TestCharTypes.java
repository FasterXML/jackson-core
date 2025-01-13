package tools.jackson.core.unittest.io;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.CharTypes;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        final int[] inputs = {0, -1, 1, 129, -129};
        for (int input : inputs) {
            assertEquals(-1, CharTypes.charToHex(input));
        }
    }
}
