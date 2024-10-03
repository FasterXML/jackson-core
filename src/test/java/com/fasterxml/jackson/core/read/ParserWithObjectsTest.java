package com.fasterxml.jackson.core.read;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying that object mapping functionality can
 * be accessed using JsonParser.
 */
class ParserWithObjectsTest
        extends JUnit5TestBase
{
    /*
    /**********************************************************
    /* Test for simple traversal with data mapping
    /**********************************************************
     */

    @Test
    void nextValue() throws IOException
    {
        // Let's test both byte-backed and Reader-based one
        _testNextValueBasic(false);
        _testNextValueBasic(true);
    }

    // [JACKSON-395]
    @Test
    void nextValueNested() throws IOException
    {
        // Let's test both byte-backed and Reader-based one
        _testNextValueNested(false);
        _testNextValueNested(true);
    }

    @SuppressWarnings("resource")
    @Test
    void isClosed() throws IOException
    {
        for (int i = 0; i < 4; ++i) {
            String JSON = "[ 1, 2, 3 ]";
            boolean stream = ((i & 1) == 0);
            JsonParser jp = stream ?
                createParserUsingStream(JSON, "UTF-8")
                : createParserUsingReader(JSON);
            boolean partial = ((i & 2) == 0);

            assertFalse(jp.isClosed());
            assertToken(JsonToken.START_ARRAY, jp.nextToken());

            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertFalse(jp.isClosed());

            if (partial) {
                jp.close();
                assertTrue(jp.isClosed());
            } else {
                assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertToken(JsonToken.END_ARRAY, jp.nextToken());
                assertNull(jp.nextToken());
                assertTrue(jp.isClosed());
            }
        }
    }

    /*
    /**********************************************************
    /* Supporting methods
    /**********************************************************
     */

    private void  _testNextValueBasic(boolean useStream) throws IOException
    {
        // first array, no change to default
        JsonParser p = _getParser("[ 1, 2, 3, 4 ]", useStream);
        assertToken(JsonToken.START_ARRAY, p.nextValue());
        for (int i = 1; i <= 4; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextValue());
            assertEquals(i, p.getIntValue());
        }
        assertToken(JsonToken.END_ARRAY, p.nextValue());
        assertNull(p.nextValue());
        p.close();

        // then Object, is different
        p = _getParser("{ \"3\" :3, \"4\": 4, \"5\" : 5 }", useStream);
        assertToken(JsonToken.START_OBJECT, p.nextValue());
        for (int i = 3; i <= 5; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextValue());
            assertEquals(String.valueOf(i), p.currentName());
            assertEquals(i, p.getIntValue());
        }
        assertToken(JsonToken.END_OBJECT, p.nextValue());
        assertNull(p.nextValue());
        p.close();

        // and then mixed...
        p = _getParser("[ true, [ ], { \"a\" : 3 } ]", useStream);

        assertToken(JsonToken.START_ARRAY, p.nextValue());
        assertToken(JsonToken.VALUE_TRUE, p.nextValue());
        assertToken(JsonToken.START_ARRAY, p.nextValue());
        assertToken(JsonToken.END_ARRAY, p.nextValue());

        assertToken(JsonToken.START_OBJECT, p.nextValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextValue());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.END_OBJECT, p.nextValue());
        assertToken(JsonToken.END_ARRAY, p.nextValue());

        assertNull(p.nextValue());
        p.close();
    }

    // [JACKSON-395]
    private void  _testNextValueNested(boolean useStream) throws IOException
    {
        // first array, no change to default
        JsonParser p;

        // then object with sub-objects...
        p = _getParser("{\"a\": { \"b\" : true, \"c\": false }, \"d\": 3 }", useStream);

        assertToken(JsonToken.START_OBJECT, p.nextValue());
        assertNull(p.currentName());
        assertToken(JsonToken.START_OBJECT, p.nextValue());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextValue());
        assertEquals("b", p.currentName());
        assertToken(JsonToken.VALUE_FALSE, p.nextValue());
        assertEquals("c", p.currentName());
        assertToken(JsonToken.END_OBJECT, p.nextValue());
        // ideally we should match closing marker with field, too:
        assertEquals("a", p.currentName());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextValue());
        assertEquals("d", p.currentName());
        assertToken(JsonToken.END_OBJECT, p.nextValue());
        assertNull(p.currentName());
        assertNull(p.nextValue());
        p.close();

        // and arrays
        p = _getParser("{\"a\": [ false ] }", useStream);

        assertToken(JsonToken.START_OBJECT, p.nextValue());
        assertNull(p.currentName());
        assertToken(JsonToken.START_ARRAY, p.nextValue());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_FALSE, p.nextValue());
        assertNull(p.currentName());
        assertToken(JsonToken.END_ARRAY, p.nextValue());
        // ideally we should match closing marker with field, too:
        assertEquals("a", p.currentName());
        assertToken(JsonToken.END_OBJECT, p.nextValue());
        assertNull(p.currentName());
        assertNull(p.nextValue());
        p.close();
    }

    private JsonParser _getParser(String doc, boolean useStream)
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        if (useStream) {
            return jf.createParser(utf8Bytes(doc));
        }
        return jf.createParser(new StringReader(doc));
    }
}
