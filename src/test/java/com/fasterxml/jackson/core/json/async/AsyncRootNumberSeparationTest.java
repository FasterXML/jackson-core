package com.fasterxml.jackson.core.json.async;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that a root-level number must be followed by white space (or end-of-input),
 * as with blocking parsers, and that malformed numbers are reported
 * regardless of where input chunk boundaries fall
 * (see [core#1506] for the original root-level issue).
 */
class AsyncRootNumberSeparationTest extends AsyncTestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
            .build();

    private final static int[] BYTES_PER_READ = { 1, 2, 3, 5, 100 };

    // Must fail on the very first token: no number returned before the error
    @Test
    void missingSeparatorAfterRootNumber() throws Exception
    {
        for (String json : new String[] {
                "12-3 ", "1.5-2 ", "1e5-1 ", "0-1 ", "-0-1 ", "-.5-.5 ",
                "1[2] ", "1{} ", "1\"a\" ", "1true ", "1.5true ", "1.5false ", "1e5.5 ",
                "0.5x ", "1,2 ", "1]", "1}"
        }) {
            _testFailsOnFirstToken(json, "Expected space separating root-level values");
        }
    }

    // Non-ASCII character reported decoded, if fully buffered; lead byte if not
    @Test
    void nonAsciiAfterRootNumber() throws Exception
    {
        _testFailsOnFirstToken("1\u00e9 ", "Expected space separating root-level values");
        AsyncReaderWrapper p = _parser("1\u00e9 ", 100, false);
        try {
            p.nextToken();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "('\u00e9' (code 233))");
        } finally {
            p.close();
        }
        p = _parser("1.5\u20ac ", 100, true);
        try {
            p.nextToken();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "(code 8364 / 0x20ac)");
        } finally {
            p.close();
        }
    }

    @Test
    void secondDecimalPointAtRoot() throws Exception
    {
        _testFailsOnFirstToken("1.5.5 ", "more than one decimal point");
    }

    // Inside Arrays/Objects, malformed number must also fail on the number itself
    @Test
    void malformedFloatInArray() throws Exception
    {
        _testFailsOnSecondToken("[1.5.5]", "more than one decimal point");
        _testFailsOnSecondToken("[1.5f]", "'f' or 'd' suffixes");
        _testFailsOnSecondToken("[1.5D]", "'f' or 'd' suffixes");
    }

    // Non-ASCII byte ending fraction reported same regardless of chunking
    @Test
    void nonAsciiAfterDecimalPointInArray() throws Exception
    {
        _testFailsOnSecondToken("[1.\u00e9]", "(code 195)");
    }

    @Test
    void validRootSeparators() throws Exception
    {
        for (String json : new String[] { "1 2", "1\n2", "1\t2", "1\r2", "1\r\n2", "1 2 " }) {
            for (int bytesPerRead : BYTES_PER_READ) {
                for (boolean bb : new boolean[] { false, true }) {
                    AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertEquals(1, p.getIntValue());
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertEquals(2, p.getIntValue());
                    assertNull(p.nextToken());
                    p.close();
                }
            }
        }
    }

    // RS (JSON Text Sequences) accepted after root-level number, if enabled
    @Test
    void recordSeparatorAfterRootNumber() throws Exception
    {
        final JsonFactory rsF = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_RS_CONTROL_CHAR)
                .build();
        for (String json : new String[] { "1\u001E2", "1.5\u001E2", "1e2\u001E2", "0\u001E2", "-0\u001E2" }) {
            for (int bytesPerRead : BYTES_PER_READ) {
                for (boolean bb : new boolean[] { false, true }) {
                    byte[] doc = json.getBytes(StandardCharsets.UTF_8);
                    AsyncReaderWrapper p = bb ? asyncForByteBuffer(rsF, bytesPerRead, doc, 0)
                            : asyncForBytes(rsF, bytesPerRead, doc, 0);
                    assertTrue(p.nextToken().isNumeric());
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertEquals(2, p.getIntValue());
                    assertNull(p.nextToken());
                    p.close();
                }
            }
        }
        // but not without feature enabled
        _testFailsOnFirstToken("1\u001E2", "Expected space separating root-level values");
    }

    @Test
    void validRootFloats() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser("1.5 .5\n-.25e1 0", bytesPerRead, bb);
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(1.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(0.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(-2.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(0, p.getIntValue());
                assertNull(p.nextToken());
                p.close();
            }
        }
    }

    private void _testFailsOnFirstToken(String json, String expMsg) throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                try {
                    JsonToken t = p.nextToken();
                    fail("Should not pass for '" + json + "' (" + bytesPerRead
                            + " bytes/read); got token " + t + " ('" + p.currentText() + "')");
                } catch (StreamReadException e) {
                    verifyException(e, expMsg);
                } finally {
                    p.close();
                }
            }
        }
    }

    private void _testFailsOnSecondToken(String json, String expMsg) throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                try {
                    JsonToken t = p.nextToken();
                    fail("Should not pass for '" + json + "' (" + bytesPerRead
                            + " bytes/read); got token " + t + " ('" + p.currentText() + "')");
                } catch (StreamReadException e) {
                    verifyException(e, expMsg);
                } finally {
                    p.close();
                }
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
