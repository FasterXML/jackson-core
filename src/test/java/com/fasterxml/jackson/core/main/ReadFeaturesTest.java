package com.fasterxml.jackson.core.main;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;

/**
 * Unit tests for verifying that additional <code>JsonParser.Feature</code>
 * and {@link JsonReadFeature}
 * settings work as expected.
 */
public class ReadFeaturesTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testStreamReadFeatureDefaults() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertTrue(f.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));

        JsonParser p = f.createParser(ObjectReadContext.empty(), new StringReader("{}"));
        _testDefaultSettings(p);
        p.close();
        p = f.createParser(ObjectReadContext.empty(), new ByteArrayInputStream("{}".getBytes("UTF-8")));
        _testDefaultSettings(p);
        p.close();
    }

    public void testJsonReadFeatureDefaultSettings() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        assertFalse(f.isEnabled(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS));
        assertFalse(f.isEnabled(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES));
        assertFalse(f.isEnabled(JsonReadFeature.ALLOW_SINGLE_QUOTES));
    }
    
    public void testQuotesRequired() throws Exception
    {
        _testQuotesRequired(false);
        _testQuotesRequired(true);
    }

    public void testTabsDefault() throws Exception
    {
        _testTabsDefault(false);
        _testTabsDefault(true);
    }

    public void testTabsEnabledBytes() throws Exception {
        _testTabsEnabled(true);
    }

    public void testTabsEnabledChars() throws Exception {
        _testTabsEnabled(false);
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
        JsonFactory f = JsonFactory.builder()
                .disable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS).build();
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
        JsonFactory f = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
                .build();
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
