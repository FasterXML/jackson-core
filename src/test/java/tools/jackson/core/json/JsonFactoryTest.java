package tools.jackson.core.json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import tools.jackson.core.*;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.async.NonBlockingByteArrayJsonParser;

public class JsonFactoryTest
    extends tools.jackson.core.BaseTest
{
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

    private final JsonFactory JSON_F = newStreamFactory();

    public void testStreamWriteFeatures() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(StreamWriteFeature.IGNORE_UNKNOWN)
                .build();
        assertTrue(f.isEnabled(StreamWriteFeature.IGNORE_UNKNOWN));
        f = f.rebuild().configure(StreamWriteFeature.IGNORE_UNKNOWN, false)
                .build();
        assertFalse(f.isEnabled(StreamWriteFeature.IGNORE_UNKNOWN));
    }

    public void testJsonWriteFeatures() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonWriteFeature.QUOTE_PROPERTY_NAMES)
                .build();
        assertTrue(f.isEnabled(JsonWriteFeature.QUOTE_PROPERTY_NAMES));
        f = f.rebuild().configure(JsonWriteFeature.QUOTE_PROPERTY_NAMES, false)
                .build();
        assertFalse(f.isEnabled(JsonWriteFeature.QUOTE_PROPERTY_NAMES));
    }

    public void testFactoryFeatures() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(TokenStreamFactory.Feature.INTERN_PROPERTY_NAMES)
                .build();
        assertTrue(f.isEnabled(JsonFactory.Feature.INTERN_PROPERTY_NAMES));

        f = f.rebuild()
                .disable(JsonFactory.Feature.INTERN_PROPERTY_NAMES)
                .build();
        assertFalse(f.isEnabled(JsonFactory.Feature.INTERN_PROPERTY_NAMES));

        // by default, should be enabled
        assertTrue(f.isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING));
        f = f.rebuild().disable(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING)
                .build();
        assertFalse(f.isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING));

        assertFalse(f.canHandleBinaryNatively());
    }

    public void testFactoryMisc() throws Exception
    {
        assertNull(JSON_F.getInputDecorator());
        assertNull(JSON_F.getOutputDecorator());

        assertFalse(JSON_F.canUseSchema(null));
        assertFalse(JSON_F.canUseSchema(new BogusSchema()));

        assertEquals(JsonReadFeature.class, JSON_F.getFormatReadFeatureType());
        assertEquals(JsonWriteFeature.class, JSON_F.getFormatWriteFeatureType());
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

        // First, generation
        for (int i = 0; i < 3; ++i) {
            StringWriter w = new StringWriter();
            JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), w);
            gen.writeStartObject();
            gen.writeEndObject();
            gen.close();
            assertEquals("{}", w.toString());
        }

        for (int i = 0; i < 3; ++i) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), bytes);
            gen.writeStartArray();
            gen.writeEndArray();
            gen.close();
            assertEquals("[]", bytes.toString("UTF-8"));
        }

        // Then parsing:
        for (int i = 0; i < 3; ++i) {
            JsonParser p = f.createParser(ObjectReadContext.empty(), "{}");
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.nextToken());
            p.close();

            p = f.createParser(ObjectReadContext.empty(), "{}".getBytes("UTF-8"));
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
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), file, JsonEncoding.UTF16_LE);
        g.writeStartObject();
        g.writeRaw("   ");
        g.writeEndObject();
        g.close();

        // Ok: first read file directly
        JsonParser p = f.createParser(ObjectReadContext.empty(), file);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();

        // Then via URL:
        p = f.createParser(ObjectReadContext.empty(), file.toURI().toURL());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();

        // ok, delete once we are done
        file.delete();
    }

    public void testCopy() throws Exception
    {
        JsonFactory f = new JsonFactory();
        // first, verify defaults

        // since 3.0:
        assertFalse(f.isEnabled(JsonFactory.Feature.INTERN_PROPERTY_NAMES));
        assertFalse(f.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        assertFalse(f.isEnabled(JsonWriteFeature.ESCAPE_NON_ASCII));

        f = f.rebuild()
                .enable(JsonFactory.Feature.INTERN_PROPERTY_NAMES)
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
                .build();
        // then change, verify that changes "stick"
        assertTrue(f.isEnabled(JsonFactory.Feature.INTERN_PROPERTY_NAMES));
        assertTrue(f.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        assertTrue(f.isEnabled(JsonWriteFeature.ESCAPE_NON_ASCII));

        JsonFactory jf2 = f.copy();
        assertTrue(jf2.isEnabled(JsonFactory.Feature.INTERN_PROPERTY_NAMES));
        assertTrue(f.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        assertTrue(f.isEnabled(JsonWriteFeature.ESCAPE_NON_ASCII));
    }

    public void testRootValues() throws Exception
    {
        assertEquals(" ", JSON_F.getRootValueSeparator());
        JsonFactoryBuilder b = JsonFactory.builder()
                .rootValueSeparator("/");
        assertEquals(new SerializedString("/"), b.rootValueSeparator());
        JsonFactory f = b.build();
        assertEquals("/", f.getRootValueSeparator());

        // but also test it is used
        StringWriter w = new StringWriter();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), w);
        g.writeNumber(1);
        g.writeNumber(2);
        g.writeNumber(3);
        g.close();
        assertEquals("1/2/3", w.toString());
    }

    public void test_createGenerator_OutputStream() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = new JsonFactory()
                .createGenerator(ObjectWriteContext.empty(), outputStream);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the stream has not been closed by close
        outputStream.write(1);
    }

    public void test_createGenerator_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        JsonGenerator jsonGenerator = new JsonFactory()
                .createGenerator(ObjectWriteContext.empty(), path.toFile(), JsonEncoding.UTF8);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_createGenerator_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        JsonGenerator jsonGenerator = new JsonFactory()
                .createGenerator(ObjectWriteContext.empty(), path, JsonEncoding.UTF8);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_createGenerator_Writer() throws Exception
    {
        Writer writer = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory()
                .createGenerator(ObjectWriteContext.empty(), writer);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(writer.toString(), "\"value\"");

        // the writer has not been closed by close
        writer.append('1');
    }

    public void test_createGenerator_DataOutput() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);
        JsonGenerator jsonGenerator = new JsonFactory()
                .createGenerator(ObjectWriteContext.empty(), dataOutput);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the data output has not been closed by close
        dataOutput.write(1);
    }

    public void test_createParser_InputStream() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = new JsonFactory()
                .createParser(ObjectReadContext.empty(), inputStream);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = new JsonFactory()
                .createParser(ObjectReadContext.empty(), path.toFile());

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = new JsonFactory()
                .createParser(ObjectReadContext.empty(), path);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_Url() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = new JsonFactory()
                .createParser(ObjectReadContext.empty(), path.toUri().toURL());

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_Reader() throws Exception
    {
        Reader reader = new StringReader("\"value\"");
        JsonParser jsonParser = new JsonFactory()
                .createParser(ObjectReadContext.empty(), reader);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_ByteArray() throws Exception
    {
        byte[] bytes = "\"value\"".getBytes(StandardCharsets.UTF_8);
        JsonParser jsonParser = new JsonFactory()
                .createParser(ObjectReadContext.empty(), bytes);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_String() throws Exception
    {
        String string = "\"value\"";
        JsonParser jsonParser = new JsonFactory()
                .createParser(ObjectReadContext.empty(), string);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_CharArray() throws Exception
    {
        char[] chars = "\"value\"".toCharArray();
        JsonParser jsonParser = new JsonFactory()
                .createParser(ObjectReadContext.empty(), chars);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_DataInput() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        DataInput dataInput = new DataInputStream(inputStream);
        JsonParser jsonParser = new JsonFactory()
                .createParser(ObjectReadContext.empty(), dataInput);

        assertEquals(jsonParser.nextTextValue(), "value");
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
                .configure(JsonFactory.Feature.CANONICALIZE_PROPERTY_NAMES, canonicalize)
                .build();
        try (JsonParser parser = factory.createParser(ObjectReadContext.empty(), contents)) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (JsonParser parser = factory.createParser(ObjectReadContext.empty(), 
                contents.getBytes(StandardCharsets.UTF_8))) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (JsonParser parser = factory.createParser(ObjectReadContext.empty(),
                new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)))) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (JsonParser parser = factory.createParser(ObjectReadContext.empty(), 
                new StringReader(contents))) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (JsonParser parser = factory.createParser(ObjectReadContext.empty(),
                (DataInput) new DataInputStream(
                new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))))) {
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
        try (NonBlockingByteArrayJsonParser parser = (NonBlockingByteArrayJsonParser) factory.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            byte[] data = contents.getBytes(StandardCharsets.UTF_8);
            parser.feedInput(data, 0, data.length);
            parser.endOfInput();
            verifyCanonicalizationTestResult(parser, canonicalize);
        }
    }

    private void verifyCanonicalizationTestResult(JsonParser parser, boolean canonicalize) throws Exception {
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        String field1 = parser.nextName();
        assertEquals("a", field1);
        assertToken(JsonToken.VALUE_TRUE, parser.nextToken());
        String field2 = parser.nextName();
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
