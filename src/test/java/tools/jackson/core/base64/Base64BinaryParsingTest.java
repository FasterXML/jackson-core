package tools.jackson.core.base64;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.*;

class Base64BinaryParsingTest
    extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void base64UsingInputStream() throws Exception
    {
        _testBase64Text(MODE_INPUT_STREAM);
        _testBase64Text(MODE_INPUT_STREAM_THROTTLED);
        _testBase64Text(MODE_DATA_INPUT);
    }

    @Test
    void base64UsingReader() throws Exception
    {
        _testBase64Text(MODE_READER);
    }

    @Test
    void streaming() throws IOException
    {
        _testStreaming(MODE_INPUT_STREAM);
        _testStreaming(MODE_INPUT_STREAM_THROTTLED);
        _testStreaming(MODE_DATA_INPUT);
        _testStreaming(MODE_READER);
    }

    @Test
    void simple() throws IOException
    {
        for (int mode : ALL_MODES) {
            // [core#414]: Allow leading/trailign white-space, ensure it is accepted
            _testSimple(mode, false, false);
            _testSimple(mode, true, false);
            _testSimple(mode, false, true);
            _testSimple(mode, true, true);
        }
    }

    @Test
    void inArray() throws IOException
    {
        for (int mode : ALL_MODES) {
            _testInArray(mode);
        }
    }

    @Test
    void withEscaped() throws IOException {
        for (int mode : ALL_MODES) {
            _testEscaped(mode);
        }
    }

    @Test
    void withEscapedPadding() throws IOException {
        for (int mode : ALL_MODES) {
            _testEscapedPadding(mode);
        }
    }

    @Test
    void invalidTokenForBase64() throws IOException
    {
        for (int mode : ALL_MODES) {

            // First: illegal padding
            JsonParser p = createParser(JSON_F, mode, "[ ]");
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                p.getBinaryValue();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "current token");
                verifyException(e, "can not access as binary");
            }
            p.close();
        }
    }

    @Test
    void invalidChar() throws IOException
    {
        for (int mode : ALL_MODES) {

            // First: illegal padding
            JsonParser p = createParser(JSON_F, mode, q("a==="));
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            try {
                p.getBinaryValue(Base64Variants.MIME);
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "padding only legal");
            }
            p.close();

            // second: invalid space within
            p = createParser(JSON_F, mode, q("ab de"));
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            try {
                p.getBinaryValue(Base64Variants.MIME);
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "illegal white space");
            }
            p.close();

            // third: something else
            p = createParser(JSON_F, mode, q("ab#?"));
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            try {
                p.getBinaryValue(Base64Variants.MIME);
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "illegal character '#'");
            }
            p.close();
        }
    }

    @Test
    void okMissingPadding() throws IOException {
        final byte[] DOC1 = new byte[] { (byte) 0xAD };
        _testOkMissingPadding(DOC1, MODE_INPUT_STREAM);
        _testOkMissingPadding(DOC1, MODE_INPUT_STREAM_THROTTLED);
        _testOkMissingPadding(DOC1, MODE_READER);
        _testOkMissingPadding(DOC1, MODE_DATA_INPUT);

        final byte[] DOC2 = new byte[] { (byte) 0xAC, (byte) 0xDC };
        _testOkMissingPadding(DOC2, MODE_INPUT_STREAM);
        _testOkMissingPadding(DOC2, MODE_INPUT_STREAM_THROTTLED);
        _testOkMissingPadding(DOC2, MODE_READER);
        _testOkMissingPadding(DOC2, MODE_DATA_INPUT);
    }

    private void _testOkMissingPadding(byte[] input, int mode)
    {
        final Base64Variant b64 = Base64Variants.MODIFIED_FOR_URL;
        final String encoded = b64.encode(input, false);
        JsonParser p = createParser(JSON_F, mode, q(encoded));
        // 1 byte -> 2 encoded chars; 2 bytes -> 3 encoded chars
        assertEquals(input.length+1, encoded.length());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        byte[] actual = p.getBinaryValue(b64);
        assertArrayEquals(input, actual);
        p.close();
    }

    @Test
    void failDueToMissingPadding() throws IOException {
        final String DOC1 = q("fQ"); // 1 bytes, no padding
        _testFailDueToMissingPadding(DOC1, MODE_INPUT_STREAM);
        _testFailDueToMissingPadding(DOC1, MODE_INPUT_STREAM_THROTTLED);
        _testFailDueToMissingPadding(DOC1, MODE_READER);
        _testFailDueToMissingPadding(DOC1, MODE_DATA_INPUT);

        final String DOC2 = q("A/A"); // 2 bytes, no padding
        _testFailDueToMissingPadding(DOC2, MODE_INPUT_STREAM);
        _testFailDueToMissingPadding(DOC2, MODE_INPUT_STREAM_THROTTLED);
        _testFailDueToMissingPadding(DOC2, MODE_READER);
        _testFailDueToMissingPadding(DOC2, MODE_DATA_INPUT);
    }

    private void _testFailDueToMissingPadding(String doc, int mode) {
        final String EXP_EXCEPTION_MATCH = "Unexpected end of base64-encoded String: base64 variant 'MIME' expects padding";

        // First, without getting text value first:
        JsonParser p = createParser(JSON_F, mode, doc);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        try {
            /*byte[] b =*/ p.getBinaryValue(Base64Variants.MIME);
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, EXP_EXCEPTION_MATCH);
        }
        p.close();

        // second, access String first
        p = createParser(JSON_F, mode, doc);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        /*String str =*/ p.getString();
        try {
            /*byte[] b =*/ p.getBinaryValue(Base64Variants.MIME);
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, EXP_EXCEPTION_MATCH);
        }
        p.close();
    }

    /*
    /**********************************************************
    /* Test helper methods
    /**********************************************************
     */

    @SuppressWarnings("resource")
    void _testBase64Text(int mode) throws Exception
    {
        // let's actually iterate over sets of encoding modes, lengths

        final int[] LENS = { 1, 2, 3, 4, 7, 9, 32, 33, 34, 35 };
        final Base64Variant[] VARIANTS = {
                Base64Variants.MIME,
                Base64Variants.MIME_NO_LINEFEEDS,
                Base64Variants.MODIFIED_FOR_URL,
                Base64Variants.PEM
        };

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter chars = null;
        for (int len : LENS) {
            byte[] input = new byte[len];
            for (int i = 0; i < input.length; ++i) {
                input[i] = (byte) i;
            }
            for (Base64Variant variant : VARIANTS) {
                JsonGenerator g;

                if (mode == MODE_READER) {
                    chars = new StringWriter();
                    g = JSON_F.createGenerator(ObjectWriteContext.empty(), chars);
                } else {
                    bytes.reset();
                    g = JSON_F.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
                }
                g.writeBinary(variant, input, 0, input.length);
                g.close();
                JsonParser p;
                if (mode == MODE_READER) {
                    p = JSON_F.createParser(ObjectReadContext.empty(), chars.toString());
                } else {
                    p = createParser(JSON_F, mode, bytes.toByteArray());
                }
                assertToken(JsonToken.VALUE_STRING, p.nextToken());

                // minor twist: for even-length values, force access as String first:
                if ((len & 1) == 0) {
                    assertNotNull(p.getString());
                }

                byte[] data = null;
                try {
                    data = p.getBinaryValue(variant);
                } catch (Exception e) {
                    RuntimeException ioException = new RuntimeException("Failed (variant "+variant+", data length "+len+"): "+e.getMessage());
                    ioException.initCause(e);
                    throw ioException;
                }
                assertNotNull(data);
                assertArrayEquals(data, input);
                if (mode != MODE_DATA_INPUT) { // no look-ahead for DataInput
                    assertNull(p.nextToken());
                }
                p.close();
            }
        }
    }

    private byte[] _generateData(int size)
    {
        byte[] result = new byte[size];
        for (int i = 0; i < size; ++i) {
            result[i] = (byte) (i % 255);
        }
        return result;
    }

    private void _testStreaming(int mode)
    {
        final int[] SIZES = new int[] {
            1, 2, 3, 4, 5, 6,
            7, 8, 12,
            100, 350, 1900, 6000, 19000, 65000,
            139000
        };

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter chars = null;

        for (int size : SIZES) {
            byte[] data = _generateData(size);
            JsonGenerator g;
            if (mode == MODE_READER) {
                chars = new StringWriter();
                g = JSON_F.createGenerator(ObjectWriteContext.empty(), chars);
            } else {
                bytes.reset();
                g = JSON_F.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
            }

            g.writeStartObject();
            g.writeName("b");
            g.writeBinary(data);
            g.writeEndObject();
            g.close();

            // and verify
            JsonParser p;
            if (mode == MODE_READER) {
                p = JSON_F.createParser(ObjectReadContext.empty(), chars.toString());
            } else {
                p = createParser(JSON_F, mode, bytes.toByteArray());
            }
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("b", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            ByteArrayOutputStream result = new ByteArrayOutputStream(size);
            int gotten = p.readBinaryValue(result);
            assertEquals(size, gotten);
            assertArrayEquals(data, result.toByteArray());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            if (mode != MODE_DATA_INPUT) { // no look-ahead for DataInput
                assertNull(p.nextToken());
            }
            p.close();
        }
    }

    private void _testSimple(int mode, boolean leadingWS, boolean trailingWS)
    {
        // The usual sample input string, from Thomas Hobbes's "Leviathan"
        // (via Wikipedia)
        final String RESULT = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
        final byte[] RESULT_BYTES = RESULT.getBytes(StandardCharsets.US_ASCII);

        // And here's what should produce it...
        String INPUT_STR =
 "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz"
+"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg"
+"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu"
+"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo"
+"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
            ;
        if (leadingWS) {
            INPUT_STR = "   "+INPUT_STR;
        }
        if (leadingWS) {
            INPUT_STR = INPUT_STR+"   ";
        }

        final String DOC = "\""+INPUT_STR+"\"";
        JsonParser p = createParser(JSON_F, mode, DOC);

        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        byte[] data = p.getBinaryValue();
        assertNotNull(data);
        assertArrayEquals(RESULT_BYTES, data);
        p.close();
    }

    private void _testInArray(int mode)
    {
        final int entryCount = 7;

        StringWriter sw = new StringWriter();
        JsonGenerator jg = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        jg.writeStartArray();

        byte[][] entries = new byte[entryCount][];
        for (int i = 0; i < entryCount; ++i) {
            byte[] b = new byte[200 + i * 100];
            for (int x = 0; x < b.length; ++x) {
                b[x] = (byte) (i + x);
            }
            entries[i] = b;
            jg.writeBinary(b);
        }

        jg.writeEndArray();
        jg.close();

        JsonParser p = createParser(JSON_F, mode, sw.toString());

        assertToken(JsonToken.START_ARRAY, p.nextToken());

        for (int i = 0; i < entryCount; ++i) {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            byte[] b = p.getBinaryValue();
            assertArrayEquals(entries[i], b);
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void _testEscaped(int mode) throws IOException
    {
        // Input: "Test!" -> "VGVzdCE="

        // First, try with embedded linefeed half-way through:

        String DOC = q("VGVz\\ndCE="); // note: must double-quote to get linefeed
        JsonParser p = createParser(JSON_F, mode, DOC);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        byte[] b = p.getBinaryValue();
        assertEquals("Test!", new String(b, "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();

        // and then with escaped chars
//            DOC = quote("V\\u0047V\\u007AdCE="); // note: must escape backslash...
        DOC = q("V\\u0047V\\u007AdCE="); // note: must escape backslash...
        p = createParser(JSON_F, mode, DOC);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        b = p.getBinaryValue();
        assertEquals("Test!", new String(b, "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();
    }

    private void _testEscapedPadding(int mode) throws IOException
    {
        // Input: "Test!" -> "VGVzdCE="
        final String DOC = q("VGVzdCE\\u003d");

        // 06-Sep-2018, tatu: actually one more, test escaping of padding
        JsonParser p = createParser(JSON_F, mode, DOC);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("Test!", new String(p.getBinaryValue(), "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();

        // also, try out alternate access method
        p = createParser(JSON_F, mode, DOC);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("Test!", new String(_readBinary(p), "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();

        // and then different padding; "X" -> "WA=="
        final String DOC2 = q("WA\\u003D\\u003D");
        p = createParser(JSON_F, mode, DOC2);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("X", new String(p.getBinaryValue(), "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();

        p = createParser(JSON_F, mode, DOC2);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("X", new String(_readBinary(p), "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();
    }

    private byte[] _readBinary(JsonParser p)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        p.readBinaryValue(bytes);
        return bytes.toByteArray();
    }
}
