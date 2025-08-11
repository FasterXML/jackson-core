package tools.jackson.core.unittest.io;

import java.io.StringWriter;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.io.JsonStringEncoder;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestJsonStringEncoder
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    @Test
    void quoteAsString() throws Exception
    {
        JsonStringEncoder encoder = new JsonStringEncoder();
        char[] result = encoder.quoteAsCharArray("foobar");
        assertArrayEquals("foobar".toCharArray(), result);
        result = encoder.quoteAsCharArray("\"x\"");
        assertArrayEquals("\\\"x\\\"".toCharArray(), result);
    }

    @Test
    void quoteCharSequenceAsString() throws Exception
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
    @Test
    void quoteLongAsString() throws Exception
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
        char[] result = encoder.quoteAsCharArray(input);
        assertEquals(2*input.length(), result.length);
        assertEquals(exp, new String(result));

    }

    @Test
    void quoteLongCharSequenceAsString() throws Exception
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

    @Test
    void quoteAsUTF8() throws Exception
    {
        // In this case, let's actually use existing JsonGenerator to produce expected values
        JsonFactory f = new JsonFactory();
        JsonStringEncoder encoder = new JsonStringEncoder();
        for (int length : new int[] {
                5, 19, 200, 7000, 21000, 37000
            }) {
            _quoteAsUTF8(f, encoder, length);
        }
    }

    private void _quoteAsUTF8(JsonFactory f, JsonStringEncoder encoder, int length) throws Exception
    {
        String str = generateRandom(length);
        StringWriter sw = new StringWriter(length*2);
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw)) {
            g.writeString(str);
        }
        String encoded = sw.toString();
        // ok, except need to remove surrounding quotes
        encoded = encoded.substring(1, encoded.length() - 1);
        byte[] expected = encoded.getBytes("UTF-8");
        byte[] actual = encoder.quoteAsUTF8(str);
        assertArrayEquals(expected, actual,
                "For content length "+length);
    }

    @Test
    void encodeAsUTF8() throws Exception
    {
        JsonStringEncoder encoder = new JsonStringEncoder();
        String[] strings = new String[] {
                "a", "foobar", "p\u00f6ll\u00f6", "\"foo\"",
                generateRandom(200),
                generateRandom(5000),
                generateRandom(39000)
        };
        for (String str : strings) {
            assertArrayEquals(str.getBytes("UTF-8"), encoder.encodeAsUTF8(str));
        }
    }

    @Test
    void ctrlChars() throws Exception
    {
        char[] input = new char[] { 0, 1, 2, 3, 4 };
        char[] quoted = JsonStringEncoder.getInstance().quoteAsCharArray(new String(input));
        assertEquals("\\u0000\\u0001\\u0002\\u0003\\u0004", new String(quoted));
    }

    // [JACKSON-884]
    @Test
    void charSequenceWithCtrlChars() throws Exception
    {
        char[] input = new char[] { 0, 1, 2, 3, 4 };
        StringBuilder builder = new StringBuilder();
        builder.append(input);
        StringBuilder output = new StringBuilder();
        JsonStringEncoder.getInstance().quoteAsString(builder, output);
        assertEquals("\\u0000\\u0001\\u0002\\u0003\\u0004", output.toString());
    }

    // [core#712]: simple sanity checks for calculation logic
    @Test
    void byteBufferDefaultSize()
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
    @Test
    void charBufferDefaultSize()
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

