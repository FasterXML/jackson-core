package com.fasterxml.jackson.core.json;

import java.io.ByteArrayOutputStream;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;

public class TestUtf8Generator
    extends BaseTest
{
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
        JsonFactory f = new JsonFactory();
        JsonParser p = f.createParser(bytes.toByteArray());
        for (int i = 1; i <= length; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(str, p.getText());
        assertNull(p.nextToken());
        p.close();
    }

    // for [Issue#115]
    public void testSurrogatesWithRaw() throws Exception
    {
        final String VALUE = quote("\ud83d\ude0c");
        JsonFactory f = new JsonFactory();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jgen = f.createGenerator(out);
        jgen.writeStartArray();
        jgen.writeRaw(VALUE);
        jgen.writeEndArray();
        jgen.close();

        final byte[] JSON = out.toByteArray();

        JsonParser jp = f.createParser(JSON);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        String str = jp.getText();
        assertEquals(2, str.length());
        assertEquals((char) 0xD83D, str.charAt(0));
        assertEquals((char) 0xDE0C, str.charAt(1));
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }
}
