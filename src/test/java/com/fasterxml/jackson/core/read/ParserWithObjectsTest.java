package com.fasterxml.jackson.core.read;

import java.io.*;

import com.fasterxml.jackson.core.*;

/**
 * Unit tests for verifying that object mapping functionality can
 * be accessed using JsonParser.
 */
public class ParserWithObjectsTest
    extends com.fasterxml.jackson.core.BaseTest
{
    /*
    /**********************************************************
    /* Test for simple traversal with data mapping
    /**********************************************************
     */

    public void testNextValue() throws IOException
    {
        // Let's test both byte-backed and Reader-based one
        _testNextValueBasic(false);
        _testNextValueBasic(true);
    }

    // [JACKSON-395]
    public void testNextValueNested() throws IOException
    {
        // Let's test both byte-backed and Reader-based one
        _testNextValueNested(false);
        _testNextValueNested(true);
    }

    @SuppressWarnings("resource")
    public void testIsClosed() throws IOException
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
        JsonParser jp = _getParser("[ 1, 2, 3, 4 ]", useStream);
        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        for (int i = 1; i <= 4; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
            assertEquals(i, jp.getIntValue());
        }
        assertToken(JsonToken.END_ARRAY, jp.nextValue());
        assertNull(jp.nextValue());
        jp.close();

        // then Object, is different
        jp = _getParser("{ \"3\" :3, \"4\": 4, \"5\" : 5 }", useStream);
        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        for (int i = 3; i <= 5; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
            assertEquals(String.valueOf(i), jp.getCurrentName());
            assertEquals(i, jp.getIntValue());
        }
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertNull(jp.nextValue());
        jp.close();

        // and then mixed...
        jp = _getParser("[ true, [ ], { \"a\" : 3 } ]", useStream);

        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        assertToken(JsonToken.VALUE_TRUE, jp.nextValue());
        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        assertToken(JsonToken.END_ARRAY, jp.nextValue());

        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertToken(JsonToken.END_ARRAY, jp.nextValue());

        assertNull(jp.nextValue());
        jp.close();
    }

    // [JACKSON-395]
    private void  _testNextValueNested(boolean useStream) throws IOException
    {
        // first array, no change to default
        JsonParser jp;

        // then object with sub-objects...
        jp = _getParser("{\"a\": { \"b\" : true, \"c\": false }, \"d\": 3 }", useStream);

        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, jp.nextValue());
        assertEquals("b", jp.getCurrentName());
        assertToken(JsonToken.VALUE_FALSE, jp.nextValue());
        assertEquals("c", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        // ideally we should match closing marker with field, too:
        assertEquals("a", jp.getCurrentName());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
        assertEquals("d", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertNull(jp.nextValue());
        jp.close();

        // and arrays
        jp = _getParser("{\"a\": [ false ] }", useStream);

        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.VALUE_FALSE, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.END_ARRAY, jp.nextValue());
        // ideally we should match closing marker with field, too:
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertNull(jp.nextValue());
        jp.close();
    }

    private JsonParser _getParser(String doc, boolean useStream)
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        if (useStream) {
            return jf.createParser(doc.getBytes("UTF-8"));
        }
        return jf.createParser(new StringReader(doc));
    }
}
