package tools.jackson.core.util;

import tools.jackson.core.io.CharTypes;

public class TestCharTypes
    extends tools.jackson.core.BaseTest
{
    public void testQuoting()
    {
        StringBuilder sb = new StringBuilder();
        CharTypes.appendQuoted(sb, "\n");
        assertEquals("\\n", sb.toString());
        sb = new StringBuilder();
        CharTypes.appendQuoted(sb, "\u0000");
        assertEquals("\\u0000", sb.toString());
    }
}
