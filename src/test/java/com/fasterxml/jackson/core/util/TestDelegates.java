package com.fasterxml.jackson.core.util;

import java.io.*;
import java.util.Iterator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.json.JsonFactory;

import static org.junit.Assert.assertArrayEquals;

public class TestDelegates extends com.fasterxml.jackson.core.BaseTest
{
    static class POJO {
        public int x = 3;
    }

    static class BogusTree implements TreeNode {
        @Override
        public JsonToken asToken() {
            return null;
        }

        @Override
        public NumberType numberType() {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isValueNode() {
            return false;
        }

        @Override
        public boolean isContainerNode() {
            return false;
        }

        @Override
        public boolean isMissingNode() {
            return false;
        }

        @Override
        public boolean isEmbeddedValue() {
            return false;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean isObject() {
            return false;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public TreeNode get(String fieldName) {
            return null;
        }

        @Override
        public TreeNode get(int index) {
            return null;
        }

        @Override
        public TreeNode path(String fieldName) {
            return null;
        }

        @Override
        public TreeNode path(int index) {
            return null;
        }

        @Override
        public Iterator<String> fieldNames() {
            return null;
        }

        @Override
        public TreeNode at(JsonPointer ptr) {
            return null;
        }

        @Override
        public TreeNode at(String jsonPointerExpression) {
            return null;
        }

        @Override
        public JsonParser traverse(ObjectReadContext readCtxt) {
            return null;
        }
    }

    private final JsonFactory JSON_F = new JsonFactory();

    /**
     * Test default, non-overridden parser delegate.
     */
    public void testParserDelegate() throws IOException
    {
        final String TOKEN ="foo";

        JsonParser parser = JSON_F.createParser(ObjectReadContext.empty(),
                "[ 1, true, null, { \"a\": \"foo\" }, \"AQI=\" ]");
        JsonParserDelegate del = new JsonParserDelegate(parser);

        // Basic capabilities for parser:
        assertFalse(del.canParseAsync());
        assertFalse(del.canReadObjectId());
        assertFalse(del.canReadTypeId());
        assertEquals(parser.version(), del.version());

        // configuration
        assertFalse(del.isEnabled(StreamReadFeature.IGNORE_UNDEFINED));
        assertSame(parser, del.delegate());
        assertNull(del.getSchema());

        // initial state
        assertNull(del.currentToken());
        assertFalse(del.hasCurrentToken());
        assertFalse(del.hasTextCharacters());
        assertNull(del.getCurrentValue());
        assertNull(del.getCurrentName());

        assertToken(JsonToken.START_ARRAY, del.nextToken());
        assertEquals(JsonTokenId.ID_START_ARRAY, del.currentTokenId());
        assertTrue(del.hasToken(JsonToken.START_ARRAY));
        assertFalse(del.hasToken(JsonToken.START_OBJECT));
        assertTrue(del.hasTokenId(JsonTokenId.ID_START_ARRAY));
        assertFalse(del.hasTokenId(JsonTokenId.ID_START_OBJECT));
        assertTrue(del.isExpectedStartArrayToken());
        assertFalse(del.isExpectedStartObjectToken());
        assertEquals("[", del.getText());
        assertNotNull(del.getParsingContext());
        assertSame(parser.getParsingContext(), del.getParsingContext());

        assertToken(JsonToken.VALUE_NUMBER_INT, del.nextToken());
        assertEquals(1, del.getIntValue());
        assertEquals(1, del.getValueAsInt());
        assertEquals(1, del.getValueAsInt(3));
        assertEquals(1L, del.getValueAsLong());
        assertEquals(1L, del.getValueAsLong(3L));
        assertEquals(1L, del.getLongValue());
        assertEquals(1d, del.getValueAsDouble());
        assertEquals(1d, del.getValueAsDouble(0.25));
        assertEquals(1d, del.getDoubleValue());
        assertTrue(del.getValueAsBoolean());
        assertTrue(del.getValueAsBoolean(false));
        assertEquals((byte)1, del.getByteValue());
        assertEquals((short)1, del.getShortValue());
        assertEquals(1f, del.getFloatValue());
        assertFalse(del.isNaN());
        assertEquals(NumberType.INT, del.getNumberType());
        assertEquals(Integer.valueOf(1), del.getNumberValue());
        assertNull(del.getEmbeddedObject());
        
        assertToken(JsonToken.VALUE_TRUE, del.nextToken());
        assertTrue(del.getBooleanValue());
        assertEquals(parser.getCurrentLocation(), del.getCurrentLocation());
        assertNull(del.getTypeId());
        assertNull(del.getObjectId());

        assertToken(JsonToken.VALUE_NULL, del.nextToken());
        assertNull(del.getCurrentValue());
        del.setCurrentValue(TOKEN);

        assertToken(JsonToken.START_OBJECT, del.nextToken());
        assertNull(del.getCurrentValue());

        assertToken(JsonToken.FIELD_NAME, del.nextToken());
        assertEquals("a", del.getCurrentName());

        assertToken(JsonToken.VALUE_STRING, del.nextToken());
        assertTrue(del.hasTextCharacters());
        assertEquals("foo", del.getText());
        
        assertToken(JsonToken.END_OBJECT, del.nextToken());
        assertEquals(TOKEN, del.getCurrentValue());

        assertToken(JsonToken.VALUE_STRING, del.nextToken());
        assertArrayEquals(new byte[] { 1, 2 }, del.getBinaryValue());
        
        assertToken(JsonToken.END_ARRAY, del.nextToken());

        del.close();
        assertTrue(del.isClosed());
        assertTrue(parser.isClosed());

        parser.close();
    }

    /**
     * Test default, non-overridden generator delegate.
     */
    public void testGeneratorDelegate() throws IOException
    {
        final String TOKEN ="foo";

        StringWriter sw = new StringWriter();
        JsonGenerator g0 = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        JsonGeneratorDelegate del = new JsonGeneratorDelegate(g0);
        del.writeStartArray();

        assertEquals(1, del.getOutputBuffered());
        
        del.writeNumber(13);
        del.writeNull();
        del.writeBoolean(false);
        del.writeString("foo");

        // verify that we can actually set/get "current value" as expected, even with delegates
        assertNull(del.getCurrentValue());
        del.setCurrentValue(TOKEN);

        del.writeStartObject();
        assertNull(del.getCurrentValue());
        del.writeEndObject();
        assertEquals(TOKEN, del.getCurrentValue());

        del.writeStartArray(0);
        del.writeEndArray();

        del.writeEndArray();
        
        del.flush();
        del.close();
        assertTrue(del.isClosed());        
        assertTrue(g0.isClosed());        
        assertEquals("[13,null,false,\"foo\",{},[]]", sw.toString());

        g0.close();
    }

    public void testNotDelegateCopyMethods() throws IOException
    {
        JsonParser jp = JSON_F.createParser(ObjectReadContext.empty(), "[{\"a\":[1,2,{\"b\":3}],\"c\":\"d\"},{\"e\":false},null]");
        StringWriter sw = new StringWriter();
        JsonGenerator jg = new JsonGeneratorDelegate(JSON_F.createGenerator(ObjectWriteContext.empty(), sw), false) {
            @Override
            public void writeFieldName(String name) throws IOException {
                super.writeFieldName(name+"-test");
                super.writeBoolean(true);
                super.writeFieldName(name);
            }
        };
        jp.nextToken();
        jg.copyCurrentStructure(jp);
        jg.flush();
        assertEquals("[{\"a-test\":true,\"a\":[1,2,{\"b-test\":true,\"b\":3}],\"c-test\":true,\"c\":\"d\"},{\"e-test\":true,\"e\":false},null]", sw.toString());
        jp.close();
        jg.close();
    }
}
