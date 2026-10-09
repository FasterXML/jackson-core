package tools.jackson.core.unittest.json.async;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonToken;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that a number starting with {@code 0} does not inherit the sign
 * of a preceding negative number.
 */
class AsyncLeadingZeroSignTest extends AsyncTestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
            .build();

    private final static int[] BYTES_PER_READ = { 1, 2, 3, 5, 100 };

    @Test
    void leadingZeroAfterNegative() throws Exception
    {
        _test("[-12,0012]", -12, 12);
        _test("[-1,05]", -1, 5);
        _test("[-1,0]", -1, 0);
        _test("[-123456789012,00123456789012]", -123456789012L, 123456789012L);
    }

    @Test
    void leadingZeroFloatAfterNegative() throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser("[-1,0.5,00.25]", bytesPerRead, bb);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(-1, p.getIntValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(0.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(0.25, p.getDoubleValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
                p.close();
            }
        }
    }

    private void _test(String json, long first, long second) throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean bb : new boolean[] { false, true }) {
                AsyncReaderWrapper p = _parser(json, bytesPerRead, bb);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(first, p.getLongValue());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(second, p.getLongValue(), "for " + json + ", " + bytesPerRead + " bytes/read");
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
