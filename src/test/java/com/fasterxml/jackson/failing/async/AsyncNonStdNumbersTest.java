package com.fasterxml.jackson.failing.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncNonStdNumbersTest extends AsyncTestBase
{
    public void testAllowNaN() throws Exception
    {
        final String JSON = "[ NaN]";
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS));

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(f, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "non-standard");
        } finally {
            p.close();
        }

        // we can enable it dynamically (impl detail)
        f.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        p = createParser(f, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        
        double d = p.getDoubleValue();
        assertTrue(Double.isNaN(d));
        assertEquals("NaN", p.currentText());

        // [Issue#98]
        try {
            /*BigDecimal dec =*/ p.getDecimalValue();
            fail("Should fail when trying to access NaN as BigDecimal");
        } catch (NumberFormatException e) {
            verifyException(e, "can not be represented as BigDecimal");
        }
       
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // finally, should also work with skipping
        f.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        p = createParser(f, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    public void testAllowInf() throws Exception
    {
        final String JSON = "[ -INF, +INF, +Infinity, Infinity, -Infinity ]";
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS));

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(f, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "Non-standard token '-INF'");
        } finally {
            p.close();
        }
        f.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        p = createParser(f, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        double d = p.getDoubleValue();
        assertEquals("-INF", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.NEGATIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("+INF", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.POSITIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("+Infinity", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.POSITIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("Infinity", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.POSITIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("-Infinity", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.NEGATIVE_INFINITY);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // finally, should also work with skipping
        f.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        p = createParser(f, JSON);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        
        p.close();
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc) throws IOException
    {
        return asyncForBytes(f, 1, _jsonDoc(doc), 1);
    }
}
