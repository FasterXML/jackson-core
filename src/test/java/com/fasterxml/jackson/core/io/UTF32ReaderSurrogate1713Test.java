package com.fasterxml.jackson.core.io;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [core#1713]: low surrogate corrupted/dropped when a supplementary character
// is split across a read() boundary
class UTF32ReaderSurrogate1713Test extends JUnit5TestBase
{
    // U+10000 is the interesting one: normalized value is 0, same as the
    // "no surrogate pending" sentinel
    private final static String SUPPLEMENTARY = "😀𐀀􏿿";

    @Test
    void singleCharReadBigEndian() throws Exception {
        _testSingleCharRead(true);
    }

    @Test
    void singleCharReadLittleEndian() throws Exception {
        _testSingleCharRead(false);
    }

    private void _testSingleCharRead(boolean bigEndian) throws Exception
    {
        byte[] input = utf32(SUPPLEMENTARY, bigEndian);
        UTF32Reader r = new UTF32Reader(null, null, input, 0, input.length, bigEndian);
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = r.read()) >= 0) {
            sb.append((char) ch);
        }
        r.close();
        assertEquals(SUPPLEMENTARY, sb.toString());
    }

    @Test
    void chunkedReadSplitsPair() throws Exception
    {
        // Text of odd length so every surrogate pair straddles some chunk boundary
        String text = "a" + SUPPLEMENTARY + "b" + SUPPLEMENTARY;
        for (boolean bigEndian : new boolean[] { true, false }) {
            for (int chunkSize = 1; chunkSize <= 7; ++chunkSize) {
                byte[] input = utf32(text, bigEndian);
                UTF32Reader r = new UTF32Reader(null, null, input, 0, input.length, bigEndian);
                CharArrayWriter w = new CharArrayWriter();
                char[] buf = new char[chunkSize];
                int count;
                while ((count = r.read(buf, 0, chunkSize)) >= 0) {
                    w.write(buf, 0, count);
                }
                r.close();
                assertEquals(text, w.toString(),
                        "bigEndian="+bigEndian+", chunkSize="+chunkSize);
            }
        }
    }

    // Via parser: byte[] source has no InputStream so a single read() can
    // fill the whole 4000-char buffer and split the pair at its end
    @Test
    void viaParserAtBufferBoundary() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        for (int prefixLen = 3995; prefixLen <= 4001; ++prefixLen) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < prefixLen; ++i) {
                sb.append((char) ('a' + (i % 26)));
            }
            sb.append(SUPPLEMENTARY);
            String value = sb.toString();
            String doc = "[\""+value+"\"]";
            for (boolean bigEndian : new boolean[] { true, false }) {
                try (JsonParser p = f.createParser(utf32(doc, bigEndian))) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertEquals(value, p.getText(), "prefixLen="+prefixLen+", bigEndian="+bigEndian);
                    assertToken(JsonToken.END_ARRAY, p.nextToken());
                }
            }
        }
    }

    private static byte[] utf32(String text, boolean bigEndian) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (bigEndian) {
                bytes.write(cp >> 24);
                bytes.write(cp >> 16);
                bytes.write(cp >> 8);
                bytes.write(cp);
            } else {
                bytes.write(cp);
                bytes.write(cp >> 8);
                bytes.write(cp >> 16);
                bytes.write(cp >> 24);
            }
        }
        return bytes.toByteArray();
    }
}
