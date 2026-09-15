package com.fasterxml.jackson.core.io;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UTF32ReaderSurrogateTest extends JUnit5TestBase
{
    @Test
    void surrogateSplitSingleCharRead() throws Exception {
        byte[] input = new byte[] { 0x00, 0x01, (byte) 0xF6, 0x00 }; // U+1F600
        UTF32Reader reader = new UTF32Reader(null, null, input, 0, input.length, true);
        assertEquals(0xD83D, reader.read());
        assertEquals(0xDE00, reader.read());
        reader.close();
    }

    @Test
    void surrogateSplitSingleCharReadLowest() throws Exception {
        byte[] input = new byte[] { 0x00, 0x01, 0x00, 0x00 }; // U+10000
        UTF32Reader reader = new UTF32Reader(null, null, input, 0, input.length, true);
        assertEquals(0xD800, reader.read());
        assertEquals(0xDC00, reader.read());
        reader.close();
    }

    @Test
    void surrogateSplitAtParserBufferBoundary() throws Exception {
        char[] f = new char[3997];
        java.util.Arrays.fill(f, 'a');
        String filler = new String(f);
        String doc = "[\"" + filler + "😀\"]";
        int[] cps = doc.codePoints().toArray();
        byte[] out = new byte[cps.length * 4];
        for (int i = 0; i < cps.length; ++i) {
            int cp = cps[i];
            out[i*4] = (byte) (cp >> 24); out[i*4+1] = (byte) (cp >> 16);
            out[i*4+2] = (byte) (cp >> 8); out[i*4+3] = (byte) cp;
        }
        JsonFactory jf = new JsonFactory();
        try (JsonParser p = jf.createParser(out)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(filler + "😀", p.getText());
            assertToken(JsonToken.END_ARRAY, p.nextToken());
        }
    }
}
