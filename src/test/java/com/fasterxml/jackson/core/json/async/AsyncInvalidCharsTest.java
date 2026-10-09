package com.fasterxml.jackson.core.json.async;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

// Tests for verifying things such as handling of invalid control characters;
// decoding of UTF-8 BOM.
class AsyncInvalidCharsTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void utf8BOMHandling() throws Exception
    {
        _testUtf8BOMHandling(0, 99);
        _testUtf8BOMHandling(0, 5);
        _testUtf8BOMHandling(0, 3);
        _testUtf8BOMHandling(0, 2);
        _testUtf8BOMHandling(0, 1);

        _testUtf8BOMHandling(2, 99);
        _testUtf8BOMHandling(2, 1);
    }

    private void _testUtf8BOMHandling(int offset, int readSize) throws Exception
    {
        _testUTF8BomOk(offset, readSize);
        _testUTF8BomFail(offset, readSize, 1,
                "Unexpected byte 0x5b following 0xEF; should get 0xBB as second byte");
        _testUTF8BomFail(offset, readSize, 2,
                "Unexpected byte 0x5b following 0xEF 0xBB; should get 0xBF as third byte");
    }

    private void _testUTF8BomOk(int offset, int readSize) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // first, write BOM:
        bytes.write(0xEF);
        bytes.write(0xBB);
        bytes.write(0xBF);
        bytes.write("[ 1 ]".getBytes("UTF-8"));
        byte[] doc = bytes.toByteArray();

        AsyncReaderWrapper p = asyncForBytes(JSON_F, readSize, doc, offset);

        assertEquals(JsonToken.START_ARRAY, p.nextToken());
        // should also have skipped first 3 bytes of BOM; but do we have offset available?
        /* Alas, due to [core#111], we have to omit BOM in calculations
         * as we do not know what the offset is due to -- may need to revisit, if this
         * discrepancy becomes an issue. For now it just means that BOM is considered
         * "out of stream" (not part of input).
         */

        JsonLocation loc = p.parser().currentTokenLocation();
        // so if BOM was consider in-stream (part of input), this should expect 3:
        // (NOTE: this is location for START_ARRAY token, now)
        assertEquals(-1, loc.getCharOffset());

// !!! TODO: fix location handling
        /*
        assertEquals(0, loc.getByteOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());
*/
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void _testUTF8BomFail(int offset, int readSize,
            int okBytes, String verify) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(0xEF);
        if (okBytes > 1) {
            bytes.write(0xBB);
        }
        bytes.write("[ 1 ]".getBytes("UTF-8"));
        byte[] doc = bytes.toByteArray();

        try (AsyncReaderWrapper p = asyncForBytes(JSON_F, readSize, doc, offset)) {
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, verify);
        }
    }

    // 09-Oct-2026, tatu: split BOM followed by white space split at end of
    //   chunk used to fail with "Internal error"
    @Test
    void utf8BOMFollowedBySplitWhitespace() throws Exception
    {
        final byte[] doc = _withBOM(" \n [ 1 ] ");
        for (int readSize = 1; readSize <= doc.length; ++readSize) {
            for (boolean byteBuffer : new boolean[] { false, true }) {
                try (AsyncReaderWrapper p = _async(JSON_F, byteBuffer, readSize, doc)) {
                    assertEquals(JsonToken.START_ARRAY, p.nextToken());
                    assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertEquals(1, p.getIntValue());
                    assertEquals(JsonToken.END_ARRAY, p.nextToken());
                    assertNull(p.nextToken());
                }
            }
        }
    }

    // BOM only allowed as the very first bytes: not after white space,
    // comment or another BOM
    @Test
    void utf8BOMNotFirst() throws Exception
    {
        final JsonFactory commentF = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
        _testBOMNotFirst(JSON_F, _concat(" ".getBytes("UTF-8"), _withBOM("[ 1 ]")));
        _testBOMNotFirst(commentF, _concat("/* x */".getBytes("UTF-8"), _withBOM("[ 1 ]")));
        _testBOMNotFirst(JSON_F, _withBOM(new String(_withBOM("[ 1 ]"), "UTF-8")));
    }

    private void _testBOMNotFirst(JsonFactory f, byte[] doc) throws Exception
    {
        for (int readSize = 1; readSize <= doc.length; ++readSize) {
            for (boolean byteBuffer : new boolean[] { false, true }) {
                try (AsyncReaderWrapper p = _async(f, byteBuffer, readSize, doc)) {
                    JsonToken t = p.nextToken();
                    fail("Should not pass for readSize "+readSize+", byteBuffer="+byteBuffer+"; got "+t);
                } catch (JsonParseException e) {
                    verifyException(e, "Unexpected character");
                }
            }
        }
    }

    // Repeated BOMs must fail with regular exception, not excessive recursion
    @Test
    void utf8ManyBOMs() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < 200_000; ++i) {
            bytes.write(0xEF);
            bytes.write(0xBB);
            bytes.write(0xBF);
        }
        bytes.write("[ 1 ]".getBytes("UTF-8"));
        final byte[] doc = bytes.toByteArray();
        try (AsyncReaderWrapper p = asyncForBytes(JSON_F, doc.length, doc, 0)) {
            JsonToken t = p.nextToken();
            fail("Should not pass; got "+t);
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character");
        }
    }

    // 09-Oct-2026, tatu: RS (if enabled) at start of document used to be rejected
    @Test
    void leadingRecordSeparator() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_RS_CONTROL_CHAR)
                .build();
        final String DOC = "\u001E[1]\n\u001E[2]\n";
        _testLeadingRS(f, DOC.getBytes("UTF-8"));
        _testLeadingRS(f, (" "+DOC).getBytes("UTF-8"));
        _testLeadingRS(f, _withBOM(DOC));
    }

    private void _testLeadingRS(JsonFactory f, byte[] doc) throws Exception
    {
        for (int readSize = 1; readSize <= doc.length; ++readSize) {
            for (boolean byteBuffer : new boolean[] { false, true }) {
                try (AsyncReaderWrapper p = _async(f, byteBuffer, readSize, doc)) {
                    for (int i = 1; i <= 2; ++i) {
                        assertEquals(JsonToken.START_ARRAY, p.nextToken());
                        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                        assertEquals(i, p.getIntValue());
                        assertEquals(JsonToken.END_ARRAY, p.nextToken());
                    }
                    assertNull(p.nextToken());
                }
            }
        }
    }

    // White space only content with end-of-input: no NOT_AVAILABLE before end
    @Test
    void whitespaceOnlyWithEndOfInput() throws Exception
    {
        for (byte[] doc : new byte[][] { "  ".getBytes("UTF-8"), _withBOM(" \n") }) {
            try (NonBlockingJsonParser p = (NonBlockingJsonParser) JSON_F.createNonBlockingByteArrayParser()) {
                p.feedInput(doc, 0, doc.length);
                p.endOfInput();
                assertNull(p.nextToken());
            }
        }
    }

    // Leading white space alone does not change current token (remains null)
    @Test
    void leadingWhitespaceKeepsNullToken() throws Exception
    {
        try (NonBlockingJsonParser p = (NonBlockingJsonParser) JSON_F.createNonBlockingByteArrayParser()) {
            byte[] ws = " \n ".getBytes("UTF-8");
            p.feedInput(ws, 0, ws.length);
            assertEquals(JsonToken.NOT_AVAILABLE, p.nextToken());
            assertNull(p.currentToken());
            p.feedInput(ws, 0, ws.length);
            assertEquals(JsonToken.NOT_AVAILABLE, p.nextToken());
            assertNull(p.currentToken());
            byte[] doc = "[1]".getBytes("UTF-8");
            p.feedInput(doc, 0, doc.length);
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    // Same with split BOM before white space
    @Test
    void splitBOMAndWhitespaceKeepsNullToken() throws Exception
    {
        try (NonBlockingJsonParser p = (NonBlockingJsonParser) JSON_F.createNonBlockingByteArrayParser()) {
            p.feedInput(new byte[] { (byte) 0xEF }, 0, 1);
            assertEquals(JsonToken.NOT_AVAILABLE, p.nextToken());
            p.feedInput(new byte[] { (byte) 0xBB, (byte) 0xBF, ' ' }, 0, 3);
            assertEquals(JsonToken.NOT_AVAILABLE, p.nextToken());
            assertNull(p.currentToken());
            byte[] doc = "[1]".getBytes("UTF-8");
            p.feedInput(doc, 0, doc.length);
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    private static AsyncReaderWrapper _async(JsonFactory f, boolean byteBuffer,
            int readSize, byte[] doc) throws Exception
    {
        return byteBuffer ? asyncForByteBuffer(f, readSize, doc, 0)
                : asyncForBytes(f, readSize, doc, 0);
    }

    private static byte[] _concat(byte[] a, byte[] b)
    {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static byte[] _withBOM(String json) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(0xEF);
        bytes.write(0xBB);
        bytes.write(0xBF);
        bytes.write(json.getBytes("UTF-8"));
        return bytes.toByteArray();
    }

    @Test
    void handlingOfInvalidSpace() throws Exception
    {
        _testHandlingOfInvalidSpace(0, 99);
        _testHandlingOfInvalidSpace(0, 3);
        _testHandlingOfInvalidSpace(0, 1);

        _testHandlingOfInvalidSpace(1, 99);
        _testHandlingOfInvalidSpace(2, 1);
    }

    private void _testHandlingOfInvalidSpace(int offset, int readSize) throws Exception
    {
        final String doc = "{ \u0008 \"a\":1}";

        AsyncReaderWrapper p = asyncForBytes(JSON_F, readSize, _jsonDoc(doc), offset);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Should have failed");
        } catch (JsonParseException e) {
            verifyException(e, "Illegal character");
            // and correct error code
            verifyException(e, "code 8");
        }
        p.close();
    }
}
