package com.fasterxml.jackson.core.json;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import static org.junit.jupiter.api.Assertions.*;

public class TestParserOverrides extends com.fasterxml.jackson.core.JUnit5TestBase
{
    /*
    /**********************************************************
    /* Wrappers, to test stream and reader-based parsers
    /**********************************************************
     */

    @Test
    void tokenAccess() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        _testTokenAccess(jf, false);
        _testTokenAccess(jf, true);
    }

    @Test
    void currentName() throws Exception
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
                jf.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
                : jf.createParser(DOC);
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
        } catch (IllegalStateException e) {
            verifyException(e, "No ObjectCodec defined");
        }
        jp.close();
    }

    private void _testCurrentName(JsonFactory f, boolean useStream) throws Exception
    {
        final String DOC = "{\"first\":{\"second\":3, \"third\":false}}";
        JsonParser p = useStream ?
                f.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
                : f.createParser(new StringReader(DOC));
        assertNull(p.currentToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("first", p.currentName());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("first", p.currentName()); // still the same...
        p.overrideCurrentName("foobar");
        assertEquals("foobar", p.currentName()); // but not any more!

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("second", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("second", p.currentName());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("third", p.currentName());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals("third", p.currentName());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        // should retain overrides, too
        assertEquals("foobar", p.currentName());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.clearCurrentToken();
        assertNull(p.currentToken());
        p.close();
    }
}
