package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying that additional <code>JsonParser.Feature</code>
 * settings work as expected.
 */
class ParserFeaturesTest
        extends JUnit5TestBase
{
    private final JsonFactory JSON_F = sharedStreamFactory();

    @Test
    void defaultSettings() throws Exception
    {
        assertTrue(JSON_F.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));

        _testDefaultSettings(createParser(JSON_F, MODE_INPUT_STREAM, "{}"));
        _testDefaultSettings(createParser(JSON_F, MODE_READER, "{}"));
        _testDefaultSettings(createParser(JSON_F, MODE_DATA_INPUT, "{}"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void deprecatedDefaultSettings() throws Exception
    {
        assertFalse(JSON_F.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertFalse(JSON_F.isEnabled(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS));
        assertFalse(JSON_F.isEnabled(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES));
        assertFalse(JSON_F.isEnabled(JsonParser.Feature.ALLOW_SINGLE_QUOTES));
    }

    @Test
    void quotesRequired() throws Exception
    {
        _testQuotesRequired(false);
        _testQuotesRequired(true);
    }

    @Test
    void tabsDefault() throws Exception
    {
        _testTabsDefault(false);
        _testTabsDefault(true);
    }

    @Test
    void tabsEnabled() throws Exception
    {
        _testTabsEnabled(false);
        _testTabsEnabled(true);
    }

    @Test
    void recordSeparatorDefault() throws Exception
    {
        _testRecordSeparatorDefault(false);
        _testRecordSeparatorDefault(true);
    }

    @Test
    void recordSeparatorEnabled() throws Exception
    {
        _testRecordSeparatorEnabled(false);
        _testRecordSeparatorEnabled(true);
    }

    /*
    /****************************************************************
    /* Secondary test methods
    /****************************************************************
     */

    private void _testDefaultSettings(JsonParser p) throws Exception {
        assertFalse(p.canReadObjectId());
        assertFalse(p.canReadTypeId());

        // [core#619]:
        assertFalse(p.getReadCapabilities().isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES));
        assertFalse(p.getReadCapabilities().isEnabled(StreamReadCapability.SCALARS_AS_OBJECTS));
        assertFalse(p.getReadCapabilities().isEnabled(StreamReadCapability.UNTYPED_SCALARS));

        p.close();
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
        JsonFactory f = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
                .build();

        String FIELD = "a\tb";
        String VALUE = "\t";
        String JSON = "{ "+q(FIELD)+" : "+q(VALUE)+"}";
        JsonParser p = useStream ? createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(FIELD, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(VALUE, p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    private void _testRecordSeparatorDefault(boolean useStream) throws Exception {
        JsonFactory f = new JsonFactory();
        String JSON = "[\"val:\"]\u001E";

        try (JsonParser p = useStream ? createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                p.nextToken(); // val
                p.nextToken(); // ]
                p.nextToken(); // RS token
                fail("Expected exception");
            } catch (StreamReadException e) {
                verifyException(e, "Illegal character ((CTRL-CHAR");
                verifyException(e, "consider enabling `JsonReadFeature.ALLOW_RS_CONTROL_CHAR`");
            }
        }
    }

    private void _testRecordSeparatorEnabled(boolean useStream) throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_RS_CONTROL_CHAR, true)
                .build();

        String FIELD = "key";
        String VALUE = "value";
        String JSON = "{ "+q(FIELD)+" : "+q(VALUE)+"}\u001E";
        JsonParser p = useStream ? createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(FIELD, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(VALUE, p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.nextToken(); // RS token
        p.close();
    }
}
