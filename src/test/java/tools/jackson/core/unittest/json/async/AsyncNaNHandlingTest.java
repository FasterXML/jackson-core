package tools.jackson.core.unittest.json.async;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

class AsyncNaNHandlingTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    @Test
    void defaultsForAsync() throws Exception {
        assertFalse(DEFAULT_F.isEnabled(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS));
    }

    @Test
    void disallowNaN() throws Exception
    {
        final String JSON = "[ NaN]";

        // without enabling, should get an exception
        for (boolean bb : new boolean[] { false, true }) {
            AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1, bb);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                p.nextToken();
                fail("Expected exception");
            } catch (Exception e) {
                verifyException(e, "non-standard");
            } finally {
                p.close();
            }
        }
    }

    @Test
    void allowNaN() throws Exception
    {
        final String JSON = "[ NaN]";
        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        for (boolean bb : new boolean[] { false, true }) {
            _testAllowNaN(f, JSON, 99, bb);
            _testAllowNaN(f, JSON, 5, bb);
            _testAllowNaN(f, JSON, 3, bb);
            _testAllowNaN(f, JSON, 2, bb);
            _testAllowNaN(f, JSON, 1, bb);
        }
    }

    private void _testAllowNaN(JsonFactory f, String doc, int readBytes, boolean byteBuffer) throws Exception
    {
        AsyncReaderWrapper p = createParser(f, doc, readBytes, byteBuffer);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

        double d = p.getDoubleValue();
        assertTrue(Double.isNaN(d));
        assertTrue(p.parser().isNaN());
        assertEquals("NaN", p.currentText());

        try {
            /*BigDecimal dec =*/ p.getDecimalValue();
            fail("Should fail when trying to access NaN as BigDecimal");
        } catch (NumberFormatException e) {
            verifyException(e, "cannot be deserialized as `java.math.BigDecimal`");
        }

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // finally, should also work with skipping
        f = f.rebuild().enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        p = createParser(f, doc, readBytes, byteBuffer);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertTrue(p.parser().isNaN());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    // [core#1817]: non-blocking parsers must report `isNaN()` like the
    // blocking ones, and a regular number following a non-standard one
    // must not inherit the flag.
    // Tested for both byte-array and ByteBuffer parsers with multiple chunk sizes.
    @Test
    void nonStdNumbersReportIsNaN() throws Exception {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        for (boolean bb : new boolean[] { false, true }) {
            for (int readBytes : new int[] { 99, 5, 3, 2, 1 }) {
                _testNonStdIsNaN(f, readBytes, bb);
                _testNonStdIsNaNRoot(f, readBytes, bb);
            }
        }
    }

    private void _testNonStdIsNaN(JsonFactory f, int readBytes, boolean byteBuffer) throws Exception {
        final String JSON = "[NaN, 1.5, Infinity, 2, -Infinity, 0, +Infinity, 10.5, 25]";
        AsyncReaderWrapper p = createParser(f, JSON, readBytes, byteBuffer);
        assertFalse(p.parser().isNaN());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertFalse(p.parser().isNaN());

        // NaN -> true
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("NaN", p.currentText());
        assertTrue(Double.isNaN(p.getDoubleValue()));
        assertTrue(p.parser().isNaN());

        // 1.5 -> false
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("1.5", p.currentText());
        assertEquals(1.5, p.getDoubleValue());
        assertFalse(p.parser().isNaN());

        // Infinity -> true
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("Infinity", p.currentText());
        assertEquals(Double.POSITIVE_INFINITY, p.getDoubleValue());
        assertTrue(p.parser().isNaN());

        // 2 -> false
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());
        assertFalse(p.parser().isNaN());

        // -Infinity -> true
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("-Infinity", p.currentText());
        assertEquals(Double.NEGATIVE_INFINITY, p.getDoubleValue());
        assertTrue(p.parser().isNaN());

        // 0 -> false
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(0, p.getIntValue());
        assertFalse(p.parser().isNaN());

        // +Infinity -> true
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("+Infinity", p.currentText());
        assertEquals(Double.POSITIVE_INFINITY, p.getDoubleValue());
        assertTrue(p.parser().isNaN());

        // 10.5 -> false
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("10.5", p.currentText());
        assertEquals(10.5, p.getDoubleValue());
        assertFalse(p.parser().isNaN());

        // 25 -> false
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(25, p.getIntValue());
        assertFalse(p.parser().isNaN());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertFalse(p.parser().isNaN());

        p.close();
    }

    private void _testNonStdIsNaNRoot(JsonFactory f, int readBytes, boolean byteBuffer) throws Exception {
        final String JSON = "NaN 1.5 Infinity 2 -Infinity 0 +Infinity 10.5 25";
        AsyncReaderWrapper p = createParser(f, JSON, readBytes, byteBuffer);
        assertFalse(p.parser().isNaN());

        // NaN -> true
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertTrue(p.parser().isNaN());

        // 1.5 -> false
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertFalse(p.parser().isNaN());

        // Infinity -> true
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertTrue(p.parser().isNaN());

        // 2 -> false
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertFalse(p.parser().isNaN());

        // -Infinity -> true
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertTrue(p.parser().isNaN());

        // 0 -> false
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertFalse(p.parser().isNaN());

        // +Infinity -> true
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertTrue(p.parser().isNaN());

        // 10.5 -> false
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertFalse(p.parser().isNaN());

        // 25 -> false
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertFalse(p.parser().isNaN());

        assertNull(p.nextToken());
        assertFalse(p.parser().isNaN());
        p.close();
    }

    @Test
    void disallowInf() throws Exception
    {
        for (boolean bb : new boolean[] { false, true }) {
            // these are serializations of JDK itself:
            _testDisallowInf(DEFAULT_F, "Infinity", 99, bb);
            _testDisallowInf(DEFAULT_F, "Infinity", 1, bb);
            _testDisallowInf(DEFAULT_F, "-Infinity", 99, bb);
            _testDisallowInf(DEFAULT_F, "-Infinity", 1, bb);
            // and this is sort of alias for first one
            _testDisallowInf(DEFAULT_F, "+Infinity", 99, bb);
            _testDisallowInf(DEFAULT_F, "+Infinity", 1, bb);
        }
    }

    private void _testDisallowInf(JsonFactory f, String token, int readBytes, boolean byteBuffer) throws Exception
    {
        final String JSON = String.format("[%s]", token);

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(f, JSON, readBytes, byteBuffer);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            JsonToken t = p.nextToken();
            fail("Expected exception; got "+t+" (text ["+p.currentText()+"])");
        } catch (Exception e) {
            verifyException(e, "Non-standard token '"+token+"'");
        } finally {
            p.close();
        }
    }

    @Test
    void allowInf() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        for (boolean bb : new boolean[] { false, true }) {
            String JSON = "[ Infinity, +Infinity, -Infinity ]";
            _testAllowInf(f, JSON, 99, bb);
            _testAllowInf(f, JSON, 5, bb);
            _testAllowInf(f, JSON, 3, bb);
            _testAllowInf(f, JSON, 2, bb);
            _testAllowInf(f, JSON, 1, bb);

            JSON = "[Infinity,+Infinity,-Infinity]";
            _testAllowInf(f, JSON, 99, bb);
            _testAllowInf(f, JSON, 1, bb);

            JSON = "[Infinity  ,   +Infinity   ,   -Infinity]";
            _testAllowInf(f, JSON, 99, bb);
            _testAllowInf(f, JSON, 1, bb);
        }
    }

    private void _testAllowInf(JsonFactory f, String doc, int readBytes, boolean byteBuffer) throws Exception
    {
        // 06-Jun-2017, tatu: Leave out "-INF" and "+INF" for now due to overlap with
        //   somewhat more standard (wrt JDK) "-Infinity" and "+Infinity"

        AsyncReaderWrapper p = createParser(f, doc, readBytes, byteBuffer);
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        double d;

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("Infinity", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertEquals(Double.POSITIVE_INFINITY, d);
        assertTrue(p.parser().isNaN());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("+Infinity", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertEquals(Double.POSITIVE_INFINITY, d);
        assertTrue(p.parser().isNaN());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("-Infinity", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertEquals(Double.NEGATIVE_INFINITY, d);
        assertTrue(p.parser().isNaN());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // finally, should also work with skipping
        f = f.rebuild().enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        p = createParser(f, doc, readBytes, byteBuffer);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertTrue(p.parser().isNaN());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertTrue(p.parser().isNaN());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertTrue(p.parser().isNaN());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        p.close();
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc, int readBytes) throws IOException
    {
        return createParser(f, doc, readBytes, false);
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc, int readBytes, boolean byteBuffer) throws IOException
    {
        byte[] bytes = _jsonDoc(doc);
        return byteBuffer ? asyncForByteBuffer(f, readBytes, bytes, 1)
                : asyncForBytes(f, readBytes, bytes, 1);
    }
}
