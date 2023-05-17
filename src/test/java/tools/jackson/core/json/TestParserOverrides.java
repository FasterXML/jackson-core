package tools.jackson.core.json;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;

public class TestParserOverrides extends tools.jackson.core.BaseTest
{
    /*
    /**********************************************************
    /* Wrappers, to test stream and reader-based parsers
    /**********************************************************
     */

    public void testTokenAccess() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        _testTokenAccess(jf, false);
        _testTokenAccess(jf, true);
    }

    public void testCurrentName() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        _testCurrentName(jf, false);
        _testCurrentName(jf, true);
    }

    /*
    /**********************************************************
    /* Actual test methods
    /**********************************************************
     */

    public void _testTokenAccess(JsonFactory jf, boolean useStream) throws Exception
    {
        final String DOC = "[ ]";
        JsonParser jp = useStream ?
                jf.createParser(ObjectReadContext.empty(), new ByteArrayInputStream(DOC.getBytes("UTF-8")))
                : jf.createParser(ObjectReadContext.empty(), DOC);
        assertNull(jp.currentToken());
        jp.clearCurrentToken();
        assertNull(jp.currentToken());
        assertNull(jp.getEmbeddedObject());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.START_ARRAY, jp.currentToken());
        jp.clearCurrentToken();
        assertNull(jp.currentToken());
        // Also: no codec defined by default
        try {
            jp.readValueAsTree();
            fail("Should get exception without codec");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Operation not supported");
        }
        jp.close();
    }

    private void _testCurrentName(JsonFactory jf, boolean useStream) throws Exception
    {
        final String DOC = "{\"first\":{\"second\":3, \"third\":false}}";
        JsonParser jp = useStream ?
                jf.createParser(ObjectReadContext.empty(), new ByteArrayInputStream(DOC.getBytes("UTF-8")))
                : jf.createParser(ObjectReadContext.empty(), new StringReader(DOC));
        assertNull(jp.currentToken());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken());
        assertEquals("first", jp.currentName());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertEquals("first", jp.currentName()); // still the same...
//        jp.overrideCurrentName("foobar");
//        assertEquals("foobar", jp.currentName()); // but not any more!

        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken());
        assertEquals("second", jp.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals("second", jp.currentName());

        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken());
        assertEquals("third", jp.currentName());
        assertToken(JsonToken.VALUE_FALSE, jp.nextToken());
        assertEquals("third", jp.currentName());

        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        // should retain overrides, too
//        assertEquals("foobar", jp.currentName());

        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        jp.clearCurrentToken();
        assertNull(jp.currentToken());
        jp.close();
    }
}
