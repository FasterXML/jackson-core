package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncNumberLeadingZeroesTest extends AsyncTestBase
{
    public void testDefaultsForAsync() throws Exception {
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS));
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
        f = f.rebuild().enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS).build();
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

    public void _testLeadingZeroesFloat(String valueStr, double value) throws Exception
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
        f = f.rebuild().enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS).build();
        p = createParser(f, JSON);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(String.valueOf(value), p.currentText());
        assertEquals(value, p.getDoubleValue());
        p.close();
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc) throws IOException
    {
        return asyncForBytes(f, 1, _jsonDoc(doc), 1);
    }
}
