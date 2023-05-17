package tools.jackson.core.json;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;

/**
 * Unit tests for verifying that additional <code>JsonParser.Feature</code>
 * and {@link JsonReadFeature}
 * settings work as expected.
 */
public class JsonReadFeaturesTest
    extends tools.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = sharedStreamFactory();

    public void testStreamReadFeatureDefaults() throws Exception
    {
        _testDefaultSettings(createParser(JSON_F, MODE_INPUT_STREAM, "{}"));
        _testDefaultSettings(createParser(JSON_F, MODE_READER, "{}"));
        _testDefaultSettings(createParser(JSON_F, MODE_DATA_INPUT, "{}"));
    }

    public void testJsonReadFeatureDefaultSettings() throws Exception
    {
        assertFalse(JSON_F.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        assertFalse(JSON_F.isEnabled(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS));
        assertFalse(JSON_F.isEnabled(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES));
        assertFalse(JSON_F.isEnabled(JsonReadFeature.ALLOW_SINGLE_QUOTES));
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

    private void _testDefaultSettings(JsonParser p) throws Exception {
        assertFalse(p.canReadObjectId());
        assertFalse(p.canReadTypeId());

        // [core#619]:
        assertFalse(p.streamReadCapabilities().isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES));
        assertFalse(p.streamReadCapabilities().isEnabled(StreamReadCapability.SCALARS_AS_OBJECTS));
        assertFalse(p.streamReadCapabilities().isEnabled(StreamReadCapability.UNTYPED_SCALARS));

        p.close();
    }

    private void _testQuotesRequired(boolean useStream) throws Exception
    {
        final String JSON = "{ test : 3 }";
        final String EXP_ERROR_FRAGMENT = "was expecting double-quote to start";
        JsonFactory f = new JsonFactory();
        try (JsonParser p = useStream ?
            createParserUsingStream(f, JSON, "UTF-8")
            : createParserUsingReader(f, JSON)
            ) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            p.nextToken();
        } catch (StreamReadException je) {
            verifyException(je, EXP_ERROR_FRAGMENT);
        }
    }

    private void _testTabsDefault(boolean useStream) throws Exception
    {
        JsonFactory f = streamFactoryBuilder()
                .disable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS).build();
        // First, let's see that by default unquoted tabs are illegal
        String JSON = "[\"tab:\t\"]";
        try (JsonParser p = useStream ? createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            p.nextToken();
            p.getText();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Illegal unquoted character");
        }
    }

    private void _testTabsEnabled(boolean useStream) throws Exception
    {
        JsonFactory f = streamFactoryBuilder()
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
                .build();
        String PROP_NAME = "a\tb";
        String VALUE = "\t";
        String JSON = "{ "+q(PROP_NAME)+" : "+q(VALUE)+"}";
        JsonParser p = useStream ? createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals(PROP_NAME, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(VALUE, p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }
}
