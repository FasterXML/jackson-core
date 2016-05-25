package com.fasterxml.jackson.core.main;

import java.io.*;

import com.fasterxml.jackson.core.*;

/**
 * Unit tests for verifying that additional <code>JsonParser.Feature</code>
 * settings work as expected.
 */
public class TestParserFeatures
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testDefaultSettings() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertTrue(f.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES));
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_SINGLE_QUOTES));
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS));

        JsonParser p = f.createParser(new StringReader("{}"));
        _testDefaultSettings(p);
        p.close();
        p = f.createParser(new ByteArrayInputStream("{}".getBytes("UTF-8")));
        _testDefaultSettings(p);
        p.close();
    }

    public void testQuotesRequired() throws Exception
    {
        _testQuotesRequired(false);
        _testQuotesRequired(true);
    }

    // // Tests for [JACKSON-208], unquoted tabs:

    public void testTabsDefault() throws Exception
    {
        _testTabsDefault(false);
        _testTabsDefault(true);
    }

    public void testTabsEnabled() throws Exception
    {
        _testTabsEnabled(false);
        _testTabsEnabled(true);
    }
    
    /*
    /****************************************************************
    /* Secondary test methods
    /****************************************************************
     */

    private void _testDefaultSettings(JsonParser p) {
        assertFalse(p.canReadObjectId());
        assertFalse(p.canReadTypeId());
    }
    
    private void _testQuotesRequired(boolean useStream) throws Exception
    {
        final String JSON = "{ test : 3 }";
        final String EXP_ERROR_FRAGMENT = "was expecting double-quote to start";
        JsonFactory f = new JsonFactory();
        JsonParser p = useStream ?
            createParserUsingStream(f, JSON, "UTF-8")
            : createParserUsingReader(f, JSON)
            ;

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
        } catch (JsonParseException je) {
            verifyException(je, EXP_ERROR_FRAGMENT);
        } finally {
            p.close();
        }
    }

    // // // Tests for [JACKSON-208]

    private void _testTabsDefault(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory();
        // First, let's see that by default unquoted tabs are illegal
        String JSON = "[\"tab:\t\"]";
        JsonParser p = useStream ? createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            p.getText();
            fail("Expected exception");
        } catch (JsonParseException e) {
            verifyException(e, "Illegal unquoted character");
        } finally {
            p.close();
        }
    }

    private void _testTabsEnabled(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

        String FIELD = "a\tb";
        String VALUE = "\t";
        String JSON = "{ "+quote(FIELD)+" : "+quote(VALUE)+"}";
        JsonParser p = useStream ? createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(FIELD, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(VALUE, p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }
}
