package com.fasterxml.jackson.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.assertj.core.api.ThrowingConsumer;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.Separators.Spacing;

public class TestDefaultPrettyPrinter extends BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testSystemLinefeed() throws IOException
    {
        PrettyPrinter pp = new DefaultPrettyPrinter();
        String LF = System.getProperty("line.separator");
        String EXP = "{" + LF +
            "  \"name\" : \"John Doe\"," + LF +
            "  \"age\" : 3.14" + LF +
            "}";
        assertEquals(EXP, _printTestData(pp, false));
        assertEquals(EXP, _printTestData(pp, true));
    }

    public void testWithLineFeed() throws IOException
    {
        PrettyPrinter pp = new DefaultPrettyPrinter()
        .withObjectIndenter(new DefaultIndenter().withLinefeed("\n"));
        String EXP = "{\n" +
            "  \"name\" : \"John Doe\",\n" +
            "  \"age\" : 3.14\n" +
            "}";
        assertEquals(EXP, _printTestData(pp, false));
        assertEquals(EXP, _printTestData(pp, true));
    }

    public void testWithIndent() throws IOException
    {
        PrettyPrinter pp = new DefaultPrettyPrinter()
        .withObjectIndenter(new DefaultIndenter().withLinefeed("\n").withIndent(" "));
        String EXP = "{\n" +
            " \"name\" : \"John Doe\",\n" +
            " \"age\" : 3.14\n" +
            "}";
        assertEquals(EXP, _printTestData(pp, false));
        assertEquals(EXP, _printTestData(pp, true));
    }

    public void testUnixLinefeed() throws IOException
    {
        PrettyPrinter pp = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("  ", "\n"));
        String EXP = "{\n" +
            "  \"name\" : \"John Doe\",\n" +
            "  \"age\" : 3.14\n" +
            "}";
        assertEquals(EXP, _printTestData(pp, false));
        assertEquals(EXP, _printTestData(pp, true));
    }

    public void testWindowsLinefeed() throws IOException
    {
        PrettyPrinter pp = new DefaultPrettyPrinter()
        .withObjectIndenter(new DefaultIndenter("  ", "\r\n"));
        String EXP = "{\r\n" +
            "  \"name\" : \"John Doe\",\r\n" +
            "  \"age\" : 3.14\r\n" +
            "}";
        assertEquals(EXP, _printTestData(pp, false));
        assertEquals(EXP, _printTestData(pp, true));
    }

    public void testTabIndent() throws IOException
    {
        PrettyPrinter pp = new DefaultPrettyPrinter()
        .withObjectIndenter(new DefaultIndenter("\t", "\n"));
        String EXP = "{\n" +
            "\t\"name\" : \"John Doe\",\n" +
            "\t\"age\" : 3.14\n" +
            "}";
        assertEquals(EXP, _printTestData(pp, false));
        assertEquals(EXP, _printTestData(pp, true));
    }
    
    public void testObjectFieldValueSpacingAfter() throws IOException
    {
        Separators separators = new Separators()
                .withObjectFieldValueSpacing(Spacing.AFTER);
        PrettyPrinter pp = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("  ", "\n"))
                .withSeparators(separators);
        String EXP = "{\n" +
                "  \"name\": \"John Doe\",\n" +
                "  \"age\": 3.14\n" +
                "}";
        assertEquals(EXP, _printTestData(pp, false));
        assertEquals(EXP, _printTestData(pp, true));
    }
    
    public void testObjectFieldValueSpacingNone() throws IOException
    {
        Separators separators = new Separators()
                .withObjectFieldValueSpacing(Spacing.NONE);
        PrettyPrinter pp = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("  ", "\n"))
                .withSeparators(separators);
        String EXP = "{\n" +
                "  \"name\":\"John Doe\",\n" +
                "  \"age\":3.14\n" +
                "}";
        assertEquals(EXP, _printTestData(pp, false));
        assertEquals(EXP, _printTestData(pp, true));
    }
    
    public void testCopyConfigOld() throws IOException
    {
        Separators separators = new Separators()
                .withObjectFieldValueSpacing(Spacing.AFTER)
                .withObjectEntrySpacing(Spacing.AFTER)
                .withArrayValueSpacing(Spacing.AFTER);
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("    ", "\n"))
                .withSeparators(separators);
        String expected = _printTestData(pp, false);
        assertEquals(expected, _printTestData(pp, true));
        
        @SuppressWarnings("deprecation") // Testing the old API
        DefaultPrettyPrinter copy = pp.withRootSeparator(DefaultPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR);
        assertEquals(expected, _printTestData(copy, false));
        assertEquals(expected, _printTestData(copy, true));
    }
    
    public void testCopyConfigNew() throws IOException
    {
        Separators separators = new Separators()
                .withObjectFieldValueSpacing(Spacing.AFTER)
                .withObjectEntrySpacing(Spacing.AFTER)
                .withArrayValueSpacing(Spacing.AFTER);
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("    ", "\n"))
                .withSeparators(separators);
        String expected = _printTestData(pp, false);
        assertEquals(expected, _printTestData(pp, true));
        
        DefaultPrettyPrinter copy = new DefaultPrettyPrinter(pp);
        assertEquals(expected, _printTestData(copy, false));
        assertEquals(expected, _printTestData(copy, true));
    }

    public void testRootSeparatorOld() throws IOException
    {
        @SuppressWarnings("deprecation") // Testing the old API
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
            .withRootSeparator("|");
        final String EXP = "1|2|3";

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeNumber(1);
            gen.writeNumber(2);
            gen.writeNumber(3);
        };

        assertEquals(EXP, _printTestData(pp, false, writeTestData));
        assertEquals(EXP, _printTestData(pp, true, writeTestData));
    }
    
    public void testRootSeparator() throws IOException
    {
        Separators separators = new Separators()
                .withRootSeparator("|");
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators);
        final String EXP = "1|2|3";
        
        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeNumber(1);
            gen.writeNumber(2);
            gen.writeNumber(3);
        };
        
        assertEquals(EXP, _printTestData(pp, false, writeTestData));
        assertEquals(EXP, _printTestData(pp, true, writeTestData));
    }

    public void testWithoutSeparatorsOld() throws IOException
    {
        // Also: let's try removing separator altogether
        @SuppressWarnings("deprecation") // Testing the old API
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withRootSeparator((String) null)
                .withArrayIndenter(null)
                .withObjectIndenter(null)
                .withoutSpacesInObjectEntries();

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeNumber(1);
            gen.writeStartArray();
            gen.writeNumber(2);
            gen.writeEndArray();
            gen.writeStartObject();
            gen.writeFieldName("a");
            gen.writeNumber(3);
            gen.writeEndObject();
        };
        // no root separator, nor array, object
        assertEquals("1[2]{\"a\":3}", _printTestData(pp, false, writeTestData));
    }
    
    public void testWithoutSeparatorsNew() throws IOException
    {
        Separators separators = new Separators()
                .withRootSeparator(null)
                .withObjectFieldValueSpacing(Spacing.NONE);
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators)
                .withArrayIndenter(null)
                .withObjectIndenter(null);

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeNumber(1);
            gen.writeStartArray();
            gen.writeNumber(2);
            gen.writeEndArray();
            gen.writeStartObject();
            gen.writeFieldName("a");
            gen.writeNumber(3);
            gen.writeEndObject();
        };
        // no root separator, nor array, object
        assertEquals("1[2]{\"a\":3}", _printTestData(pp, false, writeTestData));
    }

    private String _printTestData(PrettyPrinter pp, boolean useBytes) throws IOException
    {
        return _printTestData(pp, useBytes, gen -> {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString("John Doe");
            gen.writeFieldName("age");
            gen.writeNumber(3.14);
            gen.writeEndObject();       
        });
    }
    
    private String _printTestData(PrettyPrinter pp, boolean useBytes, ThrowingConsumer<JsonGenerator> writeTestData) throws IOException
    {
        JsonGenerator gen;
        StringWriter sw;
        ByteArrayOutputStream bytes;
        
        if (useBytes) {
            sw = null;
            bytes = new ByteArrayOutputStream();
            gen = JSON_F.createGenerator(bytes);
        } else {
            sw = new StringWriter();
            bytes = null;
            gen = JSON_F.createGenerator(sw);
        }
        gen.setPrettyPrinter(pp);
        writeTestData.accept(gen);
        gen.close();
        
        if (useBytes) {
            return bytes.toString("UTF-8");
        }
        return sw.toString();
    }

    // [core#502]: Force sub-classes to reimplement `createInstance`
    public void testInvalidSubClass() throws Exception
    {
        DefaultPrettyPrinter pp = new MyPrettyPrinter();
        try {
            pp.createInstance();
            fail("Should not pass");
        } catch (IllegalStateException e) {
            verifyException(e, "does not override");
        }
    }

    @SuppressWarnings("serial")
    static class MyPrettyPrinter extends DefaultPrettyPrinter { }
}
