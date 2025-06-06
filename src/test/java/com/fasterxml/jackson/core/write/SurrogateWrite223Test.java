package com.fasterxml.jackson.core.write;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurrogateWrite223Test extends JUnit5TestBase
{
    private final JsonFactory DEFAULT_JSON_F = newStreamFactory();

    // for [core#223]
    @Test
    void surrogatesDefaultSetting() throws Exception {
        // default in 2.x should be disabled:
        assertFalse(DEFAULT_JSON_F.isEnabled(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8.mappedFeature()));
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
        g = f.createGenerator(out);
        g.writeStartArray();
        g.writeString(toQuote);
        g.writeEndArray();
        g.close();
        assertEquals(2 + 2 + 4, out.size()); // brackets, quotes, 4-byte encoding

        // Also parse back to ensure correctness
        JsonParser p = f.createParser(out.toByteArray());
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

        g = f.createGenerator(out);
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
        g = DEFAULT_JSON_F.createGenerator(out);
        g.writeStartArray();
        g.writeString(toQuote);
        g.writeEndArray();
        g.close();
        assertEquals(2 + 2 + 2, out.toString().length()); // brackets, quotes, 2 chars as is

        // Also parse back to ensure correctness
        JsonParser p = DEFAULT_JSON_F.createParser(out.toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(toQuote, p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    //https://github.com/FasterXML/jackson-core/issues/1359
    @Test
    void checkNonSurrogates() throws Exception {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator gen = f.createGenerator(out)) {
            gen.writeStartObject();

            // Inside the BMP, beyond surrogate block; 0xFF0C - full-width comma
            gen.writeStringField("test_full_width", "foo" + new String(Character.toChars(0xFF0C)) + "bar");

            // Inside the BMP, beyond surrogate block; 0xFE6A - small form percent
            gen.writeStringField("test_small_form", "foo" + new String(Character.toChars(0xFE6A)) + "bar");

            // Inside the BMP, before the surrogate block; 0x3042 - Hiragana A
            gen.writeStringField("test_hiragana", "foo" + new String(Character.toChars(0x3042)) + "bar");

            // Outside the BMP; 0x1F60A - emoji
            gen.writeStringField("test_emoji", new String(Character.toChars(0x1F60A)));

            gen.writeEndObject();
        }
        String json = out.toString("UTF-8");
        assertTrue(json.contains("foo\uFF0Cbar"));
        assertTrue(json.contains("foo\uFE6Abar"));
        assertTrue(json.contains("foo\u3042bar"));
        assertTrue(json.contains("\"test_emoji\":\"\uD83D\uDE0A\""));
    }

    @Test
    void checkSurrogateWithCharacterEscapes() throws Exception {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
                .build();
        f.setCharacterEscapes(JsonpCharacterEscapes.instance());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator gen = f.createGenerator(out)) {
            gen.writeStartObject();
            // Outside the BMP; 0x1F60A - emoji
            gen.writeStringField("test_emoji", new String(Character.toChars(0x1F60A)));
            gen.writeEndObject();
        }
        String json = out.toString("UTF-8");
        assertEquals("{\"test_emoji\":\"\uD83D\uDE0A\"}", json);
    }
}
