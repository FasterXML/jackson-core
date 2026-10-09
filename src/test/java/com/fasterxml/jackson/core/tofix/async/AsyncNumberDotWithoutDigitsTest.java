package com.fasterxml.jackson.core.tofix.async;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;
import com.fasterxml.jackson.core.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Failing tests: with both leading and trailing decimal point allowed,
 * a number with no digits at all (like {@code .}) is accepted.
 */
class AsyncNumberDotWithoutDigitsTest extends AsyncTestBase
{
    private final JsonFactory LEADING_TRAILING_DOT_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
            .build();

    private final static int[] BYTES_PER_READ = { 1, 2, 3, 5, 100 };

    // With both leading and trailing decimal point allowed, "." alone is
    // accepted as VALUE_NUMBER_FLOAT
    @JacksonTestFailureExpected
    @Test
    void dotWithoutDigits() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                for (String json : new String[] { "[1,.]", "[1,.e5]" }) {
                    AsyncReaderWrapper p = _parser(LEADING_TRAILING_DOT_F, json, bytesPerRead, bb);
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    try {
                        p.nextToken();
                        fail("Should not pass for: "+json);
                    } catch (StreamReadException e) {
                        verifyException(e, "Unexpected character");
                    } finally {
                        p.close();
                    }
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
