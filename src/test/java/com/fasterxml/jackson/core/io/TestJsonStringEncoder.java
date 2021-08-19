package com.fasterxml.jackson.core.io;

import java.io.StringWriter;
import java.util.Random;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.*;

public class TestJsonStringEncoder
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testQuoteAsString() throws Exception
    {
        JsonStringEncoder encoder = new JsonStringEncoder();
        char[] result = encoder.quoteAsString("foobar");
        assertArrayEquals("foobar".toCharArray(), result);
        result = encoder.quoteAsString("\"x\"");
        assertArrayEquals("\\\"x\\\"".toCharArray(), result);

        // and simply for sake of code coverage
        result = encoder.quoteAsString(new StringBuilder("foobar"));
        assertArrayEquals("foobar".toCharArray(), result);
        result = encoder.quoteAsString(new StringBuilder("\"x\""));
        assertArrayEquals("\\\"x\\\"".toCharArray(), result);
    }

    public void testQuoteCharSequenceAsString() throws Exception
    {
        StringBuilder output = new StringBuilder();
        StringBuilder builder = new StringBuilder();
        builder.append("foobar");
        JsonStringEncoder.getInstance().quoteAsString(builder, output);
        assertEquals("foobar", output.toString());
        builder.setLength(0);
        output.setLength(0);
        builder.append("\"x\"");
        JsonStringEncoder.getInstance().quoteAsString(builder, output);
        assertEquals("\\\"x\\\"", output.toString());
    }

    // For [JACKSON-853]
    public void testQuoteLongAsString() throws Exception
    {
        JsonStringEncoder encoder = new JsonStringEncoder();
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < 1111; ++i) {
            sb.append('"');
            sb2.append("\\\"");
        }
        String input = sb.toString();
        String exp = sb2.toString();
        char[] result = encoder.quoteAsString(input);
        assertEquals(2*input.length(), result.length);
        assertEquals(exp, new String(result));
    }

    public void testQuoteLongCharSequenceAsString() throws Exception
    {
        StringBuilder output = new StringBuilder();
        StringBuilder input = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < 1111; ++i) {
            input.append('"');
            sb2.append("\\\"");
        }
        String exp = sb2.toString();
        JsonStringEncoder.getInstance().quoteAsString(input, output);
        assertEquals(2*input.length(), output.length());
        assertEquals(exp, output.toString());

    }

    public void testQuoteAsUTF8() throws Exception
    {
        // In this case, let's actually use existing JsonGenerator to produce expected values
        JsonFactory f = new JsonFactory();
        JsonStringEncoder encoder = new JsonStringEncoder();
        int[] lengths = new int[] {
            5, 19, 200, 7000, 21000, 37000
        };
        for (int length : lengths) {
            String str = generateRandom(length);
            StringWriter sw = new StringWriter(length*2);
            JsonGenerator jgen = f.createGenerator(sw);
            jgen.writeString(str);
            jgen.close();
            String encoded = sw.toString();
            // ok, except need to remove surrounding quotes
            encoded = encoded.substring(1, encoded.length() - 1);
            byte[] expected = encoded.getBytes("UTF-8");
            byte[] actual = encoder.quoteAsUTF8(str);
            assertArrayEquals(expected, actual);
        }
    }

    public void testEncodeAsUTF8() throws Exception
    {
        JsonStringEncoder encoder = new JsonStringEncoder();
        String[] strings = new String[] {
                "a", "foobar", "p\u00f6ll\u00f6", "\"foo\"",
                generateRandom(200),
                generateRandom(5000),
                generateRandom(39000)
        };
        for (String str : strings) {
            final byte[] exp = str.getBytes("UTF-8");
            assertArrayEquals(exp, encoder.encodeAsUTF8(str));
            // and for 2.x code coverage (only
            assertArrayEquals(exp, encoder.encodeAsUTF8((CharSequence)str));
        }
    }

    public void testCtrlChars() throws Exception
    {
        char[] input = new char[] { 0, 1, 2, 3, 4 };
        char[] quoted = JsonStringEncoder.getInstance().quoteAsString(new String(input));
        assertEquals("\\u0000\\u0001\\u0002\\u0003\\u0004", new String(quoted));
    }

    // [JACKSON-884]
    public void testCharSequenceWithCtrlChars() throws Exception
    {
        char[] input = new char[] { 0, 1, 2, 3, 4 };
        StringBuilder builder = new StringBuilder();
        builder.append(input);
        StringBuilder output = new StringBuilder();
        JsonStringEncoder.getInstance().quoteAsString(builder, output);
        assertEquals("\\u0000\\u0001\\u0002\\u0003\\u0004", output.toString());
    }

    // [core#712]: simple sanity checks for calculation logic
    public void testByteBufferDefaultSize()
    {
        // byte size is simple, x2 except below buffer size 24
        assertEquals(JsonStringEncoder.MIN_BYTE_BUFFER_SIZE,
                JsonStringEncoder._initialByteBufSize(1));
        assertEquals(JsonStringEncoder.MIN_BYTE_BUFFER_SIZE,
                JsonStringEncoder._initialByteBufSize(11));

        assertEquals(36, JsonStringEncoder._initialByteBufSize(20));
        assertEquals(73, JsonStringEncoder._initialByteBufSize(45));
        assertEquals(1506, JsonStringEncoder._initialByteBufSize(1000));
        assertEquals(9006, JsonStringEncoder._initialByteBufSize(6000));

        // and up to max initial size
        assertEquals(JsonStringEncoder.MAX_BYTE_BUFFER_SIZE,
                JsonStringEncoder._initialByteBufSize(JsonStringEncoder.MAX_BYTE_BUFFER_SIZE + 1));
        assertEquals(JsonStringEncoder.MAX_BYTE_BUFFER_SIZE,
                JsonStringEncoder._initialByteBufSize(999999));
    }

    // [core#712]: simple sanity checks for calculation logic
    public void testCharBufferDefaultSize()
    {
        // char[] bit more complex, starts with minimum size of 16
        assertEquals(JsonStringEncoder.MIN_CHAR_BUFFER_SIZE,
                JsonStringEncoder._initialCharBufSize(1));
        assertEquals(JsonStringEncoder.MIN_CHAR_BUFFER_SIZE,
                JsonStringEncoder._initialCharBufSize(8));

        // and then grows by ~5%
        assertEquals(62, JsonStringEncoder._initialCharBufSize(50));
        assertEquals(118, JsonStringEncoder._initialCharBufSize(100));
        assertEquals(1131, JsonStringEncoder._initialCharBufSize(1000));
        assertEquals(9000, JsonStringEncoder._initialCharBufSize(8000));

        // up to max, simi
        assertEquals(JsonStringEncoder.MAX_CHAR_BUFFER_SIZE,
                JsonStringEncoder._initialCharBufSize(32000));
        assertEquals(JsonStringEncoder.MAX_CHAR_BUFFER_SIZE,
                JsonStringEncoder._initialCharBufSize(900000));
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private String generateRandom(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        Random rnd = new Random(length);
        for (int i = 0; i < length; ++i) {
            // let's limit it not to include surrogate pairs:
            char ch = (char) rnd.nextInt(0xCFFF);
            sb.append(ch);
        }
        return sb.toString();
    }
}

