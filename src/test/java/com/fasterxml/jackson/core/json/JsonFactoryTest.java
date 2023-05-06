package com.fasterxml.jackson.core.json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;

public class JsonFactoryTest
    extends com.fasterxml.jackson.core.BaseTest
{
    static class BogusCodec extends ObjectCodec {
        @Override
        public Version version() { return null; }

        @Override
        public <T> T readValue(JsonParser p, Class<T> valueType) throws IOException {
            return null;
        }

        @Override
        public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef) throws IOException {
            return null;
        }

        @Override
        public <T> T readValue(JsonParser p, ResolvedType valueType) throws IOException {
            return null;
        }

        @Override
        public <T> Iterator<T> readValues(JsonParser p, Class<T> valueType) throws IOException {
            return null;
        }

        @Override
        public <T> Iterator<T> readValues(JsonParser p, TypeReference<T> valueTypeRef) throws IOException {
            return null;
        }

        @Override
        public <T> Iterator<T> readValues(JsonParser p, ResolvedType valueType) throws IOException {
            return null;
        }

        @Override
        public void writeValue(JsonGenerator gen, Object value) throws IOException {
        }

        @Override
        public <T extends TreeNode> T readTree(JsonParser p) throws IOException {
            return null;
        }

        @Override
        public void writeTree(JsonGenerator gen, TreeNode tree) throws IOException {
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
        public <T> T treeToValue(TreeNode n, Class<T> valueType) throws JsonProcessingException {
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
    }

    // for testing [core#460]
    @SuppressWarnings("serial")
    static class CustomFactory extends JsonFactory {
        public CustomFactory(JsonFactory f, ObjectCodec codec) {
            super(f, codec);
        }
    }

    static class BogusSchema implements FormatSchema
    {
        @Override
        public String getSchemaType() {
            return "test";
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    final JsonFactory JSON_F = sharedStreamFactory();

    @SuppressWarnings("deprecation")
    public void testGeneratorFeatures() throws Exception
    {
        assertNull(JSON_F.getCodec());

        JsonFactory f = JsonFactory.builder()
                .configure(JsonWriteFeature.QUOTE_FIELD_NAMES, true)
                .build();
        // 24-Oct-2018, tatu: Until 3.x, we'll only have backwards compatible
        assertTrue(f.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        f = JsonFactory.builder()
                .configure(JsonWriteFeature.QUOTE_FIELD_NAMES, false)
                .build();
        assertFalse(f.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
    }

    public void testFactoryFeatures() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .configure(JsonFactory.Feature.INTERN_FIELD_NAMES, false)
                .build();
        assertFalse(f.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));

        // by default, should be enabled
        assertTrue(f.isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING));

        assertFalse(JSON_F.requiresCustomCodec());
        assertFalse(JSON_F.canHandleBinaryNatively());
    }

    public void testFactoryMisc() throws Exception
    {
        assertNull(JSON_F.getInputDecorator());
        assertNull(JSON_F.getOutputDecorator());

        assertFalse(JSON_F.canUseSchema(null));
        assertFalse(JSON_F.canUseSchema(new BogusSchema()));

        assertNull(JSON_F.getFormatReadFeatureType());
        assertNull(JSON_F.getFormatWriteFeatureType());

        assertEquals(0, JSON_F.getFormatParserFeatures());
        assertEquals(0, JSON_F.getFormatGeneratorFeatures());
    }

    // for [core#189]: verify that it's ok to disable recycling
    // Basically simply exercises basic functionality, to ensure
    // there are no obvious problems; needed since testing never
    // disables this handling otherwise
    public void testDisablingBufferRecycling() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .disable(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING)
                .build();
        assertFalse(f.isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING));

        // First, generation
        for (int i = 0; i < 3; ++i) {
            StringWriter w = new StringWriter();
            JsonGenerator gen = f.createGenerator(w);
            gen.writeStartObject();
            gen.writeEndObject();
            gen.close();
            assertEquals("{}", w.toString());
        }

        for (int i = 0; i < 3; ++i) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            JsonGenerator gen = f.createGenerator(bytes);
            gen.writeStartArray();
            gen.writeEndArray();
            gen.close();
            assertEquals("[]", bytes.toString("UTF-8"));
        }

        // Then parsing:
        for (int i = 0; i < 3; ++i) {
            JsonParser p = f.createParser("{}");
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.nextToken());
            p.close();

            p = f.createParser("{}".getBytes("UTF-8"));
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.nextToken());
            p.close();
        }
    }

    public void testJsonWithFiles() throws Exception
    {
        File file = File.createTempFile("jackson-test", null);
        file.deleteOnExit();

        JsonFactory f = new JsonFactory();

        // First: create file via generator.. and use an odd encoding
        JsonGenerator jg = f.createGenerator(file, JsonEncoding.UTF16_LE);
        jg.writeStartObject();
        jg.writeRaw("   ");
        jg.writeEndObject();
        jg.close();

        // Ok: first read file directly
        JsonParser jp = f.createParser(file);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();

        // Then via URL:
        jp = f.createParser(file.toURI().toURL());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();

        // ok, delete once we are done
        file.delete();
    }

    // #72
    @SuppressWarnings("deprecation")
    public void testCopy() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        // first, verify defaults
        assertNull(jf.getCodec());
        assertTrue(jf.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        assertFalse(jf.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertFalse(jf.isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII));

        // then change, verify that changes "stick"
        jf = JsonFactory.builder()
                .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
                .build();
        ObjectCodec codec = new BogusCodec();
        jf.setCodec(codec);

        assertFalse(jf.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        assertTrue(jf.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertTrue(jf.isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII));
        assertSame(codec, jf.getCodec());

        JsonFactory jf2 = jf.copy();
        assertFalse(jf2.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        assertTrue(jf2.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertTrue(jf2.isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII));
        // 16-May-2018, tatu: But! Note that despited [core#460], this should NOT copy it back
        assertNull(jf2.getCodec());

        // However: real copy constructor SHOULD copy it
        JsonFactory jf3 = new CustomFactory(jf, codec);
        assertSame(codec, jf3.getCodec());
    }

    public void testRootValues() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertEquals(" ", f.getRootValueSeparator());
        f.setRootValueSeparator("/");
        assertEquals("/", f.getRootValueSeparator());

        // but also test it is used
        StringWriter w = new StringWriter();
        JsonGenerator g = f.createGenerator(w);
        g.writeNumber(1);
        g.writeNumber(2);
        g.writeNumber(3);
        g.close();
        assertEquals("1/2/3", w.toString());
    }

    public void testCanonicalizationEnabled() throws Exception {
        doCanonicalizationTest(false);
    }

    public void testCanonicalizationDisabled() throws Exception {
        doCanonicalizationTest(false);
    }

    // Configure the JsonFactory as expected, and verify across common shapes of input
    // to cover common JsonParser implementations.
    private void doCanonicalizationTest(boolean canonicalize) throws Exception {
        String contents = "{\"a\":true,\"a\":true}";
        JsonFactory factory = JsonFactory.builder()
                .configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, canonicalize)
                .build();
        try (JsonParser parser = factory.createParser(contents)) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (JsonParser parser = factory.createParser(contents.getBytes(StandardCharsets.UTF_8))) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (JsonParser parser = factory.createParser(
                new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)))) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (JsonParser parser = factory.createParser(new StringReader(contents))) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (JsonParser parser = factory.createParser((DataInput) new DataInputStream(
                new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))))) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (NonBlockingJsonParser parser = (NonBlockingJsonParser) factory.createNonBlockingByteArrayParser()) {
            byte[] data = contents.getBytes(StandardCharsets.UTF_8);
            parser.feedInput(data, 0, data.length);
            parser.endOfInput();
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
    }

    private void verifyCanonicalizationTestResult(JsonParser parser, boolean canonicalize) throws Exception {
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        String field1 = parser.nextFieldName();
        assertEquals("a", field1);
        assertToken(JsonToken.VALUE_TRUE, parser.nextToken());
        String field2 = parser.nextFieldName();
        assertEquals("a", field2);
        if (canonicalize) {
            assertSame(field1, field2);
        } else {
            // n.b. It's possible that this may flake if a garbage collector with string deduplication
            // enabled is used. Such a failure is unlikely because younger GC generations are typically
            // not considered for deduplication due to high churn, but under heavy memory pressure it
            // may be possible. I've left this comment in an attempt to simplify investigation in the
            // off-chance that such flakes eventually occur.
            assertNotSame(field1, field2);
        }
        assertToken(JsonToken.VALUE_TRUE, parser.nextToken());
        assertToken(JsonToken.END_OBJECT, parser.nextToken());
    }
}
