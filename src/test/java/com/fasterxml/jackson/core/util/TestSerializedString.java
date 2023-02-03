package com.fasterxml.jackson.core.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * Simple unit tests to try to verify that the default
 * {@link SerializableString} implementation works as expected.
 */
public class TestSerializedString
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testAppending() throws IOException
    {
        final String INPUT = "\"quo\\ted\"";
        final String QUOTED = "\\\"quo\\\\ted\\\"";

        SerializableString sstr = new SerializedString(INPUT);
        // sanity checks first:
        assertEquals(sstr.getValue(), INPUT);
        assertEquals(QUOTED, new String(sstr.asQuotedChars()));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertEquals(QUOTED.length(), sstr.writeQuotedUTF8(bytes));
        assertEquals(QUOTED, bytes.toString("UTF-8"));
        bytes.reset();
        assertEquals(INPUT.length(), sstr.writeUnquotedUTF8(bytes));
        assertEquals(INPUT, bytes.toString("UTF-8"));

        byte[] buffer = new byte[100];
        assertEquals(QUOTED.length(), sstr.appendQuotedUTF8(buffer, 3));
        assertEquals(QUOTED, new String(buffer, 3, QUOTED.length()));
        Arrays.fill(buffer, (byte) 0);
        assertEquals(INPUT.length(), sstr.appendUnquotedUTF8(buffer, 5));
        assertEquals(INPUT, new String(buffer, 5, INPUT.length()));
    }

    public void testFailedAccess() throws IOException
    {
        final String INPUT = "Bit longer text";
        SerializableString sstr = new SerializedString(INPUT);

        final byte[] buffer = new byte[INPUT.length() - 2];
        final char[] ch = new char[INPUT.length() - 2];
        final ByteBuffer bbuf = ByteBuffer.allocate(INPUT.length() - 2);

        assertEquals(-1, sstr.appendQuotedUTF8(buffer, 0));
        assertEquals(-1, sstr.appendQuoted(ch, 0));
        assertEquals(-1, sstr.putQuotedUTF8(bbuf));

        bbuf.rewind();
        assertEquals(-1, sstr.appendUnquotedUTF8(buffer, 0));
        assertEquals(-1, sstr.appendUnquoted(ch, 0));
        assertEquals(-1, sstr.putUnquotedUTF8(bbuf));
    }
}
