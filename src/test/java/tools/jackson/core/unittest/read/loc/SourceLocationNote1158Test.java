package tools.jackson.core.unittest.read.loc;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

class SourceLocationNote1158Test extends AsyncTestBase
{
    private final JsonFactory FACTORY = JsonFactory.builder()
            .disable(StreamReadFeature.INCLUDE_REDACTION_NOTE_IN_LOCATION)
            .build();

    @Test
    void omitsRedactionNoteAcrossInputModes() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(FACTORY, mode, "[ ! ]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                _verifySuppressed(p.currentTokenLocation());
                _verifySuppressed(p.currentLocation());
                StreamReadException e = assertThrows(StreamReadException.class, p::nextToken);
                _verifySuppressed(e.getLocation());
                assertFalse(e.getMessage().contains("Source:"));
                assertTrue(e.getMessage().contains(e.getLocation().offsetDescription()));
                // [core#1833]: textual content retains line/column
                assertTrue(e.getLocation().contentReference().hasTextualContent());
                assertTrue(e.getLocation().offsetDescription().startsWith("line: 1, column: "),
                        e.getLocation().offsetDescription());
            }
        }
    }

    @Test
    void omitsRedactionNoteForAsyncParsers() throws Exception
    {
        for (int bytesPerRead : new int[] { 1, 3 }) {
            try (AsyncReaderWrapper p = asyncForBytes(FACTORY, bytesPerRead,
                    utf8Bytes("[ ! ]"), 0)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                _verifySuppressed(assertThrows(StreamReadException.class, p::nextToken).getLocation());
            }
            try (AsyncReaderWrapper p = asyncForByteBuffer(FACTORY, bytesPerRead,
                    utf8Bytes("[ ! ]"), 0)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                _verifySuppressed(assertThrows(StreamReadException.class, p::nextToken).getLocation());
            }
        }
    }

    @Test
    void configurationIsLocalToFactory() throws Exception
    {
        JsonFactory defaults = JsonFactory.builder().build();
        JsonFactory restored = FACTORY.rebuild()
                .enable(StreamReadFeature.INCLUDE_REDACTION_NOTE_IN_LOCATION).build();
        for (JsonFactory f : new JsonFactory[] { defaults, FACTORY, restored, defaults }) {
            try (JsonParser p = createParser(f, MODE_READER, "[1]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                if (f == FACTORY) {
                    _verifySuppressed(p.currentLocation());
                } else {
                    assertTrue(p.currentLocation().toString().contains("Source: REDACTED"));
                    assertNull(p.currentLocation().contentReference().getRawContent());
                }
            }
        }
    }

    @Test
    void explicitlyIncludedSourceStillShown() throws Exception
    {
        JsonFactory f = FACTORY.rebuild()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION).build();
        String doc = "[ ! ]";
        try (JsonParser p = f.createParser(ObjectReadContext.empty(), doc)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            StreamReadException e = assertThrows(StreamReadException.class, p::nextToken);
            assertSame(doc, e.getLocation().contentReference().getRawContent());
            assertTrue(e.getLocation().toString().contains("Source: (String)"));
            assertFalse(e.getLocation().toString().contains("REDACTED"));
        }
    }

    @Test
    void preservesOffsetsAndStandaloneDescriptions() throws Exception
    {
        JsonFactory defaults = JsonFactory.builder().build();
        for (int mode : ALL_MODES) {
            try (JsonParser a = createParser(defaults, mode, "\n[1]");
                    JsonParser b = createParser(FACTORY, mode, "\n[1]")) {
                assertToken(JsonToken.START_ARRAY, a.nextToken());
                assertToken(JsonToken.START_ARRAY, b.nextToken());
                TokenStreamLocation expected = a.currentLocation();
                TokenStreamLocation actual = b.currentLocation();
                assertEquals(expected.getLineNr(), actual.getLineNr());
                assertEquals(expected.getColumnNr(), actual.getColumnNr());
                assertEquals(expected.getByteOffset(), actual.getByteOffset());
                assertEquals(expected.getCharOffset(), actual.getCharOffset());
                assertEquals(expected.offsetDescription(), actual.offsetDescription());
            }
        }
        assertTrue(ContentReference.redacted().buildSourceDescription().startsWith("REDACTED"));
        assertSame(ContentReference.redactedWithoutNote(), ContentReference.redactedWithoutNote(false));
        assertFalse(ContentReference.redactedWithoutNote(false).hasTextualContent());
        assertTrue(ContentReference.redactedWithoutNote(true).hasTextualContent());
        assertEquals("", ContentReference.redactedWithoutNote(true).buildSourceDescription());
        assertEquals("UNKNOWN", ContentReference.unknown().buildSourceDescription());
        assertEquals("[No location information]", TokenStreamLocation.NA.toString());
    }

    private void _verifySuppressed(TokenStreamLocation location)
    {
        assertNull(location.contentReference().getRawContent());
        assertEquals("", location.sourceDescription());
        assertEquals("[" + location.offsetDescription() + "]", location.toString());
        assertEquals("prefix " + location.toString(),
                location.toString(new StringBuilder("prefix ")).toString());
    }
}
