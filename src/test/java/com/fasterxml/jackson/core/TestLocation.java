package com.fasterxml.jackson.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.fasterxml.jackson.core.json.JsonFactory;

public class TestLocation extends BaseTest
{
    static class Foobar { }

    public void testBasics()
    {
        JsonLocation loc1 = new JsonLocation("src", 10L, 10L, 1, 2);
        JsonLocation loc2 = new JsonLocation(null, 10L, 10L, 3, 2);
        assertEquals(loc1, loc1);
        assertFalse(loc1.equals(null));
        assertFalse(loc1.equals(loc2));
        assertFalse(loc2.equals(loc1));

        // don't care about what it is; should not compute to 0 with data above
        assertTrue(loc1.hashCode() != 0);
        assertTrue(loc2.hashCode() != 0);
    }

    public void testBasicToString() throws Exception
    {
        // no location:
        assertEquals("[Source: UNKNOWN; line: 3, column: 2]",
                new JsonLocation(null, 10L, 10L, 3, 2).toString());

        // Short String
        assertEquals("[Source: (String)\"string-source\"; line: 1, column: 2]",
                new JsonLocation("string-source", 10L, 10L, 1, 2).toString());

        // Short char[]
        assertEquals("[Source: (char[])\"chars-source\"; line: 1, column: 2]",
                new JsonLocation("chars-source".toCharArray(), 10L, 10L, 1, 2).toString());

        // Short byte[]
        assertEquals("[Source: (byte[])\"bytes-source\"; line: 1, column: 2]",
                new JsonLocation("bytes-source".getBytes("UTF-8"), 10L, 10L, 1, 2).toString());

        // InputStream
        assertEquals("[Source: (ByteArrayInputStream); line: 1, column: 2]",
                new JsonLocation(new ByteArrayInputStream(new byte[0]), 10L, 10L, 1, 2).toString());

        // Class<?> that specifies source type
        assertEquals("[Source: (InputStream); line: 1, column: 2]",
                new JsonLocation(InputStream.class, 10L, 10L, 1, 2).toString());

        // misc other
        Foobar srcRef = new Foobar();
        assertEquals("[Source: ("+srcRef.getClass().getName()+"); line: 1, column: 2]",
                new JsonLocation(srcRef, 10L, 10L, 1, 2).toString());
    }

    public void testTruncatedSource() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < JsonLocation.MAX_CONTENT_SNIPPET; ++i) {
            sb.append("x");
        }
        String main = sb.toString();
        String json = main + "yyy";
        JsonLocation loc = new JsonLocation(json, 0L, 0L, 1, 1);
        String desc = loc.sourceDescription();
        assertEquals(String.format("(String)\"%s\"[truncated 3 chars]", main), desc);

        // and same with bytes
        loc = new JsonLocation(json.getBytes("UTF-8"), 0L, 0L, 1, 1);
        desc = loc.sourceDescription();
        assertEquals(String.format("(byte[])\"%s\"[truncated 3 bytes]", main), desc);
    }

    // for [jackson-core#356]
    public void testDisableSourceInclusion() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();
        JsonParser p = f.createParser(ObjectReadContext.empty(), "[ foobar ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Shouldn't have passed");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized token");
            JsonLocation loc = e.getLocation();
            assertNull(loc.getSourceRef());
            assertEquals("UNKNOWN", loc.sourceDescription());
        }
        p.close();

        // and verify same works for byte-based too
        p = f.createParser(ObjectReadContext.empty(), "[ foobar ]".getBytes("UTF-8"));
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Shouldn't have passed");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized token");
            JsonLocation loc = e.getLocation();
            assertNull(loc.getSourceRef());
            assertEquals("UNKNOWN", loc.sourceDescription());
        }
        p.close();
    }
}
