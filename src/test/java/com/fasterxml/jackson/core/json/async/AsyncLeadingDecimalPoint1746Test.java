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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1746]: leading-decimal-point numbers must be accepted
 * after {@code ,} and {@code :}, not just as the first value.
 */
class AsyncLeadingDecimalPoint1746Test extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    private final JsonFactory LEADING_DOT_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .build();

    private final JsonFactory LEADING_DOT_PLUS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .build();

    private final static int[] BYTES_PER_READ = { 1, 2, 3, 5, 100 };

    @Test
    void leadingDotAfterComma() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                for (String json : new String[] { "[1,.5]", "[1, .5]", "[1,\n.5]" }) {
                    AsyncReaderWrapper p = _parser(LEADING_DOT_F, json, bytesPerRead, bb);
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                    assertEquals(".5", p.currentText());
                    assertEquals(0.5, p.getDoubleValue());
                    assertToken(JsonToken.END_ARRAY, p.nextToken());
                    p.close();
                }
            }
        }
    }

    @Test
    void leadingDotAfterColon() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                for (String json : new String[] { "{\"a\":.5}", "{\"a\": .5}" }) {
                    AsyncReaderWrapper p = _parser(LEADING_DOT_F, json, bytesPerRead, bb);
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    assertToken(JsonToken.FIELD_NAME, p.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                    assertEquals(".5", p.currentText());
                    assertEquals(0.5, p.getDoubleValue());
                    assertToken(JsonToken.END_OBJECT, p.nextToken());
                    p.close();
                }
            }
        }
    }

    @Test
    void leadingDotMixed() throws Exception
    {
        final String json = "{\"a\":1,\"b\":.25e2,\"c\":[.5,.75]}";
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(LEADING_DOT_F, json, bytesPerRead, bb);
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(25.0, p.getDoubleValue());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(0.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(0.75, p.getDoubleValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
                p.close();
            }
        }
    }

    // Text must not depend on where chunk boundaries fall
    @Test
    void signedLeadingDot() throws Exception
    {
        _testSignedLeadingDot("[1,-.5]", "-.5", -0.5);
        _testSignedLeadingDot("[1, -.5]", "-.5", -0.5);
        _testSignedLeadingDot("[1,+.5]", ".5", 0.5);
        _testSignedLeadingDot("[-.25e2]", "-.25e2", -25.0);
    }

    @Test
    void signedLeadingDotAfterColon() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                for (String json : new String[] { "{\"a\":-.5}", "{\"a\": -.5}" }) {
                    AsyncReaderWrapper p = _parser(LEADING_DOT_F, json, bytesPerRead, bb);
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    assertToken(JsonToken.FIELD_NAME, p.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                    assertEquals("-.5", p.currentText());
                    assertEquals(-0.5, p.getDoubleValue());
                    assertToken(JsonToken.END_OBJECT, p.nextToken());
                    p.close();
                }
            }
        }
    }

    @Test
    void signedLeadingDotAtRoot() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(LEADING_DOT_PLUS_F, "-.5 +.75 ", bytesPerRead, bb);
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals("-.5", p.currentText());
                assertEquals(-0.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(".75", p.currentText());
                assertEquals(0.75, p.getDoubleValue());
                p.close();
            }
        }
    }

    @Test
    void plusLeadingDotWithoutPlusFeature() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(LEADING_DOT_F, "[1,+.5]", bytesPerRead, bb);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                try {
                    p.nextToken();
                    fail("Should not pass");
                } catch (StreamReadException e) {
                    verifyException(e, "JSON spec does not allow numbers to have plus signs");
                } finally {
                    p.close();
                }
            }
        }
    }

    @Test
    void signedLeadingDotDisabled() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(DEFAULT_F, "[1,-.5]", bytesPerRead, bb);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                try {
                    p.nextToken();
                    fail("Should not pass");
                } catch (StreamReadException e) {
                    verifyException(e, "Unexpected character ('.'");
                } finally {
                    p.close();
                }
            }
        }
    }

    private void _testSignedLeadingDot(String json, String expText, double expValue) throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(LEADING_DOT_PLUS_F, json, bytesPerRead, bb);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                if (json.startsWith("[1")) {
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                }
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(expText, p.currentText());
                assertEquals(expValue, p.getDoubleValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
                p.close();
            }
        }
    }

    @Test
    void leadingDotAfterCommaDisabled() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(DEFAULT_F, "[1,.5]", bytesPerRead, bb);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                try {
                    p.nextToken();
                    fail("Should not pass");
                } catch (StreamReadException e) {
                    verifyException(e, "Unexpected character ('.'");
                } finally {
                    p.close();
                }
            }
        }
    }

    @Test
    void leadingDotAfterColonDisabled() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(DEFAULT_F, "{\"a\":.5}", bytesPerRead, bb);
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                try {
                    p.nextToken();
                    fail("Should not pass");
                } catch (StreamReadException e) {
                    verifyException(e, "Unexpected character ('.'");
                } finally {
                    p.close();
                }
            }
        }
    }

    private AsyncReaderWrapper _parser(JsonFactory f, String json, int bytesPerRead, boolean bb)
        throws Exception
    {
        byte[] doc = json.getBytes(StandardCharsets.UTF_8);
        return bb ? asyncForByteBuffer(f, bytesPerRead, doc, 0)
                : asyncForBytes(f, bytesPerRead, doc, 0);
    }
}
