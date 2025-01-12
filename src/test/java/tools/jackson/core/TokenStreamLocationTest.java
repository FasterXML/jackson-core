package tools.jackson.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.json.JsonFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying internal working of {@link TokenStreamLocation} class itself,
 * as opposed to accuracy of reported location information by parsers.
 */
class TokenStreamLocationTest
    extends JacksonCoreTestBase
{
    static class Foobar { }

    @Test
    void basics()
    {
        TokenStreamLocation loc1 = new TokenStreamLocation(_sourceRef("src"),
                10L, 10L, 1, 2);
        TokenStreamLocation loc2 = new TokenStreamLocation(null, 10L, 10L, 3, 2);
        assertEquals(loc1, loc1);
        assertNotEquals(null, loc1);
        assertNotEquals(loc1, loc2);
        assertNotEquals(loc2, loc1);

        // don't care about what it is; should not compute to 0 with data above
        assertTrue(loc1.hashCode() != 0);
        assertTrue(loc2.hashCode() != 0);
    }

    @Test
    void basicToString() throws Exception
    {
        // no location; presumed to be Binary due to defaulting
        assertEquals("[Source: UNKNOWN; byte offset: #10]",
                new TokenStreamLocation(null, 10L, 10L, 3, 2).toString());

        // Short String
        assertEquals("[Source: (String)\"string-source\"; line: 1, column: 2]",
                new TokenStreamLocation(_sourceRef("string-source"), 10L, 10L, 1, 2).toString());

        // Short char[]
        assertEquals("[Source: (char[])\"chars-source\"; line: 1, column: 2]",
                new TokenStreamLocation(_sourceRef("chars-source".toCharArray()), 10L, 10L, 1, 2).toString());

        // Short byte[]
        assertEquals("[Source: (byte[])\"bytes-source\"; line: 1, column: 2]",
                new TokenStreamLocation(_sourceRef(utf8Bytes("bytes-source")), 10L, 10L, 1, 2).toString());

        // InputStream
        assertEquals("[Source: (ByteArrayInputStream); line: 1, column: 2]",
                new TokenStreamLocation(_sourceRef(new ByteArrayInputStream(new byte[0])),
                        10L, 10L, 1, 2).toString());

        // Class<?> that specifies source type
        assertEquals("[Source: (InputStream); line: 1, column: 2]",
                new TokenStreamLocation(_rawSourceRef(true, InputStream.class), 10L, 10L, 1, 2).toString());

        // misc other
        Foobar srcRef = new Foobar();
        assertEquals("[Source: ("+srcRef.getClass().getName()+"); line: 1, column: 2]",
                new TokenStreamLocation(_rawSourceRef(true, srcRef), 10L, 10L, 1, 2).toString());
    }

    @Test
    void truncatedSource() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH; ++i) {
            sb.append("x");
        }
        String main = sb.toString();
        String json = main + "yyy";
        TokenStreamLocation loc = new TokenStreamLocation(_sourceRef(json), 0L, 0L, 1, 1);
        String desc = loc.sourceDescription();
        assertEquals(String.format("(String)\"%s\"[truncated 3 chars]", main), desc);

        // and same with bytes
        loc = new TokenStreamLocation(_sourceRef(utf8Bytes(json)), 0L, 0L, 1, 1);
        desc = loc.sourceDescription();
        assertEquals(String.format("(byte[])\"%s\"[truncated 3 bytes]", main), desc);
    }

    // for [jackson-core#658]
    @Test
    void escapeNonPrintable() throws Exception
    {
        final String DOC = "[ \"tab:[\t]/null:[\0]\" ]";
        TokenStreamLocation loc = new TokenStreamLocation(_sourceRef(DOC), 0L, 0L, -1, -1);
        final String sourceDesc = loc.sourceDescription();
        assertEquals(String.format("(String)\"[ \"tab:[%s]/null:[%s]\" ]\"",
                "\\u0009", "\\u0000"), sourceDesc);
    }

    // for [jackson-core#356]
    @Test
    void disableSourceInclusion() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();

        try (JsonParser p = f.createParser(ObjectReadContext.empty(), "[ foobar ]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                p.nextToken();
                fail("Shouldn't have passed");
            } catch (StreamReadException e) {
                _verifyContentDisabled(e);
            }
        }

        // and verify same works for byte-based too
        try (JsonParser p = f.createParser(ObjectReadContext.empty(), utf8Bytes("[ foobar ]"))) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                p.nextToken();
                fail("Shouldn't have passed");
            } catch (StreamReadException e) {
                _verifyContentDisabled(e);
            }
        }
    }

    private void _verifyContentDisabled(StreamReadException e) {
        verifyException(e, "unrecognized token");
        TokenStreamLocation loc = e.getLocation();
        assertNull(loc.contentReference().getRawContent());
        assertThat(loc.sourceDescription()).startsWith("REDACTED");
    }

    // for [jackson-core#739]: try to support equality
    @Test
    void locationEquality() throws Exception
    {
        // important: create separate but equal instances
        File src1 = new File("/tmp/foo");
        File src2 = new File("/tmp/foo");
        assertEquals(src1, src2);

        TokenStreamLocation loc1 = new TokenStreamLocation(_sourceRef(src1),
                10L, 10L, 1, 2);
        TokenStreamLocation loc2 = new TokenStreamLocation(_sourceRef(src2),
                10L, 10L, 1, 2);
        assertEquals(loc1, loc2);

        // Also make sure to consider offset/length
        final byte[] bogus = "BOGUS".getBytes();

        // If same, equals:
        assertEquals(new TokenStreamLocation(_sourceRef(bogus, 0, 5), 5L, 0L, 1, 2),
                new TokenStreamLocation(_sourceRef(bogus, 0, 5), 5L, 0L, 1, 2));

        // If different, not equals
        loc1 = new TokenStreamLocation(_sourceRef(bogus, 0, 5),
                5L, 0L, 1, 2);
        loc2 = new TokenStreamLocation(_sourceRef(bogus, 1, 4),
                5L, 0L, 1, 2);
        assertNotEquals(loc1, loc2);
        assertNotEquals(loc2, loc1);
    }

    private ContentReference _sourceRef(String rawSrc) {
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length(),
                ErrorReportConfiguration.defaults());
    }

    private ContentReference _sourceRef(char[] rawSrc) {
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length,
                ErrorReportConfiguration.defaults());
    }

    private ContentReference _sourceRef(byte[] rawSrc) {
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length,
                ErrorReportConfiguration.defaults());
    }

    private ContentReference _sourceRef(byte[] rawSrc, int offset, int length) {
        return ContentReference.construct(true, rawSrc, offset, length,
                ErrorReportConfiguration.defaults());
    }

    private ContentReference _sourceRef(InputStream rawSrc) {
        return ContentReference.construct(true, rawSrc, -1, -1,
                ErrorReportConfiguration.defaults());
    }

    private ContentReference _sourceRef(File rawSrc) {
        return ContentReference.construct(true, rawSrc, -1, -1,
                ErrorReportConfiguration.defaults());
    }

    private ContentReference _rawSourceRef(boolean textual, Object rawSrc) {
        return ContentReference.rawReference(textual, rawSrc);
    }
}
