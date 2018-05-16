package com.fasterxml.jackson.core.json;

import java.io.*;
import java.util.Iterator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;

public class JsonFactoryTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private static class BogusCodec extends ObjectCodec {
        @Override
        public Version version() { return null; }

        @Override
        public <T> T readValue(JsonParser p, Class<T> valueType) throws IOException {
            return null;
        }

        @Override
        public <T> T readValue(JsonParser p, TypeReference<?> valueTypeRef) throws IOException {
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
        public <T> Iterator<T> readValues(JsonParser p, TypeReference<?> valueTypeRef) throws IOException {
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
    }

    // for testing [core#460]
    @SuppressWarnings("serial")
    static class CustomFactory extends JsonFactory {
        public CustomFactory(JsonFactory f, ObjectCodec codec) {
            super(f, codec);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */
    
    public void testGeneratorFeatures() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertNull(f.getCodec());

        f.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        assertTrue(f.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        f.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
        assertFalse(f.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
    }

    public void testFactoryFeatures() throws Exception
    {
        JsonFactory f = new JsonFactory();

        f.configure(JsonFactory.Feature.INTERN_FIELD_NAMES, true);
        assertTrue(f.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        f.configure(JsonFactory.Feature.INTERN_FIELD_NAMES, false);
        assertFalse(f.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));

        // by default, should be enabled
        assertTrue(f.isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING));
        f.configure(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING, false);
        assertFalse(f.isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING));
    }

    // for [core#189]: verify that it's ok to disable recycling
    // Basically simply exercises basic functionality, to ensure
    // there are no obvious problems; needed since testing never
    // disables this handling otherwise
    public void testDisablingBufferRecycling() throws Exception
    {
        JsonFactory f = new JsonFactory();

        f.disable(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING);

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
    public void testCopy() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        // first, verify defaults
        assertNull(jf.getCodec());
        assertTrue(jf.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        assertFalse(jf.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertFalse(jf.isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII));

        // then change, verify that changes "stick"
        jf.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
        jf.enable(JsonParser.Feature.ALLOW_COMMENTS);
        jf.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
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
}
