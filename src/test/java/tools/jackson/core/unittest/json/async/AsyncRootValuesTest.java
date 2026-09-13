package tools.jackson.core.unittest.json.async;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

class AsyncRootValuesTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    /*
    /**********************************************************************
    /* Simple token (true, false, null) tests
    /**********************************************************************
     */

    // [core#1664]: multi-byte UTF-8 character must be decoded before reporting
    // missing root-level separator (when whole sequence is in the buffer)
    @Test
    void missingRootSeparatorUTF8() throws Exception {
        // 2-byte sequence: NBSP (U+00A0)
        _testMissingRootSeparatorUTF8(new byte[] { '1', '.', '5', (byte) 0xC2, (byte) 0xA0 },
                90, "code 160");
        // 3-byte sequence: LINE SEPARATOR (U+2028)
        _testMissingRootSeparatorUTF8(new byte[] { '1', '.', '5', (byte) 0xE2, (byte) 0x80, (byte) 0xA8 },
                90, "code 8232");
        // ... but decoding is strictly best-effort: if sequence is not (yet) fully
        // available, lead byte is reported as-is (non-blocking parser cannot wait
        // for more input just to build a message)
        _testMissingRootSeparatorUTF8(new byte[] { '1', '.', '5', (byte) 0xC2, (byte) 0xA0 },
                4, "code 194");
        _testMissingRootSeparatorUTF8(new byte[] { '1', '.', '5', (byte) 0xC2 },
                90, "code 194");
        // ... and malformed UTF-8 must not replace the error caller asked for
        _testMissingRootSeparatorUTF8(new byte[] { '1', '.', '5', (byte) 0xC2, (byte) 0x41 },
                90, "code 194"); // invalid continuation byte
        _testMissingRootSeparatorUTF8(new byte[] { '1', '.', '5', (byte) 0xFF },
                90, "code 255"); // invalid lead byte
    }

    private void _testMissingRootSeparatorUTF8(byte[] doc, int readSize, String expCode)
        throws Exception
    {
        try (AsyncReaderWrapper r = asyncForBytes(JSON_F, readSize, doc, 0)) {
            r.nextToken();
            r.nextToken();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, expCode);
            verifyException(e, "Expected space separating root-level values");
        }
    }

    @Test
    void tokenRootTokens() throws Exception {
        _testTokenRootTokens(JsonToken.VALUE_TRUE, "true");
        _testTokenRootTokens(JsonToken.VALUE_FALSE, "false");
        _testTokenRootTokens(JsonToken.VALUE_NULL, "null");

        _testTokenRootTokens(JsonToken.VALUE_TRUE, "true  ");
        _testTokenRootTokens(JsonToken.VALUE_FALSE, "false  ");
        _testTokenRootTokens(JsonToken.VALUE_NULL, "null  ");
    }

    private void _testTokenRootTokens(JsonToken expToken, String doc) throws Exception
    {
        byte[] input = _jsonDoc(doc);
        JsonFactory f = JSON_F;
        _testTokenRootTokens(expToken, f, input, 0, 90);
        _testTokenRootTokens(expToken, f, input, 0, 3);
        _testTokenRootTokens(expToken, f, input, 0, 2);
        _testTokenRootTokens(expToken, f, input, 0, 1);

        _testTokenRootTokens(expToken, f, input, 1, 90);
        _testTokenRootTokens(expToken, f, input, 1, 3);
        _testTokenRootTokens(expToken, f, input, 1, 1);
    }

    private void _testTokenRootTokens(JsonToken expToken, JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertNull(r.currentToken());

        assertToken(expToken, r.nextToken());
        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }

    /*
    /**********************************************************************
    /* Root-level sequences
    /**********************************************************************
     */

    @Test
    void tokenRootSequence() throws Exception
    {
        byte[] input = _jsonDoc("\n[ true, false,\nnull  ,null\n,true,false]");

        JsonFactory f = JSON_F;
        _testTokenRootSequence(f, input, 0, 900);
        _testTokenRootSequence(f, input, 0, 3);
        _testTokenRootSequence(f, input, 0, 1);

        _testTokenRootSequence(f, input, 1, 900);
        _testTokenRootSequence(f, input, 1, 3);
        _testTokenRootSequence(f, input, 1, 1);
    }

    private void _testTokenRootSequence(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertNull(r.currentToken());

        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.VALUE_NULL, r.nextToken());
        assertToken(JsonToken.VALUE_NULL, r.nextToken());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }

    @Test
    void mixedRootSequence() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(100);

        // Let's simply concatenate documents...
        bytes.write(_jsonDoc("{ \"a\" : 4 }"));
        bytes.write(_jsonDoc("[ 12, -987,false ]"));
        bytes.write(_jsonDoc(" 12356"));
        bytes.write(_jsonDoc(" true"));
        byte[] input = bytes.toByteArray();

        JsonFactory f = JSON_F;
        _testMixedRootSequence(f, input, 0, 100);
        _testMixedRootSequence(f, input, 0, 3);
        _testMixedRootSequence(f, input, 0, 1);

        _testMixedRootSequence(f, input, 1, 100);
        _testMixedRootSequence(f, input, 1, 3);
        _testMixedRootSequence(f, input, 1, 1);
    }

    private void _testMixedRootSequence(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertNull(r.currentToken());

        // { "a":4 }
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals("a", r.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(4, r.getIntValue());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        // [ 12, -987, false]
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(12, r.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(-987, r.getIntValue());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(12356, r.getIntValue());

        assertToken(JsonToken.VALUE_TRUE, r.nextToken());

        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }
}
