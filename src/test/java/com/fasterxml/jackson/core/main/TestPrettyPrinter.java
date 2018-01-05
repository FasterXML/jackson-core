package com.fasterxml.jackson.core.main;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

import java.io.*;

/**
 * Set of basic unit tests for verifying that indenting
 * option of generator works correctly
 */
@SuppressWarnings("serial")
public class TestPrettyPrinter
    extends com.fasterxml.jackson.core.BaseTest
{
    static class CountPrinter extends MinimalPrettyPrinter
    {
        @Override
        public void writeEndObject(JsonGenerator jg, int nrOfEntries)
                throws IOException, JsonGenerationException
        {
            jg.writeRaw("("+nrOfEntries+")}");
        }

        @Override
        public void writeEndArray(JsonGenerator jg, int nrOfValues)
            throws IOException, JsonGenerationException
        {
            jg.writeRaw("("+nrOfValues+")]");
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    public void testObjectCount() throws Exception
    {
        final String EXP = "{\"x\":{\"a\":1,\"b\":2(2)}(1)}";
        final JsonFactory f = new JsonFactory();

        for (int i = 0; i < 2; ++i) {
            boolean useBytes = (i > 0);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            StringWriter sw = new StringWriter();
            JsonGenerator gen = useBytes ? f.createGenerator(ObjectWriteContext.empty(), bytes)
                    : f.createGenerator(ObjectWriteContext.empty(), sw);
            gen.setPrettyPrinter(new CountPrinter());
            gen.writeStartObject();
            gen.writeFieldName("x");
            gen.writeStartObject();
            gen.writeNumberField("a", 1);
            gen.writeNumberField("b", 2);
            gen.writeEndObject();
            gen.writeEndObject();
            gen.close();

            String json = useBytes ? bytes.toString("UTF-8") : sw.toString();
            assertEquals(EXP, json);
        }
    }

    public void testArrayCount() throws Exception
    {
        final String EXP = "[6,[1,2,9(3)](2)]";
        
        final JsonFactory f = new JsonFactory();

        for (int i = 0; i < 2; ++i) {
            boolean useBytes = (i > 0);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            StringWriter sw = new StringWriter();
            JsonGenerator gen = useBytes ? f.createGenerator(ObjectWriteContext.empty(), bytes)
                    : f.createGenerator(ObjectWriteContext.empty(), sw);
            gen.setPrettyPrinter(new CountPrinter());
            gen.writeStartArray();
            gen.writeNumber(6);
            gen.writeStartArray();
            gen.writeNumber(1);
            gen.writeNumber(2);
            gen.writeNumber(9);
            gen.writeEndArray();
            gen.writeEndArray();
            gen.close();

            String json = useBytes ? bytes.toString("UTF-8") : sw.toString();
            assertEquals(EXP, json);
        }
    }
    
    public void testSimpleDocWithDefault() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.useDefaultPrettyPrinter();
        _verifyPrettyPrinter(gen, sw);
        gen.close();
    }

    @SuppressWarnings("resource")
    public void testSimpleDocWithMinimal() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        // first with standard minimal
        gen.setPrettyPrinter(new MinimalPrettyPrinter());
        String docStr = _verifyPrettyPrinter(gen, sw);
        // which should have no linefeeds, tabs
        assertEquals(-1, docStr.indexOf('\n'));
        assertEquals(-1, docStr.indexOf('\t'));

        // And then with slightly customized variant
        gen = new JsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.setPrettyPrinter(new MinimalPrettyPrinter() {
            @Override
            // use TAB between array values
            public void beforeArrayValues(JsonGenerator jg) throws IOException, JsonGenerationException
            {
                jg.writeRaw("\t");
            }
        });
        docStr = _verifyPrettyPrinter(gen, sw);
        assertEquals(-1, docStr.indexOf('\n'));
        assertTrue(docStr.indexOf('\t') >= 0);
        gen.close();
    }

    // [Issue#26]
    public void testCustomRootSeparatorWithPP() throws Exception
    {
        JsonFactory f = new JsonFactory();
        // first, no pretty-printing (will still separate root values with a space!)
        assertEquals("{} {} []", _generateRoot(f, null));
        // First with default pretty printer, default configs:
        assertEquals("{ } { } [ ]", _generateRoot(f, new DefaultPrettyPrinter()));
        // then custom:
        assertEquals("{ }|{ }|[ ]", _generateRoot(f, new DefaultPrettyPrinter("|")));
    }

    // Alternative solution for [jackson-core#26]
    public void testCustomRootSeparatorWithFactory() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .rootValueSeparator("##")
                .build();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeNumber(13);
        gen.writeBoolean(false);
        gen.writeNull();
        gen.close();
        assertEquals("13##false##null", sw.toString());
    }

    public void testCustomSeparatorsWithMinimal() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.setPrettyPrinter(new MinimalPrettyPrinter().setSeparators(Separators.createDefaultInstance()
                .withObjectFieldValueSeparator('=')
                .withObjectEntrySeparator(';')
                .withArrayValueSeparator('|')));
        _writeTestDocument(gen);
        gen.close();
        assertEquals("[3|\"abc\"|[true]|{\"f\"=null;\"f2\"=null}]", sw.toString());
    }

    public void testCustomSeparatorsWithPP() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.setPrettyPrinter(new DefaultPrettyPrinter().withSeparators(Separators.createDefaultInstance()
                .withObjectFieldValueSeparator('=')
                .withObjectEntrySeparator(';')
                .withArrayValueSeparator('|')));

        _writeTestDocument(gen);
        gen.close();
        assertEquals("[ 3| \"abc\"| [ true ]| {" + DefaultIndenter.SYS_LF +
                "  \"f\" = null;" + DefaultIndenter.SYS_LF +
                "  \"f2\" = null" + DefaultIndenter.SYS_LF +
                "} ]", sw.toString());
    }

    public void testCustomSeparatorsWithPPWithoutSpaces() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.setPrettyPrinter(new DefaultPrettyPrinter().withSeparators(Separators.createDefaultInstance()
                .withObjectFieldValueSeparator('=')
                .withObjectEntrySeparator(';')
                .withArrayValueSeparator('|'))
            .withoutSpacesInObjectEntries());

        _writeTestDocument(gen);
        gen.close();
        assertEquals("[ 3| \"abc\"| [ true ]| {" + DefaultIndenter.SYS_LF +
                "  \"f\"=null;" + DefaultIndenter.SYS_LF +
                "  \"f2\"=null" + DefaultIndenter.SYS_LF +
                "} ]", sw.toString());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private String _verifyPrettyPrinter(JsonGenerator gen, StringWriter sw) throws Exception
    {
        _writeTestDocument(gen);

        String docStr = sw.toString();
        JsonParser jp = createParserUsingReader(docStr);

        assertEquals(JsonToken.START_ARRAY, jp.nextToken());

        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(3, jp.getIntValue());
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("abc", jp.getText());

        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(JsonToken.VALUE_TRUE, jp.nextToken());
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());

        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("f", jp.getText());
        assertEquals(JsonToken.VALUE_NULL, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("f2", jp.getText());
        assertEquals(JsonToken.VALUE_NULL, jp.nextToken());
        assertEquals(JsonToken.END_OBJECT, jp.nextToken());

        assertEquals(JsonToken.END_ARRAY, jp.nextToken());

        jp.close();

        return docStr;
    }

    private void _writeTestDocument(JsonGenerator gen) throws IOException {
        gen.writeStartArray();
        gen.writeNumber(3);
        gen.writeString("abc");

        gen.writeStartArray();
        gen.writeBoolean(true);
        gen.writeEndArray();

        gen.writeStartObject();
        gen.writeFieldName("f");
        gen.writeNull();
        gen.writeFieldName("f2");
        gen.writeNull();
        gen.writeEndObject();

        gen.writeEndArray();
        gen.close();
    }

    protected String _generateRoot(TokenStreamFactory f, PrettyPrinter pp) throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.setPrettyPrinter(pp);
        gen.writeStartObject();
        gen.writeEndObject();
        gen.writeStartObject();
        gen.writeEndObject();
        gen.writeStartArray();
        gen.writeEndArray();
        gen.close();
        return sw.toString();
    }
}
