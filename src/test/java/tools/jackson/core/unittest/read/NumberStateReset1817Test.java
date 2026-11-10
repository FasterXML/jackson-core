package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [core#1817] (and related): per-number state (NaN, hex, deferred
 * decoding) of one number token must not leak into the next one, for
 * both blocking and non-blocking parsers.
 */
class NumberStateReset1817Test extends AsyncTestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_HEXADECIMAL_NUMBERS)
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();

    private final static String MIXED_DOC = "[0x1F, 12345, NaN, 0x10, 7, 1.5, 0x20, Infinity, 0]";

    @Test
    void mixedNumbersBlocking() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, MIXED_DOC)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                _verifyInt(p, p.nextToken(), 0x1F);
                _verifyInt(p, p.nextToken(), 12345);
                _verifyNaN(p, p.nextToken(), Double.NaN);
                _verifyInt(p, p.nextToken(), 0x10);
                _verifyInt(p, p.nextToken(), 7);
                _verifyDouble(p, p.nextToken(), 1.5);
                _verifyInt(p, p.nextToken(), 0x20);
                _verifyNaN(p, p.nextToken(), Double.POSITIVE_INFINITY);
                _verifyInt(p, p.nextToken(), 0);
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    @Test
    void mixedNumbersAsync() throws Exception
    {
        for (int readSize : new int[] { 1, 2, 3, 1000 }) {
            for (boolean bb : new boolean[] { false, true }) {
                try (AsyncReaderWrapper r = _asyncParser(MIXED_DOC, readSize, bb)) {
                    final JsonParser p = r.parser();
                    assertToken(JsonToken.START_ARRAY, r.nextToken());
                    _verifyInt(p, r.nextToken(), 0x1F);
                    _verifyInt(p, r.nextToken(), 12345);
                    _verifyNaN(p, r.nextToken(), Double.NaN);
                    _verifyInt(p, r.nextToken(), 0x10);
                    _verifyInt(p, r.nextToken(), 7);
                    _verifyDouble(p, r.nextToken(), 1.5);
                    _verifyInt(p, r.nextToken(), 0x20);
                    _verifyNaN(p, r.nextToken(), Double.POSITIVE_INFINITY);
                    _verifyInt(p, r.nextToken(), 0);
                    assertToken(JsonToken.END_ARRAY, r.nextToken());
                }
            }
        }
    }

    // Deferred decoding state of a floating-point value must not leak into
    // following hex value; root-level so that last hex value ends at end-of-input
    private final static String DEFERRED_DOC = "1.25 0x10 2.5 0x20";

    @Test
    void deferredFloatThenHexBlocking() throws Exception
    {
        for (int mode : ALL_MODES) {
            // trailing space needed for DataInput (cannot detect end of root-level number)
            try (JsonParser p = createParser(F, mode, DEFERRED_DOC + " ")) {
                _verifyDeferred(p, p.nextToken(), "1.25");
                _verifyInt(p, p.nextToken(), 0x10);
                _verifyDeferred(p, p.nextToken(), "2.5");
                _verifyInt(p, p.nextToken(), 0x20);
                assertNull(p.nextToken());
            }
        }
    }

    @Test
    void deferredFloatThenHexAsync() throws Exception
    {
        for (int readSize : new int[] { 1, 2, 3, 1000 }) {
            for (boolean bb : new boolean[] { false, true }) {
                try (AsyncReaderWrapper r = _asyncParser(DEFERRED_DOC, readSize, bb)) {
                    final JsonParser p = r.parser();
                    _verifyDeferred(p, r.nextToken(), "1.25");
                    _verifyInt(p, r.nextToken(), 0x10);
                    _verifyDeferred(p, r.nextToken(), "2.5");
                    _verifyInt(p, r.nextToken(), 0x20);
                    assertNull(r.nextToken());
                }
            }
        }
    }

    private AsyncReaderWrapper _asyncParser(String doc, int readSize, boolean byteBuffer) {
        return byteBuffer
                ? asyncForByteBuffer(F, readSize, _jsonDoc(doc), 0)
                : asyncForBytes(F, readSize, _jsonDoc(doc), 0);
    }

    // Leaves decoding of `double` deferred
    private void _verifyDeferred(JsonParser p, JsonToken t, String exp) {
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, t);
        assertEquals(exp, String.valueOf(p.getNumberValueDeferred()));
    }

    private void _verifyInt(JsonParser p, JsonToken t, int exp) {
        assertToken(JsonToken.VALUE_NUMBER_INT, t);
        assertFalse(p.isNaN());
        assertEquals(exp, p.getIntValue());
        assertEquals((double) exp, p.getDoubleValue());
    }

    private void _verifyDouble(JsonParser p, JsonToken t, double exp) {
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, t);
        assertFalse(p.isNaN());
        assertEquals(exp, p.getDoubleValue());
    }

    private void _verifyNaN(JsonParser p, JsonToken t, double exp) {
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, t);
        assertTrue(p.isNaN());
        assertEquals(exp, p.getDoubleValue());
    }
}
