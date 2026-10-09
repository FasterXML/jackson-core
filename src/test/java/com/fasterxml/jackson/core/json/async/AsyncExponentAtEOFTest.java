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
 * Tests that a root-level number ending at end-of-input right after
 * exponent sign is rejected.
 */
class AsyncExponentAtEOFTest extends AsyncTestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .build();

    private final static int[] BYTES_PER_READ = { 1, 2, 3, 5, 100 };

    @Test
    void exponentSignWithoutDigits() throws Exception
    {
        for (String json : new String[] { "1e-", "1E+", "1.5e+", "-.5e-", "-0e-" }) {
            for (int bytesPerRead : BYTES_PER_READ) {
                for (boolean bb : new boolean[] { false, true }) {
                    AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                    try {
                        p.nextToken();
                        fail("Should not pass for: '" + json + "' (" + bytesPerRead + " bytes/read)");
                    } catch (StreamReadException e) {
                        verifyException(e, "Exponent indicator not followed by a digit");
                    } finally {
                        p.close();
                    }
                }
            }
        }
    }

    @Test
    void validNumbersAtEOF() throws Exception
    {
        _testValid("1e5", 1e5);
        _testValid("1e-5", 1e-5);
        _testValid("1.5", 1.5);
        _testValid("-.5e+2", -50.0);
    }

    private void _testValid(String json, double exp) throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(exp, p.getDoubleValue());
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
