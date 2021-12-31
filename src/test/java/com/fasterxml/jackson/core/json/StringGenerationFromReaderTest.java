package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

import java.io.*;
import java.util.Random;

/**
 * Set of basic unit tests for verifying that the string
 * generation, including character escaping, works as expected.
 */
public class StringGenerationFromReaderTest
    extends BaseTest
{
    final static String[] SAMPLES = new String[] {
        "\"test\"",
        "\n", "\\n", "\r\n", "a\\b", "tab:\nok?",
        "a\tb\tc\n\fdef\t \tg\"\"\"h\"\\ijklmn\b",
        "\"\"\"", "\\r)'\"",
        "Longer text & other stuff:\twith some\r\n\r\n random linefeeds etc added in to cause some \"special\" handling \\\\ to occur...\n"
    };

    private final JsonFactory FACTORY = new JsonFactory();
    
    public void testBasicEscaping() throws Exception
    {
        doTestBasicEscaping();
    }

    // for [core#194]
    public void testMediumStringsBytes() throws Exception
    {
        final JsonFactory jsonF = new JsonFactory();
        for (int mode : ALL_BINARY_MODES) {
            for (int size : new int[] { 1100, 2300, 3800, 7500, 19000, 33333 }) {
                _testMediumStrings(jsonF, mode, size);
            }
        }
    }

    // for [core#194]
    public void testMediumStringsChars() throws Exception
    {
        final JsonFactory jsonF = new JsonFactory();
        for (int mode : ALL_TEXT_MODES) {
            for (int size : new int[] { 1100, 2300, 3800, 7500, 19000, 33333 }) {
                _testMediumStrings(jsonF, mode, size);
            }
        }
    }

    public void testLongerRandomSingleChunk() throws Exception
    {
        // Let's first generate 100k of pseudo-random characters, favoring
        // 7-bit ascii range
        for (int mode : ALL_TEXT_MODES) {
            for (int round = 0; round < 80; ++round) {
                String content = generateRandom(75000+round);
                _testLongerRandom(mode, content);
            }
        }
    }

    public void testLongerRandomMultiChunk() throws Exception
    {
        // Let's first generate 100k of pseudo-random characters, favoring
        // 7-bit ascii range
        for (int mode : ALL_TEXT_MODES) {
            for (int round = 0; round < 70; ++round) {
                String content = generateRandom(73000+round);
                _testLongerRandomMulti(mode, content, round);
            }
        }
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private String _generareMediumText(int minLen)
    {
        StringBuilder sb = new StringBuilder(minLen + 1000);
        Random rnd = new Random(minLen);
        do {
            switch (rnd.nextInt() % 4) {
            case 0:
                sb.append(" foo");
                break;
            case 1:
                sb.append(" bar");
                break;
            case 2:
                sb.append(String.valueOf(sb.length()));
                break;
            default:
                sb.append(" \"stuff\"");
                break;
            }
        } while (sb.length() < minLen);
        return sb.toString();
    }
    
    private String generateRandom(int len)
    {
        StringBuilder sb = new StringBuilder(len+1000); // pad for surrogates
        Random r = new Random(len);
        for (int i = 0; i < len; ++i) {
            if (r.nextBoolean()) { // non-ascii
                int value = r.nextInt() & 0xFFFF;
                // Otherwise easy, except that need to ensure that
                // surrogates are properly paired: and, also
                // their values do not exceed 0x10FFFF
                if (value >= 0xD800 && value <= 0xDFFF) {
                    // Let's discard first value, then, and produce valid pair
                    int fullValue = (r.nextInt() & 0xFFFFF);
                    sb.append((char) (0xD800 + (fullValue >> 10)));
                    value = 0xDC00 + (fullValue & 0x3FF);
                }
                sb.append((char) value);
            } else { // ascii
                sb.append((char) (r.nextInt() & 0x7F));
            }
        }
        return sb.toString();
    }

    private void _testMediumStrings(JsonFactory jsonF,
            int readMode, int length) throws Exception
    {
        String text = _generareMediumText(length);
        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        JsonGenerator gen = (readMode != MODE_READER) ? jsonF.createGenerator(bytes)
                : jsonF.createGenerator(sw);
        gen.writeStartArray();

        StringReader reader = new StringReader(text);
        gen.writeString(reader, -1);
        gen.writeEndArray();
        gen.close();

        JsonParser p;
        if (readMode == MODE_READER) {
            p = jsonF.createParser(sw.toString());
        } else {
            p = createParser(jsonF, readMode, bytes.toByteArray());
        }
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(text, p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void doTestBasicEscaping() throws Exception
    {
        for (int i = 0; i < SAMPLES.length; ++i) {
            String VALUE = SAMPLES[i];
            StringWriter sw = new StringWriter();
            JsonGenerator gen = FACTORY.createGenerator(sw);
            gen.writeStartArray();
            StringReader reader = new StringReader(VALUE);
            gen.writeString(reader, -1);
            gen.writeEndArray();
            gen.close();
            String docStr = sw.toString();
            JsonParser p = createParserUsingReader(docStr);
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            assertEquals(JsonToken.VALUE_STRING, t);
            assertEquals(VALUE, p.getText());
            assertEquals(JsonToken.END_ARRAY, p.nextToken());
            assertEquals(null, p.nextToken());
            p.close();
        }
    }

    private void _testLongerRandom(int readMode, String text)
        throws Exception
    {
        ByteArrayOutputStream bow = new ByteArrayOutputStream(text.length());
        JsonGenerator gen = FACTORY.createGenerator(bow, JsonEncoding.UTF8);
        gen.writeStartArray();
        StringReader reader = new StringReader(text);
        gen.writeString(reader, -1);
        gen.writeEndArray();
        gen.close();
        byte[] docData = bow.toByteArray();
        JsonParser p = createParser(FACTORY, readMode, docData);
        assertEquals(JsonToken.START_ARRAY, p.nextToken());
        JsonToken t = p.nextToken();
        assertEquals(JsonToken.VALUE_STRING, t);
        String act = p.getText();
        if (!text.equals(act)) {
            if (text.length() != act.length()) {
                fail("Expected string length "+text.length()+", actual "+act.length());
            }
            int i = 0;
            for (int len = text.length(); i < len; ++i) {
                if (text.charAt(i) != act.charAt(i)) {
                    break;
                }
            }
            fail("Strings differ at position #"+i+" (len "+text.length()+"): expected char 0x"+Integer.toHexString(text.charAt(i))+", actual 0x"+Integer.toHexString(act.charAt(i)));
        }
        assertEquals(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void _testLongerRandomMulti(int readMode, String text, int round)
        throws Exception
    {
        ByteArrayOutputStream bow = new ByteArrayOutputStream(text.length());
        JsonGenerator gen = FACTORY.createGenerator(bow, JsonEncoding.UTF8);
        gen.writeStartArray();
        StringReader reader = new StringReader(text);
        gen.writeString(reader, -1);
        gen.writeEndArray();
        gen.close();
        
        gen = FACTORY.createGenerator(bow, JsonEncoding.UTF8);
        gen.writeStartArray();
        gen.writeStartArray();

        Random rnd = new Random(text.length());
        int offset = 0;

        while (offset < text.length()) {
            int shift = 1 + ((rnd.nextInt() & 0xFFFFF) % 12); // 1 - 12
            int len = (1 << shift) + shift; // up to 4k
            if ((offset + len) >= text.length()) {
                len = text.length() - offset;
            } else {
            	// Need to avoid splitting surrogates though
            	char c = text.charAt(offset+len-1);
            	if (c >= 0xD800 && c < 0xDC00) {
            		++len;
            	}
            }
            reader = new StringReader(text.substring(offset, offset+len));
            gen.writeString(reader, -1);
            offset += len;
        }

        gen.writeEndArray();
        gen.close();
        byte[] docData = bow.toByteArray();
        JsonParser p = createParser(FACTORY, readMode, docData);
        assertEquals(JsonToken.START_ARRAY, p.nextToken());

        offset = 0;
        while (p.nextToken() == JsonToken.VALUE_STRING) {
            // Let's verify, piece by piece
            String act = p.getText();
            String exp = text.substring(offset, offset+act.length());
            if (act.length() != exp.length()) {
                fail("String segment ["+offset+" - "+(offset+act.length())+"[ differs; exp length "+exp+", actual "+act);                
            }
            if (!act.equals(exp)) {
                int i = 0;
                while (act.charAt(i) == exp.charAt(i)) {
                    ++i;
                }
                fail("String segment ["+offset+" - "+(offset+act.length())+"[ different at offset #"+i
                        +"; exp char 0x"+Integer.toHexString(exp.charAt(i))
                        +", actual 0x"+Integer.toHexString(act.charAt(i)));
            }
            offset += act.length();
        }
        assertEquals(JsonToken.END_ARRAY, p.currentToken());
        p.close();
    }

    // [jackson-core#556]
    public void testIssue556() throws Exception
    {
        StringBuilder sb = new StringBuilder(8000);
        sb.append('"');
        for (int i = 0; i < 7988; i++) {
             sb.append("a");
        }
        sb.append('"');
        JsonGenerator g = FACTORY.createGenerator(new ByteArrayOutputStream());

        g.writeStartArray();
        _write556(g, sb.toString());
        _write556(g, "b");
        _write556(g, "c");
        g.writeEndArray();
        g.close();
    }

    private void _write556(JsonGenerator g, String value) throws Exception
    {
        g.writeString(new StringReader(value), -1);
    }
}
