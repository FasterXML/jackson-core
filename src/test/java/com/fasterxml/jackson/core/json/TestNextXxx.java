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

    private final JsonFactory JSON_F = new JsonFactory();

    public void testIsNextTokenName() throws Exception
    {
        _testIsNextTokenName1(false);
        _testIsNextTokenName1(true);
    }

    public void testIsNextTokenName2() throws Exception {
        _testIsNextTokenName2(false);
        _testIsNextTokenName2(true);
    }        
    
    public void testIsNextTokenName3() throws Exception {
        _testIsNextTokenName3(false);
        _testIsNextTokenName3(true);
    }

    public void testIsNextTokenName4() throws Exception {
        _testIsNextTokenName4(false);
        _testIsNextTokenName4(true);
    }
    
    // [jackson-core#34]
    public void testIssue34() throws Exception
    {
        _testIssue34(false);
        _testIssue34(true);
    }

    // [jackson-core#38] with nextFieldName
    public void testIssue38() throws Exception
    {
        _testIssue38(false);
        _testIssue38(true);
    }

    public void testNextNameWithLongContent() throws Exception
    {
        _testNextNameWithLong(false);
        _testNextNameWithLong(true);
    }

    // for [core#220]: problem with `nextFieldName(str)`, indented content
    public void testNextNameWithIndentation() throws Exception
    {
        _testNextFieldNameIndent(false);
        _testNextFieldNameIndent(true);
    }
    
    public void testNextTextValue() throws Exception
    {
        _textNextText(false);
        _textNextText(true);
    }

    public void testNextIntValue() throws Exception
    {
        _textNextInt(false);
        _textNextInt(true);
    }

    public void testNextLongValue() throws Exception
    {
        _textNextLong(false);
        _textNextLong(true);
    }

    public void testNextBooleanValue() throws Exception
    {
        _textNextBoolean(false);
        _textNextBoolean(true);
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
        // do NOT check number value, to enforce skipping

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
        JsonParser jp = useStream ?
                JSON_F.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : JSON_F.createParser(new StringReader(DOC));
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

    private void _testIsNextTokenName3(boolean useStream) throws Exception
    {
        final String DOC = "{\"name\":123,\"name2\":14,\"x\":\"name\"}";
        JsonParser p = useStream ?
                JSON_F.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : JSON_F.createParser(new StringReader(DOC));
        assertNull(p.nextFieldName());
        assertToken(JsonToken.START_OBJECT, p.getCurrentToken());
        assertEquals("name", p.nextFieldName());
        assertToken(JsonToken.FIELD_NAME, p.getCurrentToken());
        assertEquals("name", p.getCurrentName());
        assertEquals("name", p.getText());
        assertNull(p.nextFieldName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.getCurrentToken());
        assertEquals(123, p.getIntValue());

        assertEquals("name2", p.nextFieldName());
        assertToken(JsonToken.FIELD_NAME, p.getCurrentToken());
        assertEquals("name2", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());

        assertEquals("x", p.nextFieldName());
        assertToken(JsonToken.FIELD_NAME, p.getCurrentToken());
        assertEquals("x", p.getCurrentName());

        assertNull(p.nextFieldName());
        assertToken(JsonToken.VALUE_STRING, p.getCurrentToken());

        assertNull(p.nextFieldName());
        assertToken(JsonToken.END_OBJECT, p.getCurrentToken());

        assertNull(p.nextFieldName());
        assertNull(p.getCurrentToken());

        p.close();
    }

    private void _testIsNextTokenName4(boolean useStream) throws Exception
    {
        final String DOC = "{\"name\":-123,\"name2\":99}";
        JsonParser jp = useStream ?
                JSON_F.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : JSON_F.createParser(new StringReader(DOC));
        assertToken(JsonToken.START_OBJECT, jp.nextToken());

        assertTrue(jp.nextFieldName(new SerializedString("name")));
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(-123, jp.getIntValue());

        assertTrue(jp.nextFieldName(new SerializedString("name2")));
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(99, jp.getIntValue());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());

        jp.close();
    }

    private void _testNextFieldNameIndent(boolean useStream) throws Exception
    {
        final String DOC = "{\n  \"name\" : \n  [\n  ]\n   }";
        JsonParser p = useStream ?
                JSON_F.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : JSON_F.createParser(new StringReader(DOC));
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertTrue(p.nextFieldName(new SerializedString("name")));

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertNull(p.nextToken());

        p.close();
    }

    private void _textNextText(boolean useStream) throws Exception
    {
        final String DOC = aposToQuotes("{'a':'123','b':5,'c':[false,'foo']}");
        JsonParser p = useStream ?
            JSON_F.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : JSON_F.createParser(new StringReader(DOC));
        assertNull(p.nextTextValue());
        assertToken(JsonToken.START_OBJECT, p.getCurrentToken());
        assertNull(p.nextTextValue());
        assertToken(JsonToken.FIELD_NAME, p.getCurrentToken());
        assertEquals("a", p.getCurrentName());

        assertEquals("123", p.nextTextValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertNull(p.nextFieldName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.getCurrentToken());

        assertEquals("c", p.nextFieldName());
        
        assertNull(p.nextTextValue());
        assertToken(JsonToken.START_ARRAY, p.getCurrentToken());
        assertNull(p.nextTextValue());
        assertToken(JsonToken.VALUE_FALSE, p.getCurrentToken());
        assertEquals("foo", p.nextTextValue());
        
        assertNull(p.nextTextValue());
        assertToken(JsonToken.END_ARRAY, p.getCurrentToken());
        assertNull(p.nextTextValue());
        assertToken(JsonToken.END_OBJECT, p.getCurrentToken());
        assertNull(p.nextTextValue());
        assertNull(p.getCurrentToken());

        p.close();
    }

    private void _textNextInt(boolean useStream) throws Exception
    {
        final String DOC = aposToQuotes("{'a':'123','b':5,'c':[false,456]}");
        JsonParser p = useStream ?
            JSON_F.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : JSON_F.createParser(new StringReader(DOC));
        assertEquals(0, p.nextIntValue(0));
        assertToken(JsonToken.START_OBJECT, p.getCurrentToken());
        assertEquals(0, p.nextIntValue(0));
        assertToken(JsonToken.FIELD_NAME, p.getCurrentToken());
        assertEquals("a", p.getCurrentName());

        assertEquals(0, p.nextIntValue(0));
        assertToken(JsonToken.VALUE_STRING, p.getCurrentToken());
        assertEquals("123", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertEquals(5, p.nextIntValue(0));

        assertEquals("c", p.nextFieldName());
        
        assertEquals(0, p.nextIntValue(0));
        assertToken(JsonToken.START_ARRAY, p.getCurrentToken());
        assertEquals(0, p.nextIntValue(0));
        assertToken(JsonToken.VALUE_FALSE, p.getCurrentToken());
        assertEquals(456, p.nextIntValue(0));
        
        assertEquals(0, p.nextIntValue(0));
        assertToken(JsonToken.END_ARRAY, p.getCurrentToken());
        assertEquals(0, p.nextIntValue(0));
        assertToken(JsonToken.END_OBJECT, p.getCurrentToken());
        assertEquals(0, p.nextIntValue(0));
        assertNull(p.getCurrentToken());

        p.close();
    }

    private void _textNextLong(boolean useStream) throws Exception
    {
        final String DOC = aposToQuotes("{'a':'xyz','b':-59,'c':[false,-1]}");
        JsonParser p = useStream ?
            JSON_F.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : JSON_F.createParser(new StringReader(DOC));
        assertEquals(0L, p.nextLongValue(0L));
        assertToken(JsonToken.START_OBJECT, p.getCurrentToken());
        assertEquals(0L, p.nextLongValue(0L));
        assertToken(JsonToken.FIELD_NAME, p.getCurrentToken());
        assertEquals("a", p.getCurrentName());

        assertEquals(0L, p.nextLongValue(0L));
        assertToken(JsonToken.VALUE_STRING, p.getCurrentToken());
        assertEquals("xyz", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertEquals(-59L, p.nextLongValue(0L));

        assertEquals("c", p.nextFieldName());
        
        assertEquals(0L, p.nextLongValue(0L));
        assertToken(JsonToken.START_ARRAY, p.getCurrentToken());
        assertEquals(0L, p.nextLongValue(0L));
        assertToken(JsonToken.VALUE_FALSE, p.getCurrentToken());
        assertEquals(-1L, p.nextLongValue(0L));
        
        assertEquals(0L, p.nextLongValue(0L));
        assertToken(JsonToken.END_ARRAY, p.getCurrentToken());
        assertEquals(0L, p.nextLongValue(0L));
        assertToken(JsonToken.END_OBJECT, p.getCurrentToken());
        assertEquals(0L, p.nextLongValue(0L));
        assertNull(p.getCurrentToken());

        p.close();
    }

    private void _textNextBoolean(boolean useStream) throws Exception
    {
        final String DOC = aposToQuotes("{'a':'xyz','b':true,'c':[false,0]}");
        JsonParser p = useStream ?
            JSON_F.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : JSON_F.createParser(new StringReader(DOC));
        assertNull(p.nextBooleanValue());
        assertToken(JsonToken.START_OBJECT, p.getCurrentToken());
        assertNull(p.nextBooleanValue());
        assertToken(JsonToken.FIELD_NAME, p.getCurrentToken());
        assertEquals("a", p.getCurrentName());

        assertNull(p.nextBooleanValue());
        assertToken(JsonToken.VALUE_STRING, p.getCurrentToken());
        assertEquals("xyz", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertEquals(Boolean.TRUE, p.nextBooleanValue());

        assertEquals("c", p.nextFieldName());
        
        assertNull(p.nextBooleanValue());
        assertToken(JsonToken.START_ARRAY, p.getCurrentToken());
        assertEquals(Boolean.FALSE, p.nextBooleanValue());
        assertNull(p.nextBooleanValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.getCurrentToken());
        assertEquals(0, p.getIntValue());
        
        assertNull(p.nextBooleanValue());
        assertToken(JsonToken.END_ARRAY, p.getCurrentToken());
        assertNull(p.nextBooleanValue());
        assertToken(JsonToken.END_OBJECT, p.getCurrentToken());
        assertNull(p.nextBooleanValue());
        assertNull(p.getCurrentToken());

        p.close();
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

    private void _testNextNameWithLong(boolean useStream) throws Exception
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
                JSON_F.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
                : JSON_F.createParser(new StringReader(DOC));
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
