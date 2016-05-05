package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;

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
        JsonGenerator jgen = JSON_F.createGenerator(out);
        jgen.writeStartArray();
        jgen.writeRaw(VALUE);
        jgen.writeEndArray();
        jgen.close();

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
    
    public void testSupplementaryCharacterWithString() throws Exception {
    	// U+2070E
    	// UTF 8 format : f0 a0 9c 8e
    	// UTF 16 format : d841 df0e
    	final String VALUE = new String(Character.toChars(0x2070E));
    	
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jgen = JSON_F.createGenerator(out);
        jgen.writeStartArray();
        jgen.writeString(VALUE);
        jgen.writeEndArray();
        jgen.close();

        final byte[] JSON = out.toByteArray();
        assertEquals(8, JSON.length);
        assertEquals((byte) 0xf0, JSON[2]);
        assertEquals((byte) 0xa0, JSON[3]);
        assertEquals((byte) 0x9c, JSON[4]);
        assertEquals((byte) 0x8e, JSON[5]);

        JsonParser jp = JSON_F.createParser(JSON);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        String str = jp.getText();
        assertEquals(2, str.length());
        assertEquals((char) 0xd841, str.charAt(0));
        assertEquals((char) 0xdf0e, str.charAt(1));
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    	
    }
}
