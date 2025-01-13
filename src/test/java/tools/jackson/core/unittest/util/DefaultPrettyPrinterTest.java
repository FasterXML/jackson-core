package tools.jackson.core.unittest.util;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.core.util.Separators.Spacing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultPrettyPrinterTest extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void systemLinefeed() throws Exception
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
    void withLineFeed() throws Exception
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
    void withIndent() throws Exception
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
    void unixLinefeed() throws Exception
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
    void windowsLinefeed() throws Exception
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
    void tabIndent() throws Exception
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
    void objectNameValueSpacingAfter() throws Exception
    {
        Separators separators = new Separators()
                .withObjectNameValueSpacing(Spacing.AFTER);
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
    void objectNameValueSpacingNone() throws Exception
    {
        Separators separators = new Separators()
                .withObjectNameValueSpacing(Spacing.NONE);
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
    void copyConfigNew() throws Exception
    {
        Separators separators = new Separators()
                .withObjectNameValueSpacing(Spacing.AFTER)
                .withObjectEntrySpacing(Spacing.AFTER)
                .withArrayElementSpacing(Spacing.AFTER);
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
    void rootSeparator() throws Exception
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
    void withoutSeparatorsNew() throws Exception
    {
        Separators separators = new Separators()
                .withRootSeparator(null)
                .withObjectNameValueSpacing(Spacing.NONE);
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
            gen.writeName("a");
            gen.writeNumber(3);
            gen.writeEndObject();
        };
        // no root separator, nor array, object
        assertEquals("1[2]{\"a\":3}", _printTestData(pp, false, writeTestData));
    }

    @Test
    void objectEmptySeparatorDefault() throws Exception
    {
        Separators separators = new Separators()
            .withRootSeparator(null)
            .withObjectNameValueSpacing(Spacing.NONE);
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators)
                .withArrayIndenter(null)
                .withObjectIndenter(null);

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeStartObject();
            gen.writeName("objectEmptySeparatorDefault");
            gen.writeStartObject();
            gen.writeEndObject();
            gen.writeEndObject();
        };
        
        assertEquals("{\"objectEmptySeparatorDefault\":{ }}", _printTestData(pp, false, writeTestData));
    }

    @Test
    void objectEmptySeparatorCustom() throws Exception
    {
        Separators separators = new Separators()
            .withRootSeparator(null)
            .withObjectNameValueSpacing(Spacing.NONE)
            .withObjectEmptySeparator("    ");
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators)
                .withArrayIndenter(null)
                .withObjectIndenter(null);

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeStartObject();
            gen.writeName("objectEmptySeparatorCustom");
            gen.writeStartObject();
            gen.writeEndObject();
            gen.writeEndObject();
        };
        
        assertEquals("{\"objectEmptySeparatorCustom\":{    }}",
                _printTestData(pp, false, writeTestData));
    }

    @Test
    void arrayEmptySeparatorDefault() throws Exception
    {
        Separators separators = new Separators()
            .withRootSeparator(null)
            .withObjectNameValueSpacing(Spacing.NONE);
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators)
                .withArrayIndenter(null)
                .withObjectIndenter(null);

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeStartObject();
            gen.writeName("arrayEmptySeparatorDefault");
            gen.writeStartArray();
            gen.writeEndArray();
            gen.writeEndObject();
        };
        
        assertEquals("{\"arrayEmptySeparatorDefault\":[ ]}", _printTestData(pp, false, writeTestData));
    }

    @Test
    void arrayEmptySeparatorCustom() throws Exception
    {
        Separators separators = new Separators()
            .withRootSeparator(null)
            .withObjectNameValueSpacing(Spacing.NONE)
            .withArrayEmptySeparator("    ");
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter()
                .withSeparators(separators)
                .withArrayIndenter(null)
                .withObjectIndenter(null);

        ThrowingConsumer<JsonGenerator> writeTestData = gen -> {
            gen.writeStartObject();
            gen.writeName("arrayEmptySeparatorCustom");
            gen.writeStartArray();
            gen.writeEndArray();
            gen.writeEndObject();
        };
        
        assertEquals("{\"arrayEmptySeparatorCustom\":[    ]}",
                _printTestData(pp, false, writeTestData));
    }

    private String _printTestData(final PrettyPrinter pp, boolean useBytes) throws Exception
    {
        return _printTestData(pp, useBytes, gen -> {
            gen.writeStartObject();
            gen.writeName("name");
            gen.writeString("John Doe");
            gen.writeName("age");
            gen.writeNumber(3.14);
            gen.writeEndObject();       
        });
    }
    
    private String _printTestData(PrettyPrinter pp, boolean useBytes, ThrowingConsumer<JsonGenerator> writeTestData)
        throws Exception
    {
        JsonGenerator gen;
        StringWriter sw;
        ByteArrayOutputStream bytes;

        ObjectWriteContext ppContext = objectWriteContext(pp);

        if (useBytes) {
            sw = null;
            bytes = new ByteArrayOutputStream();
            gen = JSON_F.createGenerator(ppContext, bytes);
        } else {
            sw = new StringWriter();
            bytes = null;
            gen = JSON_F.createGenerator(ppContext, sw);
        }

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

    private ObjectWriteContext objectWriteContext(PrettyPrinter pp) {
        return new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() { return pp; }
        };
    }
}
