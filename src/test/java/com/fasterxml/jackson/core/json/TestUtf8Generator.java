package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;

import java.io.ByteArrayOutputStream;

public class TestUtf8Generator extends BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testUtf8Issue462() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IOContext ioc = new IOContext(new BufferRecycler(), bytes, true);
        JsonGenerator gen = new UTF8JsonGenerator(ioc, 0, null, bytes);
        String str = "Natuurlijk is alles gelukt en weer een tevreden klant\uD83D\uDE04";
        int length = 4000 - 38;

        for (int i = 1; i <= length; ++i) {
            gen.writeNumber(1);
        }
        gen.writeString(str);
        gen.flush();
        gen.close();
        
        // Also verify it's parsable?
        JsonParser p = JSON_F.createParser(bytes.toByteArray());
        for (int i = 1; i <= length; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(str, p.getText());
        assertNull(p.nextToken());
        p.close();
    }

    // for [core#115]
    public void testSurrogatesWithRaw() throws Exception
    {
        final String VALUE = quote("\ud83d\ude0c");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator g = JSON_F.createGenerator(out);
        g.writeStartArray();
        g.writeRaw(VALUE);
        g.writeEndArray();
        g.close();

        final byte[] JSON = out.toByteArray();

        JsonParser jp = JSON_F.createParser(JSON);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        String str = jp.getText();
        assertEquals(2, str.length());
        assertEquals((char) 0xD83D, str.charAt(0));
        assertEquals((char) 0xDE0C, str.charAt(1));
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }

    public void testFilteringWithEscapedChars() throws Exception
    {
        final String SAMPLE_WITH_QUOTES = "\b\t\f\n\r\"foo\"\u0000";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @SuppressWarnings("resource")
        JsonGenerator g = JSON_F.createGenerator(out);

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(g,
                new JsonPointerBasedFilter("/escapes"),
                true, // includePath
                false // multipleMatches
        );

        //final String JSON = "{'a':123,'array':[1,2],'escapes':'\b\t\f\n\r\"foo\"\u0000'}";

        gen.writeStartObject();

        gen.writeFieldName("a");
        gen.writeNumber((int) 123);

        gen.writeFieldName("array");
        gen.writeStartArray();
        gen.writeNumber((short) 1);
        gen.writeNumber((short) 2);
        gen.writeEndArray();

        gen.writeFieldName("escapes");

        final byte[] raw = SAMPLE_WITH_QUOTES.toString().getBytes("UTF-8");
        gen.writeUTF8String(raw, 0, raw.length);

        gen.writeEndObject();
        gen.close();

        JsonParser p = JSON_F.createParser(out.toByteArray());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("escapes", p.getCurrentName());

        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(SAMPLE_WITH_QUOTES, p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }
}
