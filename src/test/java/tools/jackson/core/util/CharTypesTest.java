package tools.jackson.core.util;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.CharTypes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharTypesTest
    extends tools.jackson.core.JUnit5TestBase
{
    @Test
    void quoting()
    {
        StringBuilder sb = new StringBuilder();
        CharTypes.appendQuoted(sb, "\n");
        assertEquals("\\n", sb.toString());
        sb = new StringBuilder();
        CharTypes.appendQuoted(sb, "\u0000");
        assertEquals("\\u0000", sb.toString());
    }
}
