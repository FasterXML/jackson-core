package com.fasterxml.jackson.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import com.fasterxml.jackson.core.io.ContentReference;

public class JsonLocationTest extends BaseTest
{
    static class Foobar { }

    public void testBasics()
    {
        JsonLocation loc1 = new JsonLocation(_sourceRef("src"),
                10L, 10L, 1, 2);
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
        // no location; presumed to be Binary due to defaulting
        assertEquals("[Source: UNKNOWN; line: 3, column: 2]",
                new JsonLocation(null, 10L, 10L, 3, 2).toString());

        // Short String
        assertEquals("[Source: (String)\"string-source\"; line: 1, column: 2]",
                new JsonLocation(_sourceRef("string-source"), 10L, 10L, 1, 2).toString());

        // Short char[]
        assertEquals("[Source: (char[])\"chars-source\"; line: 1, column: 2]",
                new JsonLocation(_sourceRef("chars-source".toCharArray()), 10L, 10L, 1, 2).toString());

        // Short byte[]
        assertEquals("[Source: (byte[])\"bytes-source\"; line: 1, column: 2]",
                new JsonLocation(_sourceRef("bytes-source".getBytes("UTF-8")),
                        10L, 10L, 1, 2).toString());

        // InputStream
        assertEquals("[Source: (ByteArrayInputStream); line: 1, column: 2]",
                new JsonLocation(_sourceRef(new ByteArrayInputStream(new byte[0])),
                        10L, 10L, 1, 2).toString());

        // Class<?> that specifies source type
        assertEquals("[Source: (InputStream); line: 1, column: 2]",
                new JsonLocation(_rawSourceRef(true, InputStream.class), 10L, 10L, 1, 2).toString());

        // misc other
        Foobar srcRef = new Foobar();
        assertEquals("[Source: ("+srcRef.getClass().getName()+"); line: 1, column: 2]",
                new JsonLocation(_rawSourceRef(true, srcRef), 10L, 10L, 1, 2).toString());
    }

    public void testTruncatedSource() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ContentReference.DEFAULT_MAX_CONTENT_SNIPPET; ++i) {
            sb.append("x");
        }
        String main = sb.toString();
        String json = main + "yyy";
        JsonLocation loc = new JsonLocation(_sourceRef(json), 0L, 0L, 1, 1);
        String desc = loc.sourceDescription();
        assertEquals(String.format("(String)\"%s\"[truncated 3 chars]", main), desc);

        // and same with bytes
        loc = new JsonLocation(_sourceRef(json.getBytes("UTF-8")), 0L, 0L, 1, 1);
        desc = loc.sourceDescription();
        assertEquals(String.format("(byte[])\"%s\"[truncated 3 bytes]", main), desc);
    }

    // for [jackson-core#658]
    public void testEscapeNonPrintable() throws Exception
    {
        final String DOC = "[ \"tab:[\t]/null:[\0]\" ]";
        JsonLocation loc = new JsonLocation(_sourceRef(DOC), 0L, 0L, -1, -1);
        final String sourceDesc = loc.sourceDescription();
        assertEquals(String.format("(String)\"[ \"tab:[%s]/null:[%s]\" ]\"",
                "\\u0009", "\\u0000"), sourceDesc);
    }

    // for [jackson-core#356]
    public void testDisableSourceInclusion() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();

        JsonParser p = f.createParser("[ foobar ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Shouldn't have passed");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized token");
            JsonLocation loc = e.getLocation();
            assertNull(loc.contentReference().getRawContent());
            assertEquals("UNKNOWN", loc.sourceDescription());
        }
        p.close();

        // and verify same works for byte-based too
        p = f.createParser("[ foobar ]".getBytes("UTF-8"));
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Shouldn't have passed");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized token");
            JsonLocation loc = e.getLocation();
            assertNull(loc.contentReference().getRawContent());
            assertEquals("UNKNOWN", loc.sourceDescription());
        }
        p.close();
    }

    // for [jackson-core#739]: try to support equality
    public void testLocationEquality() throws Exception
    {
        // important: create separate but equal instances
        File src1 = new File("/tmp/foo");
        File src2 = new File("/tmp/foo");
        assertEquals(src1, src2);

        JsonLocation loc1 = new JsonLocation(_sourceRef(src1),
                10L, 10L, 1, 2);
        JsonLocation loc2 = new JsonLocation(_sourceRef(src2),
                10L, 10L, 1, 2);
        assertEquals(loc1, loc2);

        // Also make sure to consider offset/length
        final byte[] bogus = "BOGUS".getBytes();

        // If same, equals:
        assertEquals(new JsonLocation(_sourceRef(bogus, 0, 5), 5L, 0L, 1, 2),
                new JsonLocation(_sourceRef(bogus, 0, 5), 5L, 0L, 1, 2));

        // If different, not equals
        loc1 = new JsonLocation(_sourceRef(bogus, 0, 5),
                5L, 0L, 1, 2);
        loc2 = new JsonLocation(_sourceRef(bogus, 1, 4),
                5L, 0L, 1, 2);
        assertFalse(loc1.equals(loc2));
        assertFalse(loc2.equals(loc1));
    }

    private ContentReference _sourceRef(String rawSrc) {
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length());
    }

    private ContentReference _sourceRef(char[] rawSrc) {
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length);
    }

    private ContentReference _sourceRef(byte[] rawSrc) {
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length);
    }

    private ContentReference _sourceRef(byte[] rawSrc, int offset, int length) {
        return ContentReference.construct(true, rawSrc, offset, length);
    }

    private ContentReference _sourceRef(InputStream rawSrc) {
        return ContentReference.construct(true, rawSrc, -1, -1);
    }

    private ContentReference _sourceRef(File rawSrc) {
        return ContentReference.construct(true, rawSrc, -1, -1);
    }

    private ContentReference _rawSourceRef(boolean textual, Object rawSrc) {
        return ContentReference.rawReference(textual, rawSrc);
    }
}