package com.fasterxml.jackson.core.io;

public class TestCharTypes
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testAppendQuoted0_31 ()
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

    public void testHexOutOfRange()
    {
        final int[] inputs = {0, -1, 1, 129, -129};
        for (int input : inputs) {
            assertEquals(-1, CharTypes.charToHex(input));
        }
    }
}
