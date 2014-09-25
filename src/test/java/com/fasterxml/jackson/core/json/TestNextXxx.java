package com.fasterxml.jackson.core.json;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Random;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;

public class TestNextXxx
    extends com.fasterxml.jackson.core.BaseTest
{
    /*
    /********************************************************
    /* Wrappers to test InputStream vs Reader
    /********************************************************
     */
    
    // [JACKSON-653]
    public void testIsNextTokenName() throws Exception
    {
        _testIsNextTokenName1(false);
        _testIsNextTokenName1(true);
        _testIsNextTokenName2(false);
        _testIsNextTokenName2(true);
    }

    // [Issue#34]
    public void testIssue34() throws Exception
    {
        _testIssue34(false);
        _testIssue34(true);
    }

    // [Issue#38] with nextFieldName
    public void testIssue38() throws Exception
    {
        _testIssue38(false);
        _testIssue38(true);
    }

    public void testNextNameWithLongContent() throws Exception
    {
        final JsonFactory jf = new JsonFactory();

        _testLong(jf, false);
        _testLong(jf, true);
    }
    
    /*
    /********************************************************
    /* Actual test code
    /********************************************************
     */

    private void _testIsNextTokenName1(boolean useStream) throws Exception
    {
        final String DOC = "{\"name\":123,\"name2\":14,\"x\":\"name\"}";
        JsonFactory jf = new JsonFactory();
        JsonParser jp = useStream ?
            jf.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : jf.createParser(new StringReader(DOC));
        final SerializedString NAME = new SerializedString("name");
        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.START_OBJECT, jp.getCurrentToken());
        assertTrue(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals(NAME.getValue(), jp.getCurrentName());
        assertEquals(NAME.getValue(), jp.getText());
        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.getCurrentToken());
        assertEquals(123, jp.getIntValue());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("name2", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("x", jp.getCurrentName());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_STRING, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertNull(jp.getCurrentToken());

        jp.close();

        // Actually, try again with slightly different sequence...
        jp = useStream ? jf.createParser(DOC.getBytes("UTF-8"))
                : jf.createParser(DOC);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertFalse(jp.nextFieldName(new SerializedString("Nam")));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals(NAME.getValue(), jp.getCurrentName());
        assertEquals(NAME.getValue(), jp.getText());
        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.getCurrentToken());
        assertEquals(123, jp.getIntValue());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("name2", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("x", jp.getCurrentName());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_STRING, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertNull(jp.getCurrentToken());

        jp.close();
    }

    private void _testIsNextTokenName2(boolean useStream) throws Exception
    {
        final String DOC = "{\"name\":123,\"name2\":14,\"x\":\"name\"}";
        JsonFactory jf = new JsonFactory();
        JsonParser jp = useStream ?
            jf.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : jf.createParser(new StringReader(DOC));
        SerializableString NAME = new SerializedString("name");
        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.START_OBJECT, jp.getCurrentToken());
        assertTrue(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals(NAME.getValue(), jp.getCurrentName());
        assertEquals(NAME.getValue(), jp.getText());
        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.getCurrentToken());
        assertEquals(123, jp.getIntValue());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("name2", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("x", jp.getCurrentName());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_STRING, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertNull(jp.getCurrentToken());

        jp.close();
    }

    private void _testIssue34(boolean useStream) throws Exception
    {
        final int TESTROUNDS = 223;
        final String DOC_PART = "{ \"fieldName\": 1 }";
        
        // build the big document to trigger issue
        StringBuilder sb = new StringBuilder(2000);
        for (int i = 0; i < TESTROUNDS; ++i) {
            sb.append(DOC_PART);
        }
        final String DOC = sb.toString();
        
        SerializableString fieldName = new SerializedString("fieldName");
        JsonFactory jf = new JsonFactory();
        JsonParser parser = useStream ?
            jf.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : jf.createParser(new StringReader(DOC));

        for (int i = 0; i < TESTROUNDS - 1; i++) {
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());

            // These will succeed
            assertTrue(parser.nextFieldName(fieldName));

            parser.nextLongValue(-1);
            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        }

        assertEquals(JsonToken.START_OBJECT, parser.nextToken());

        // This will fail
        assertTrue(parser.nextFieldName(fieldName));
        parser.close();
    }

    private void _testIssue38(boolean useStream) throws Exception
    {
        final String DOC = "{\"field\" :\"value\"}";
        SerializableString fieldName = new SerializedString("field");
        JsonFactory jf = new JsonFactory();
        JsonParser parser = useStream ?
            jf.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : jf.createParser(new StringReader(DOC));
        assertEquals(JsonToken.START_OBJECT, parser.nextToken());
        assertTrue(parser.nextFieldName(fieldName));
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("value", parser.getText());
        assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        assertNull(parser.nextToken());
        parser.close();
    }

    private void _testLong(JsonFactory f, boolean useStream) throws Exception
    {
        // do 5 meg thingy
        final int SIZE = 5 * 1024 * 1024;
        StringBuilder sb = new StringBuilder(SIZE + 20);

        sb.append("{");
        Random rnd = new Random(1);
        int count = 0;
        while (sb.length() < SIZE) {
            ++count;
            if (sb.length() > 1) {
                sb.append(", ");
            }
            int val = rnd.nextInt();
            sb.append('"');
            sb.append("f"+val);
            sb.append("\":");
            sb.append(String.valueOf(val % 1000));
        }
        sb.append("}");
        final String DOC = sb.toString();
    
        JsonParser parser = useStream ?
                f.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
                : f.createParser(new StringReader(DOC));
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        rnd = new Random(1);
        for (int i = 0; i < count; ++i) {
            int exp = rnd.nextInt();
            SerializableString expName = new SerializedString("f"+exp);
            assertTrue(parser.nextFieldName(expName));
            assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(exp % 1000, parser.getIntValue());
        }
        assertToken(JsonToken.END_OBJECT, parser.nextToken());
        parser.close();
    }
}
