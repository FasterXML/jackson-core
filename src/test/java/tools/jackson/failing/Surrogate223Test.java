package tools.jackson.failing;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.Writer;

import tools.jackson.core.BaseTest;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

public class Surrogate223Test extends BaseTest
{
    private final JsonFactory JSON_F = newStreamFactory();

    // for [core#223]
    public void testSurrogatesByteBacked()
    {
        ByteArrayOutputStream out;
        JsonGenerator g;
        final String toQuote = new String(Character.toChars(0x1F602));
        assertEquals(2, toQuote.length()); // just sanity check

        // default should be disabled:
//        assertFalse(JSON_F.isEnabled(JsonGenerator.Feature.ESCAPE_UTF8_SURROGATES));

        out = new ByteArrayOutputStream();
        g = JSON_F.createGenerator(ObjectWriteContext.empty(), out);
        g.writeStartArray();
        g.writeString(toQuote);
        g.writeEndArray();
        g.close();
        assertEquals(2 + 2 + 4, out.size()); // brackets, quotes, 4-byte encoding

        // Also parse back to ensure correctness
        JsonParser p = JSON_F.createParser(ObjectReadContext.empty(), out.toByteArray());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // but may revert back to original behavior
        out = new ByteArrayOutputStream();
        g = JSON_F.createGenerator(ObjectWriteContext.empty(), out);
//        g.enable(JsonGenerator.Feature.ESCAPE_UTF8_SURROGATES);
        g.writeStartArray();
        g.writeString(toQuote);
        g.writeEndArray();
        g.close();
        assertEquals(2 + 2 + 12, out.size()); // brackets, quotes, 2 x 6 byte JSON escape
    }

    // for [core#223]
    public void testSurrogatesCharBacked()
    {
        Writer out;
        JsonGenerator g;
        final String toQuote = new String(Character.toChars(0x1F602));
        assertEquals(2, toQuote.length()); // just sanity check

        // default should be disabled:
//        assertFalse(JSON_F.isEnabled(JsonGenerator.Feature.ESCAPE_UTF8_SURROGATES));

        out = new StringWriter();
        g = JSON_F.createGenerator(ObjectWriteContext.empty(), out);
        g.writeStartArray();
        g.writeString(toQuote);
        g.writeEndArray();
        g.close();
        assertEquals(2 + 2 + 2, out.toString().length()); // brackets, quotes, 2 chars as is

        // Also parse back to ensure correctness
        JsonParser p = JSON_F.createParser(ObjectReadContext.empty(), out.toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // but may revert back to original behavior
        out = new StringWriter();
        g = JSON_F.createGenerator(ObjectWriteContext.empty(), out);
//        g.enable(JsonGenerator.Feature.ESCAPE_UTF8_SURROGATES);
        g.writeStartArray();
        g.writeString(toQuote);
        g.writeEndArray();
        g.close();
        assertEquals(2 + 2 + 12, out.toString().length()); // brackets, quotes, 2 x 6 byte JSON escape
    }
}
