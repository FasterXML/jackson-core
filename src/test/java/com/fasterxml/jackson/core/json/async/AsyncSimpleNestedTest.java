package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncSimpleNestedTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    /*
    /**********************************************************************
    /* Test methods, success
    /**********************************************************************
     */

    public void testStuffInObject() throws Exception
    {
        byte[] data = _jsonDoc(a2q(
                "{'foobar':[1,2,-999],'emptyObject':{},'emptyArray':[], 'other':{'':null} }"));

        JsonFactory f = JSON_F;
        _testStuffInObject(f, data, 0, 100);
        _testStuffInObject(f, data, 0, 3);
        _testStuffInObject(f, data, 0, 1);

        _testStuffInObject(f, data, 1, 100);
        _testStuffInObject(f, data, 1, 3);
        _testStuffInObject(f, data, 1, 1);
    }

    private void _testStuffInObject(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertFalse(r.parser().hasTextCharacters());

        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("foobar", r.currentName());
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertEquals("[", r.currentText());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(1, r.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(2, r.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(-999, r.getIntValue());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("emptyObject", r.currentName());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());


        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("emptyArray", r.currentName());
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("other", r.currentName());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("", r.currentName());
        assertToken(JsonToken.VALUE_NULL, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.END_OBJECT, r.nextToken());

        // another twist: close in the middle, verify
        r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        r.parser().close();
        assertTrue(r.parser().isClosed());
        assertNull(r.parser().nextToken());
    }

    public void testStuffInArray() throws Exception
    {
        byte[] data = _jsonDoc(a2q("[true,{'moreStuff':0},[null],{'extraOrdinary':23}]"));
        JsonFactory f = JSON_F;

        _testStuffInArray(f, data, 0, 100);
        _testStuffInArray(f, data, 0, 3);
        _testStuffInArray(f, data, 0, 1);

        _testStuffInArray(f, data, 3, 100);
        _testStuffInArray(f, data, 3, 3);
        _testStuffInArray(f, data, 3, 1);
    }

    private void _testStuffInArray(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertFalse(r.parser().hasTextCharacters());

        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertEquals("{", r.currentText());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("moreStuff", r.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(0L, r.getLongValue());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_NULL, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("extraOrdinary", r.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(23, r.getIntValue());
        assertToken(JsonToken.END_OBJECT, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());
    }

    final static String SHORT_NAME = String.format("u-%s", UNICODE_SEGMENT);
    final static String LONG_NAME = String.format("Unicode-with-some-longer-name-%s", UNICODE_SEGMENT);

    public void testStuffInArray2() throws Exception
    {
        byte[] data = _jsonDoc(a2q(String.format(
                "[{'%s':true},{'%s':false},{'%s':true},{'%s':false}]",
                SHORT_NAME, LONG_NAME, LONG_NAME, SHORT_NAME)));
        JsonFactory f = JSON_F;

        _testStuffInArray2(f, data, 0, 100);
        _testStuffInArray2(f, data, 0, 3);
        _testStuffInArray2(f, data, 0, 1);

        _testStuffInArray2(f, data, 3, 100);
        _testStuffInArray2(f, data, 3, 3);
        _testStuffInArray2(f, data, 3, 1);
    }

    private void _testStuffInArray2(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(SHORT_NAME, r.currentName());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(LONG_NAME, r.currentName());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(LONG_NAME, r.currentName());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(SHORT_NAME, r.currentName());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.END_ARRAY, r.nextToken());
    }

    /*
    /**********************************************************************
    /* Test methods, fail checking
    /**********************************************************************
     */

    public void testMismatchedArray() throws Exception
    {
        byte[] data = _jsonDoc(a2q("[  }"));

        JsonFactory f = JSON_F;
        _testMismatchedArray(f, data, 0, 99);
        _testMismatchedArray(f, data, 0, 3);
        _testMismatchedArray(f, data, 0, 2);
        _testMismatchedArray(f, data, 0, 1);

        _testMismatchedArray(f, data, 1, 3);
        _testMismatchedArray(f, data, 1, 1);
    }

    private void _testMismatchedArray(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        try {
            r.nextToken();
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected close marker '}': expected ']'");
        }
    }

    public void testMismatchedObject() throws Exception
    {
        byte[] data = _jsonDoc(a2q("{ ]"));

        JsonFactory f = JSON_F;
        _testMismatchedObject(f, data, 0, 99);
        _testMismatchedObject(f, data, 0, 3);
        _testMismatchedObject(f, data, 0, 2);
        _testMismatchedObject(f, data, 0, 1);

        _testMismatchedObject(f, data, 1, 3);
        _testMismatchedObject(f, data, 1, 1);
    }

    private void _testMismatchedObject(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        try {
            r.nextToken();
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected close marker ']': expected '}'");
        }
    }
}
