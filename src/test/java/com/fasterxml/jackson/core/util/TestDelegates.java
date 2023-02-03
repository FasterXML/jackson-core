package com.fasterxml.jackson.core.util;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;

import static org.junit.Assert.assertArrayEquals;

public class TestDelegates extends com.fasterxml.jackson.core.BaseTest
{
    static class BogusSchema implements FormatSchema
    {
        @Override
        public String getSchemaType() {
            return "test";
        }
    }

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
        public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef) {
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
                TypeReference<T> valueTypeRef) throws IOException {
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
        public TreeNode missingNode() {
            return null;
        }

        @Override
        public TreeNode nullNode() {
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
        final int MAX_NUMBER_LEN = 200;
        final String TOKEN ="foo";

        StreamReadConstraints CUSTOM_CONSTRAINTS = StreamReadConstraints.builder()
                .maxNumberLength(MAX_NUMBER_LEN)
                .build();
        JsonFactory jsonF = JsonFactory.builder()
                .streamReadConstraints(CUSTOM_CONSTRAINTS)
                .build();
        JsonParser parser = jsonF.createParser("[ 1, true, null, { \"a\": \"foo\" }, \"AQI=\" ]");
        JsonParserDelegate del = new JsonParserDelegate(parser);

        // Basic capabilities for parser:
        assertFalse(del.canParseAsync());
        assertFalse(del.canReadObjectId());
        assertFalse(del.canReadTypeId());
        assertFalse(del.requiresCustomCodec());
        assertEquals(parser.version(), del.version());
        assertSame(parser.streamReadConstraints(), del.streamReadConstraints());
        assertSame(parser.getReadCapabilities(), del.getReadCapabilities());

        // configuration
        assertFalse(del.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
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
        assertFalse(del.isExpectedNumberIntToken());
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
        assertTrue(del.isExpectedNumberIntToken());
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
    @SuppressWarnings("deprecation")
    public void testGeneratorDelegate() throws IOException
    {
        final String TOKEN ="foo";

        StringWriter sw = new StringWriter();
        JsonGenerator g0 = JSON_F.createGenerator(sw);
        JsonGeneratorDelegate del = new JsonGeneratorDelegate(g0);

        // Basic capabilities for parser:
        assertTrue(del.canOmitFields());
        assertFalse(del.canWriteBinaryNatively());
        assertTrue(del.canWriteFormattedNumbers());
        assertFalse(del.canWriteObjectId());
        assertFalse(del.canWriteTypeId());
        assertEquals(g0.version(), del.version());

        // configuration
        assertTrue(del.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        assertFalse(del.isEnabled(StreamWriteFeature.IGNORE_UNKNOWN));
        assertSame(g0, del.delegate());
        assertSame(g0, del.getDelegate()); // deprecated as of 2.11

        assertFalse(del.canUseSchema(new BogusSchema()));

        // initial state
        assertNull(del.getSchema());
        assertNull(del.getPrettyPrinter());

        del.writeStartArray();

        assertEquals(1, del.getOutputBuffered());

        del.writeNumber(13);
        del.writeNumber(BigInteger.ONE);
        del.writeNumber(new BigDecimal(0.5));
        del.writeNumber("137");
        del.writeNull();
        del.writeBoolean(false);
        del.writeString("foo");

        // verify that we can actually set/get "current value" as expected, even with delegates
        assertNull(del.getCurrentValue());
        del.setCurrentValue(TOKEN);

        del.writeStartObject(null, 0);
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
        assertEquals("[13,1,0.5,137,null,false,\"foo\",{},[]]", sw.toString());

        g0.close();
    }

    public void testGeneratorDelegateArrays() throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g0 = JSON_F.createGenerator(sw);
        JsonGeneratorDelegate del = new JsonGeneratorDelegate(g0);

        final Object MARKER = new Object();
        del.writeStartArray(MARKER);
        assertSame(MARKER, del.getCurrentValue());

        del.writeArray(new int[] { 1, 2, 3 }, 0, 3);
        del.writeArray(new long[] { 1, 123456, 2 }, 1, 1);
        del.writeArray(new double[] { 0.25, 0.5, 0.75 }, 0, 2);
        del.writeArray(new String[] { "Aa", "Bb", "Cc" }, 1, 2);

        del.close();
        assertEquals("[[1,2,3],[123456],[0.25,0.5],[\"Bb\",\"Cc\"]]", sw.toString());

        g0.close();
    }

    public void testGeneratorDelegateComments() throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g0 = JSON_F.createGenerator(sw);
        JsonGeneratorDelegate del = new JsonGeneratorDelegate(g0);

        final Object MARKER = new Object();
        del.writeStartArray(MARKER, 5);
        assertSame(MARKER, del.getCurrentValue());

        del.writeNumber((short) 1);
        del.writeNumber(12L);
        del.writeNumber(0.25);
        del.writeNumber(0.5f);

        del.writeRawValue("/*foo*/");
        del.writeRaw("  ");

        del.close();
        assertEquals("[1,12,0.25,0.5,/*foo*/  ]", sw.toString());

        g0.close();
    }

    public void testDelegateCopyMethods() throws IOException
    {
        JsonParser p = JSON_F.createParser("[123,[true,false]]");
        StringWriter sw = new StringWriter();
        JsonGenerator g0 = JSON_F.createGenerator(sw);
        JsonGeneratorDelegate del = new JsonGeneratorDelegate(g0);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        del.copyCurrentEvent(p);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        del.copyCurrentStructure(p);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        del.copyCurrentEvent(p);
        g0.writeEndArray();

        del.close();
        g0.close();
        p.close();
        assertEquals("[123,false]", sw.toString());
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
