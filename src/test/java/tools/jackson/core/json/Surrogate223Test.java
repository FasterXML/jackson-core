package tools.jackson.core.json;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Surrogate223Test extends JUnit5TestBase
{
    private final JsonFactory DEFAULT_JSON_F = newStreamFactory();

    // for [core#223]
    @Test
    void surrogatesDefaultSetting() throws Exception {
        // default in 3.x should be disabled:
        assertTrue(DEFAULT_JSON_F.isEnabled(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8));
    }

    // for [core#223]
    @Test
    void surrogatesByteBacked() throws Exception
    {
        ByteArrayOutputStream out;
        JsonGenerator g;
        final String toQuote = new String(Character.toChars(0x1F602));
        assertEquals(2, toQuote.length()); // just sanity check

        out = new ByteArrayOutputStream();

        JsonFactory f = JsonFactory.builder()
                .enable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
                .build();
        g = f.createGenerator(ObjectWriteContext.empty(), out);
        g.writeStartArray();
        g.writeString(toQuote);
        g.writeEndArray();
        g.close();
        assertEquals(2 + 2 + 4, out.size()); // brackets, quotes, 4-byte encoding

        // Also parse back to ensure correctness
        JsonParser p = f.createParser(ObjectReadContext.empty(), out.toByteArray());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(toQuote, p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // but may revert back to original behavior
        out = new ByteArrayOutputStream();
        f = JsonFactory.builder()
                .disable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
                .build();

        g = f.createGenerator(ObjectWriteContext.empty(), out);
        g.writeStartArray();
        g.writeString(toQuote);
        g.writeEndArray();
        g.close();
        assertEquals(2 + 2 + 12, out.size()); // brackets, quotes, 2 x 6 byte JSON escape
    }

    // for [core#223]: no change for character-backed (cannot do anything)
    @Test
    void surrogatesCharBacked() throws Exception
    {
        Writer out;
        JsonGenerator g;
        final String toQuote = new String(Character.toChars(0x1F602));
        assertEquals(2, toQuote.length()); // just sanity check

        out = new StringWriter();
        g = DEFAULT_JSON_F.createGenerator(ObjectWriteContext.empty(), out);
        g.writeStartArray();
        g.writeString(toQuote);
        g.writeEndArray();
        g.close();
        assertEquals(2 + 2 + 2, out.toString().length()); // brackets, quotes, 2 chars as is

        // Also parse back to ensure correctness
        JsonParser p = DEFAULT_JSON_F.createParser(ObjectReadContext.empty(), out.toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(toQuote, p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }
}
