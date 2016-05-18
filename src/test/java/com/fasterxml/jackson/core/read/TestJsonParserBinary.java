package com.fasterxml.jackson.core.read;

import java.io.*;

import com.fasterxml.jackson.core.*;

import static org.junit.Assert.*;

/**
 * Tests for verifying that accessing base64 encoded content works ok.
 */
public class TestJsonParserBinary
    extends com.fasterxml.jackson.core.BaseTest
{
    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    public void testSimple() throws IOException
    {
        for (int mode : ALL_MODES) {
            _testSimple(mode);
        }
    }

    public void testInArray() throws IOException
    {
        for (int mode : ALL_MODES) {
            _testInArray(mode);
        }
    }

    public void testWithEscaped() throws IOException {
        for (int mode : ALL_MODES) {
            _testEscaped(mode);
        }
    }

    /*
    /**********************************************************************
    /* Actual test methods
    /**********************************************************************
     */

    private void _testSimple(int mode)
        throws IOException
    {
        /* The usual sample input string, from Thomas Hobbes's "Leviathan"
         * (via Wikipedia)
         */
        final String RESULT = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
        final byte[] RESULT_BYTES = RESULT.getBytes("US-ASCII");

        // And here's what should produce it...
        final String INPUT_STR = 
 "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz"
+"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg"
+"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu"
+"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo"
+"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
            ;

        final String DOC = "\""+INPUT_STR+"\"";
        JsonParser p = createParser(mode, DOC);

        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        byte[] data = p.getBinaryValue();
        assertNotNull(data);
        assertArrayEquals(RESULT_BYTES, data);
        p.close();
    }

    private void _testInArray(int mode) throws IOException
    {
        JsonFactory f = new JsonFactory();

        final int entryCount = 7;

        StringWriter sw = new StringWriter();
        JsonGenerator jg = f.createGenerator(sw);
        jg.writeStartArray();

        byte[][] entries = new byte[entryCount][];
        for (int i = 0; i < entryCount; ++i) {
            byte[] b = new byte[200 + i * 100];
            for (int x = 0; x < b.length; ++x) {
                b[x] = (byte) (i + x);
            }
            entries[i] = b;
            jg.writeBinary(b);
        }

        jg.writeEndArray();
        jg.close();

        JsonParser p = createParser(f, mode, sw.toString());

        assertToken(JsonToken.START_ARRAY, p.nextToken());

        for (int i = 0; i < entryCount; ++i) {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            byte[] b = p.getBinaryValue();
            assertArrayEquals(entries[i], b);
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void _testEscaped(int mode) throws IOException
    {
        // Input: "Test!" -> "VGVzdCE="

        // First, try with embedded linefeed half-way through:

        String DOC = quote("VGVz\\ndCE="); // note: must double-quote to get linefeed
        JsonParser p = createParser(mode, DOC);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        byte[] b = p.getBinaryValue();
        assertEquals("Test!", new String(b, "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();

        // and then with escaped chars
//        DOC = quote("V\\u0047V\\u007AdCE="); // note: must escape backslash...
        DOC = quote("V\\u0047V\\u007AdCE="); // note: must escape backslash...
        p = createParser(mode, DOC);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        b = p.getBinaryValue();
        assertEquals("Test!", new String(b, "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();
    }
}
