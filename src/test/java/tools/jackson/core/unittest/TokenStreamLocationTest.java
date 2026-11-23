package tools.jackson.core.unittest;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.testutil.MockDataInput;

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
        assertTrue(loc.contentReference().hasTextualContent());
        assertThat(loc.sourceDescription()).startsWith("REDACTED");
        assertEquals("line: 1, column: 3", loc.offsetDescription());
    }

    // for [jackson-core#1833]
    @Test
    void redactedLocationPreservesTextualLineAndColumn() throws Exception
    {
        final String DOC = "{\n  \"a\": 1,\n  \"b\": x\n}";
        final JsonFactory fDisabled = JsonFactory.builder()
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();
        final JsonFactory fEnabled = JsonFactory.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();

        // Textual inputs (String, char[], Reader, byte[], InputStream) should report line & column
        // regardless of whether INCLUDE_SOURCE_IN_LOCATION is enabled or disabled
        for (JsonFactory f : new JsonFactory[] { fDisabled, fEnabled }) {
            boolean incl = f.isEnabled(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);
            _verifyTextualErrorLocation(f.createParser(ObjectReadContext.empty(), DOC),
                    incl, "line: 3, column: 8");
            _verifyTextualErrorLocation(f.createParser(ObjectReadContext.empty(), DOC.toCharArray()),
                    incl, "line: 3, column: 8");
            _verifyTextualErrorLocation(f.createParser(ObjectReadContext.empty(), new StringReader(DOC)),
                    incl, "line: 3, column: 8");
            _verifyTextualErrorLocation(f.createParser(ObjectReadContext.empty(), utf8Bytes(DOC)),
                    incl, "line: 3, column: 8");
            _verifyTextualErrorLocation(f.createParser(ObjectReadContext.empty(), new ByteArrayInputStream(utf8Bytes(DOC))),
                    incl, "line: 3, column: 8");
            _verifyTextualErrorLocation(f.createParser(ObjectReadContext.empty(), (DataInput) new MockDataInput(DOC)),
                    incl, "line: 3, column: UNKNOWN");

            // Non-blocking parser too
            JsonParser asyncP = f.createNonBlockingByteArrayParser(ObjectReadContext.empty());
            ByteArrayFeeder feeder = (ByteArrayFeeder) asyncP.nonBlockingInputFeeder();
            byte[] bytes = utf8Bytes(DOC);
            feeder.feedInput(bytes, 0, bytes.length);
            feeder.endOfInput();
            _verifyTextualErrorLocation(asyncP, incl, "line: 3, column: 8");
        }

        // Also check direct ContentReference.redacted(boolean)
        ContentReference binaryRedacted = ContentReference.redacted(false);
        assertSame(ContentReference.redacted(), binaryRedacted);
        assertFalse(binaryRedacted.hasTextualContent());
        assertThat(binaryRedacted.buildSourceDescription()).startsWith("REDACTED");
        assertEquals("[Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); byte offset: #19]",
                new TokenStreamLocation(binaryRedacted, 19L, -1L, -1, -1).toString());

        ContentReference textualRedacted = ContentReference.redacted(true);
        assertTrue(textualRedacted.hasTextualContent());
        assertThat(textualRedacted.buildSourceDescription()).startsWith("REDACTED");
        assertEquals("[Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 3, column: 8]",
                new TokenStreamLocation(textualRedacted, 19L, 19L, 3, 8).toString());
    }

    private void _verifyTextualErrorLocation(JsonParser p, boolean includeSource, String expectedOffsetDesc)
    {
        try (p) {
            while (p.nextToken() != null) { }
            fail("Should not pass");
        } catch (StreamReadException e) {
            TokenStreamLocation loc = e.getLocation();
            assertTrue(loc.contentReference().hasTextualContent());
            assertEquals(3, loc.getLineNr());
            assertEquals(expectedOffsetDesc, loc.offsetDescription());
            if (includeSource) {
                assertThat(loc.sourceDescription()).doesNotStartWith("REDACTED");
            } else {
                assertNull(loc.contentReference().getRawContent());
                assertThat(loc.sourceDescription()).startsWith("REDACTED");
                assertThat(e.getMessage()).contains("REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); "
                        + expectedOffsetDesc);
            }
        }
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
