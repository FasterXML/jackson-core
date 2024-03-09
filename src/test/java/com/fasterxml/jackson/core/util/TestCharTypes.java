package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.TestBase;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.io.CharTypes;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCharTypes
    extends TestBase
{
    @Test
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
