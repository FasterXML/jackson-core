package com.fasterxml.jackson.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.assertj.core.api.ThrowingConsumer;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.util.Separators.Spacing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultPrettyPrinterTest extends JUnit5TestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void systemLinefeed() throws IOException
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

    @Test
    void withLineFeed() throws IOException
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

    @Test
    void withIndent() throws IOException
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

    @Test
    void unixLinefeed() throws IOException
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

    @Test
    void windowsLinefeed() throws IOException
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

    @Test
    void tabIndent() throws IOException
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

    @Test
    void objectFieldValueSpacingAfter() throws IOException
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

    @Test
    void objectFieldValueSpacingNone() throws IOException
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

    @Test
    void copyConfigOld() throws IOException
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

    @Test
    void copyConfigNew() throws IOException
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

    @Test
    void rootSeparatorOld() throws IOException
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

    @Test
    void rootSeparator() throws IOException
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

    @Test
    void withoutSeparatorsOld() throws IOException
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

    @Test
    void withoutSeparatorsNew() throws IOException
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

    @Test
    void objectEmptySeparatorDefault() throws IOException
    {
        Separators separators = new Separators()
            .withRootSeparator(null)
            .withObjectFieldValueSpacing(Spacing.NONE);
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators)
                .withArrayIndenter(null)
                .withObjectIndenter(null);

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeStartObject();
            gen.writeFieldName("objectEmptySeparatorDefault");
            gen.writeStartObject();
            gen.writeEndObject();
            gen.writeEndObject();
        };
        
        assertEquals("{\"objectEmptySeparatorDefault\":{ }}", _printTestData(pp, false, writeTestData));
    }

    @Test
    void objectEmptySeparatorCustom() throws IOException
    {
        Separators separators = new Separators()
            .withRootSeparator(null)
            .withObjectFieldValueSpacing(Spacing.NONE)
            .withObjectEmptySeparator("    ");
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators)
                .withArrayIndenter(null)
                .withObjectIndenter(null);

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeStartObject();
            gen.writeFieldName("objectEmptySeparatorCustom");
            gen.writeStartObject();
            gen.writeEndObject();
            gen.writeEndObject();
        };
        
        assertEquals("{\"objectEmptySeparatorCustom\":{    }}", _printTestData(pp, false, writeTestData));
    }

    @Test
    void arrayEmptySeparatorDefault() throws IOException
    {
        Separators separators = new Separators()
            .withRootSeparator(null)
            .withObjectFieldValueSpacing(Spacing.NONE);
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators)
                .withArrayIndenter(null)
                .withObjectIndenter(null);

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeStartObject();
            gen.writeFieldName("arrayEmptySeparatorDefault");
            gen.writeStartArray();
            gen.writeEndArray();
            gen.writeEndObject();
        };
        
        assertEquals("{\"arrayEmptySeparatorDefault\":[ ]}", _printTestData(pp, false, writeTestData));
    }

    @Test
    void arrayEmptySeparatorCustom() throws IOException
    {
        Separators separators = new Separators()
            .withRootSeparator(null)
            .withObjectFieldValueSpacing(Spacing.NONE)
            .withArrayEmptySeparator("    ");
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators)
                .withArrayIndenter(null)
                .withObjectIndenter(null);

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeStartObject();
            gen.writeFieldName("arrayEmptySeparatorCustom");
            gen.writeStartArray();
            gen.writeEndArray();
            gen.writeEndObject();
        };
        
        assertEquals("{\"arrayEmptySeparatorCustom\":[    ]}", _printTestData(pp, false, writeTestData));
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
    @Test
    void invalidSubClass() throws Exception
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
