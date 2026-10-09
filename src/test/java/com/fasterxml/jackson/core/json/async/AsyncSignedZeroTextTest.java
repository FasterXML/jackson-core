package com.fasterxml.jackson.core.json.async;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the non-blocking parser retains the sign of numbers starting
 * with {@code +0} or {@code -0} in token text, regardless of chunking.
 */
class AsyncSignedZeroTextTest extends AsyncTestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
            .build();

    private final static int[] BYTES_PER_READ = { 1, 2, 3, 5, 100 };

    @Test
    void signedZeroInArray() throws Exception
    {
        _testInArray("+0", JsonToken.VALUE_NUMBER_INT, "+0", 0.0);
        _testInArray("-0", JsonToken.VALUE_NUMBER_INT, "-0", 0.0);
        _testInArray("+00", JsonToken.VALUE_NUMBER_INT, "+0", 0.0);
        _testInArray("-00", JsonToken.VALUE_NUMBER_INT, "-0", 0.0);
        _testInArray("+05", JsonToken.VALUE_NUMBER_INT, "+5", 5.0);
        _testInArray("+0.5", JsonToken.VALUE_NUMBER_FLOAT, "+0.5", 0.5);
        _testInArray("-0.5", JsonToken.VALUE_NUMBER_FLOAT, "-0.5", -0.5);
        _testInArray("+0e2", JsonToken.VALUE_NUMBER_FLOAT, "+0e2", 0.0);
    }

    // Root-level value ending at end-of-input
    @Test
    void signedZeroAtRootEOF() throws Exception
    {
        _testAtRoot("+0", "+0", 0);
        _testAtRoot("-0", "-0", 0);
        _testAtRoot("0", "0", 0);
    }

    private void _testInArray(String value, JsonToken expToken, String expText,
            double expValue) throws Exception
    {
        final String json = "[" + value + "]";
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(expToken, p.nextToken());
                assertEquals(expText, p.currentText(), "for " + json + ", " + bytesPerRead + " bytes/read");
                assertEquals(expValue, p.getDoubleValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
                p.close();
            }
        }
    }

    private void _testAtRoot(String json, String expText, int expValue) throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(expText, p.currentText(), "for " + json + ", " + bytesPerRead + " bytes/read");
                assertEquals(expValue, p.getIntValue());
                p.close();
            }
        }
    }

    private AsyncReaderWrapper _parser(String json, int bytesPerRead, boolean bb)
        throws Exception
    {
        byte[] doc = json.getBytes(StandardCharsets.UTF_8);
        return bb ? asyncForByteBuffer(F, bytesPerRead, doc, 0)
                : asyncForBytes(F, bytesPerRead, doc, 0);
    }
}
