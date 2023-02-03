package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncNonStdNumberHandlingTest extends AsyncTestBase
{
    @SuppressWarnings("deprecation")
    public void testDefaultsForAsync() throws Exception {
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS));
    }

    public void testLeadingZeroesInt() throws Exception
    {
        _testLeadingZeroesInt("00003", 3);
        _testLeadingZeroesInt("00003 ", 3);
        _testLeadingZeroesInt(" 00003", 3);

        _testLeadingZeroesInt("-00007", -7);
        _testLeadingZeroesInt("-00007 ", -7);
        _testLeadingZeroesInt(" -00007", -7);

        _testLeadingZeroesInt("056", 56);
        _testLeadingZeroesInt("056 ", 56);
        _testLeadingZeroesInt(" 056", 56);

        _testLeadingZeroesInt("-04", -4);
        _testLeadingZeroesInt("-04  ", -4);
        _testLeadingZeroesInt(" -04", -4);

        _testLeadingZeroesInt("0"+Integer.MAX_VALUE, Integer.MAX_VALUE);
        _testLeadingZeroesInt(" 0"+Integer.MAX_VALUE, Integer.MAX_VALUE);
        _testLeadingZeroesInt("0"+Integer.MAX_VALUE+" ", Integer.MAX_VALUE);
    }

    public void _testLeadingZeroesInt(String valueStr, int value) throws Exception
    {
        // first: verify that we get an exception

        JsonFactory f = new JsonFactory();
        String JSON = valueStr;
        AsyncReaderWrapper p = createParser(f, JSON);
        try {
            p.nextToken();
            p.currentText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "invalid numeric value");
        } finally {
            p.close();
        }

        // and then verify it's ok when enabled
        f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                .build();
        p = createParser(f, JSON);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(value, p.getIntValue());
        assertEquals(String.valueOf(value), p.currentText());
        p.close();
    }

    public void testLeadingZeroesFloat() throws Exception
    {
        _testLeadingZeroesFloat("00.25", 0.25);
        _testLeadingZeroesFloat("  00.25", 0.25);
        _testLeadingZeroesFloat("00.25  ", 0.25);

        _testLeadingZeroesFloat("-000.5", -0.5);
        _testLeadingZeroesFloat("  -000.5", -0.5);
        _testLeadingZeroesFloat("-000.5  ", -0.5);
    }

    private void _testLeadingZeroesFloat(String valueStr, double value) throws Exception
    {
        // first: verify that we get an exception
        JsonFactory f = new JsonFactory();
        String JSON = valueStr;
        AsyncReaderWrapper p = createParser(f, JSON);
        try {
            p.nextToken();
            p.currentText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "invalid numeric value");
        } finally {
            p.close();
        }

        // and then verify it's ok when enabled
        f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                .build();
        p = createParser(f, JSON);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(String.valueOf(value), p.currentText());
        assertEquals(value, p.getDoubleValue());
        p.close();
    }

    public void testLeadingPeriodFloat() throws Exception
    {
        _testLeadingPeriodFloat(".25", 0.25, 1);
        _testLeadingPeriodFloat(".25", 0.25, 100);
        _testLeadingPeriodFloat(" .25 ", 0.25, 1);
        _testLeadingPeriodFloat(" .25 ", 0.25, 100);

        _testLeadingPeriodFloat(".1", 0.1, 1);
        _testLeadingPeriodFloat(".1", 0.1, 100);

        _testLeadingPeriodFloat(".6125 ", 0.6125, 1);
        _testLeadingPeriodFloat(".6125 ", 0.6125, 100);
    }

    private void _testLeadingPeriodFloat(String valueStr, double value, int bytesPerRead)
        throws Exception
    {
        // first: verify that we get an exception

        JsonFactory f = new JsonFactory();
        String JSON = valueStr;
        AsyncReaderWrapper p = createParser(f, JSON, bytesPerRead);
        try {
            p.nextToken();
            p.currentText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('.'");
            verifyException(e, "expected a valid value");
        } finally {
            p.close();
        }

        // and then verify it's ok when enabled
        f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .build();
        p = createParser(f, JSON, bytesPerRead);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(value, p.getDoubleValue());
        assertEquals(valueStr.trim(), p.currentText());
        p.close();
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc) throws IOException
    {
        return createParser(f, doc, 1);
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc, int bytesPerRead)
            throws IOException
    {
        return asyncForBytes(f, bytesPerRead, _jsonDoc(doc), 1);
    }
}
