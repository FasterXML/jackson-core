package com.fasterxml.jackson.core.json.async;

import java.io.ByteArrayOutputStream;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.core.async.AsyncTestBase;

import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

// Tests for verifying things such as handling of invalid control characters;
// decoding of UTF-8 BOM.
public class AsyncInvalidCharsTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testUtf8BOMHandling() throws Exception
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

        JsonLocation loc = p.parser().getTokenLocation();
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

        AsyncReaderWrapper p = asyncForBytes(JSON_F, readSize, doc, offset);

        try {
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, verify);
        }
    }

    public void testHandlingOfInvalidSpace() throws Exception
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
