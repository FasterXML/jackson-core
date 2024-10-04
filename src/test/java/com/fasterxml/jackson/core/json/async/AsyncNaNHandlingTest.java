package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

class AsyncNaNHandlingTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    @SuppressWarnings("deprecation")
    @Test
    void defaultsForAsync() throws Exception {
        assertFalse(DEFAULT_F.isEnabled(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS));
    }

    @Test
    void disallowNaN() throws Exception
    {
        final String JSON = "[ NaN]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
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

    @Test
    void allowNaN() throws Exception
    {
        final String JSON = "[ NaN]";
        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        _testAllowNaN(f, JSON, 99);
        _testAllowNaN(f, JSON, 5);
        _testAllowNaN(f, JSON, 3);
        _testAllowNaN(f, JSON, 2);
        _testAllowNaN(f, JSON, 1);
    }

    private void _testAllowNaN(JsonFactory f, String doc, int readBytes) throws Exception
    {
        AsyncReaderWrapper p = createParser(f, doc, readBytes);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

        double d = p.getDoubleValue();
        assertTrue(Double.isNaN(d));
        assertEquals("NaN", p.currentText());

        try {
            /*BigDecimal dec =*/ p.getDecimalValue();
            fail("Should fail when trying to access NaN as BigDecimal");
        } catch (NumberFormatException e) {
            verifyException(e, "can not be deserialized as `java.math.BigDecimal`");
        }

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // finally, should also work with skipping
        f = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS, true)
                .build();
        p = createParser(f, doc, readBytes);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    @Test
    void disallowInf() throws Exception
    {
        // these are serializations of JDK itself:
        _testDisallowInf(DEFAULT_F, "Infinity", 99);
        _testDisallowInf(DEFAULT_F, "Infinity", 1);
        _testDisallowInf(DEFAULT_F, "-Infinity", 99);
        _testDisallowInf(DEFAULT_F, "-Infinity", 1);
        // and this is sort of alias for first one
        _testDisallowInf(DEFAULT_F, "+Infinity", 99);
        _testDisallowInf(DEFAULT_F, "+Infinity", 1);

        // And these may or may not be supported as further aliases

        // 06-Jun-2017, tatu: Problematic for now since they share same prefix; can
        //   be supported, eventually, if really care. For now leave it be.
//        _testDisallowInf(DEFAULT_F, "-INF");
//        _testDisallowInf(DEFAULT_F, "+INF");
    }

    private void _testDisallowInf(JsonFactory f, String token, int readBytes) throws Exception
    {
        final String JSON = String.format("[%s]", token);

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(f, JSON, readBytes);
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
        String JSON = "[ Infinity, +Infinity, -Infinity ]";
        _testAllowInf(f, JSON, 99);
        _testAllowInf(f, JSON, 5);
        _testAllowInf(f, JSON, 3);
        _testAllowInf(f, JSON, 2);
        _testAllowInf(f, JSON, 1);

        JSON = "[Infinity,+Infinity,-Infinity]";
        _testAllowInf(f, JSON, 99);
        _testAllowInf(f, JSON, 1);

        JSON = "[Infinity  ,   +Infinity   ,   -Infinity]";
        _testAllowInf(f, JSON, 99);
        _testAllowInf(f, JSON, 1);
    }

    private void _testAllowInf(JsonFactory f, String doc, int readBytes) throws Exception
    {
        // 06-Jun-2017, tatu: Leave out "-INF" and "+INF" for now due to overlap with
        //   somewhat more standard (wrt JDK) "-Infinity" and "+Infinity"

        AsyncReaderWrapper p = createParser(f, doc, readBytes);
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        double d;

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("Infinity", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertEquals(Double.POSITIVE_INFINITY, d);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("+Infinity", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertEquals(Double.POSITIVE_INFINITY, d);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("-Infinity", p.currentText());
        assertTrue(Double.isInfinite(d));
        assertEquals(Double.NEGATIVE_INFINITY, d);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // finally, should also work with skipping
        f = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS, true)
                .build();
        p = createParser(f, doc, readBytes);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        p.close();
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc, int readBytes) throws IOException
    {
        return asyncForBytes(f, readBytes, _jsonDoc(doc), 1);
    }
}
