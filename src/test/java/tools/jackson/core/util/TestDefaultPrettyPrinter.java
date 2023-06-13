package tools.jackson.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.util.Separators.Spacing;

import org.assertj.core.api.ThrowingConsumer;

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
    
    public void testObjectNameValueSpacingAfter() throws IOException
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
    
    public void testObjectNameValueSpacingNone() throws IOException
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

    public void testCopyConfigNew() throws IOException
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

    public void testWithoutSeparatorsNew() throws IOException
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

    private String _printTestData(final PrettyPrinter pp, boolean useBytes) throws IOException
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
    
    private String _printTestData(PrettyPrinter pp, boolean useBytes, ThrowingConsumer<JsonGenerator> writeTestData) throws IOException
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

    // [core#502]: Force sub-classes to re-implement `createInstance`
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

    private ObjectWriteContext objectWriteContext(PrettyPrinter pp) {
        return new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() { return pp; }
        };
    }
}
