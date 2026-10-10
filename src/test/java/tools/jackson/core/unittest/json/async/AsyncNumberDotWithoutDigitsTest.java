package tools.jackson.core.unittest.json.async;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonToken;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that a number must have at least one digit, even with both
 * leading and trailing decimal point allowed.
 */
class AsyncNumberDotWithoutDigitsTest extends AsyncTestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .build();

    private final static int[] BYTES_PER_READ = { 1, 2, 3, 5, 100 };

    @Test
    void dotWithoutDigitsInArray() throws Exception
    {
        for (String value : new String[] { ".", ".e5", "-.", "+.", "-.e5" }) {
            final String json = "[1," + value + "]";
            for (int bytesPerRead : BYTES_PER_READ) {
                for (boolean bb : new boolean[] { false, true }) {
                    AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    try {
                        p.nextToken();
                        fail("Should not pass for: " + json + " (" + bytesPerRead + " bytes/read)");
                    } catch (StreamReadException e) {
                        verifyException(e, "Decimal point not followed by a digit");
                    } finally {
                        p.close();
                    }
                }
            }
        }
    }

    @Test
    void dotWithoutDigitsAfterColon() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser("{\"a\":.}", bytesPerRead, bb);
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                try {
                    p.nextToken();
                    fail("Should not pass (" + bytesPerRead + " bytes/read)");
                } catch (StreamReadException e) {
                    verifyException(e, "Decimal point not followed by a digit");
                } finally {
                    p.close();
                }
            }
        }
    }

    // Root-level value: both with trailing space and ending at end-of-input
    @Test
    void dotWithoutDigitsAtRoot() throws Exception
    {
        for (String json : new String[] { ". ", ".", "-.", "+." }) {
            for (int bytesPerRead : BYTES_PER_READ) {
                for (boolean bb : new boolean[] { false, true }) {
                    AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                    try {
                        p.nextToken();
                        fail("Should not pass for: '" + json + "' (" + bytesPerRead + " bytes/read)");
                    } catch (StreamReadException e) {
                        verifyException(e, "Decimal point not followed by a digit");
                    } finally {
                        p.close();
                    }
                }
            }
        }
    }

    // Trailing decimal point with integer part, ending at end-of-input, still fine
    @Test
    void trailingDotAtRootEOF() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser("1.", bytesPerRead, bb);
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(1.0, p.getDoubleValue());
                p.close();
            }
        }
    }

    @Test
    void singleDigitAllowed() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser("[1.,.5,-.5,-1.,1.e2]", bytesPerRead, bb);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(1.0, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(0.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(-0.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(-1.0, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(100.0, p.getDoubleValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
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
