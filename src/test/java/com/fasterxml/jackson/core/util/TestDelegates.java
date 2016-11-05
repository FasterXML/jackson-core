package com.fasterxml.jackson.core.util;

import java.io.*;
import java.util.Iterator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;

public class TestDelegates extends com.fasterxml.jackson.core.BaseTest
{
    static class POJO {
        public int x = 3;
    }

    static class BogusCodec extends ObjectCodec
    {
        public Object pojoWritten;
        public TreeNode treeWritten;

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public <T> T readValue(JsonParser p, Class<T> valueType) {
            return null;
        }
        @Override
        public <T> T readValue(JsonParser p, TypeReference<?> valueTypeRef) {
            return null;
        }
        @Override
        public <T> T readValue(JsonParser p, ResolvedType valueType) {
            return null;
        }
        @Override
        public <T> Iterator<T> readValues(JsonParser p, Class<T> valueType) {
            return null;
        }
        @Override
        public <T> Iterator<T> readValues(JsonParser p,
                TypeReference<?> valueTypeRef) throws IOException {
            return null;
        }
        @Override
        public <T> Iterator<T> readValues(JsonParser p, ResolvedType valueType) {
            return null;
        }
        @Override
        public void writeValue(JsonGenerator gen, Object value) throws IOException {
            gen.writeString("pojo");
            pojoWritten = value;
        }

        @Override
        public <T extends TreeNode> T readTree(JsonParser p) {
            return null;
        }
        @Override
        public void writeTree(JsonGenerator gen, TreeNode tree) throws IOException {
            gen.writeString("tree");
            treeWritten = tree;
        }

        @Override
        public TreeNode createObjectNode() {
            return null;
        }
        @Override
        public TreeNode createArrayNode() {
            return null;
        }
        @Override
        public JsonParser treeAsTokens(TreeNode n) {
            return null;
        }
        @Override
        public <T> T treeToValue(TreeNode n, Class<T> valueType) {
            return null;
        } 
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
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean isObject() {
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
        public JsonParser traverse() {
            return null;
        }

        @Override
        public JsonParser traverse(ObjectCodec codec) {
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

        JsonParser parser = JSON_F.createParser("[ 1, true, null, { } ]");
        JsonParserDelegate del = new JsonParserDelegate(parser);
        
        assertNull(del.currentToken());
        assertToken(JsonToken.START_ARRAY, del.nextToken());
        assertEquals("[", del.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, del.nextToken());
        assertEquals(1, del.getIntValue());

        assertToken(JsonToken.VALUE_TRUE, del.nextToken());
        assertTrue(del.getBooleanValue());

        assertToken(JsonToken.VALUE_NULL, del.nextToken());
        assertNull(del.getCurrentValue());
        del.setCurrentValue(TOKEN);

        assertToken(JsonToken.START_OBJECT, del.nextToken());
        assertNull(del.getCurrentValue());

        assertToken(JsonToken.END_OBJECT, del.nextToken());
        assertEquals(TOKEN, del.getCurrentValue());

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
        JsonGenerator g0 = JSON_F.createGenerator(sw);
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
        JsonParser jp = JSON_F.createParser("[{\"a\":[1,2,{\"b\":3}],\"c\":\"d\"},{\"e\":false},null]");
        StringWriter sw = new StringWriter();
        JsonGenerator jg = new JsonGeneratorDelegate(JSON_F.createGenerator(sw), false) {
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

    @SuppressWarnings("resource")
    public void testGeneratorWithCodec() throws IOException
    {
        BogusCodec codec = new BogusCodec();
        StringWriter sw = new StringWriter();
        JsonGenerator g0 = JSON_F.createGenerator(sw);
        g0.setCodec(codec);
        JsonGeneratorDelegate del = new JsonGeneratorDelegate(g0, false);
        del.writeStartArray();
        POJO pojo = new POJO();
        del.writeObject(pojo);
        TreeNode tree = new BogusTree();
        del.writeTree(tree);
        del.writeEndArray();
        del.close();

        assertEquals("[\"pojo\",\"tree\"]", sw.toString());

        assertSame(tree, codec.treeWritten);
        assertSame(pojo, codec.pojoWritten);
    }
}
