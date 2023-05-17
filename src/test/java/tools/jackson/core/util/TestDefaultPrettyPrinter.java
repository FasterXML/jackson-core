package tools.jackson.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;

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

    public void testRootSeparator() throws IOException
    {
        final String EXP = "1|2|3";

        ObjectWriteContext ppContext = new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() {
                return new DefaultPrettyPrinter()
                    .withRootSeparator("|");
            }
        };

        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ppContext, sw);

        gen.writeNumber(1);
        gen.writeNumber(2);
        gen.writeNumber(3);
        gen.close();
        assertEquals(EXP, sw.toString());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        gen = JSON_F.createGenerator(ppContext, bytes);

        gen.writeNumber(1);
        gen.writeNumber(2);
        gen.writeNumber(3);
        gen.close();
        assertEquals(EXP, bytes.toString("UTF-8"));

        // Also: let's try removing separator altogether
        ppContext = new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() {
                return new DefaultPrettyPrinter()
                        .withRootSeparator((String) null)
                        .withArrayIndenter(null)
                        .withObjectIndenter(null)
                        .withoutSpacesInObjectEntries();
            }
        };

        sw = new StringWriter();
        gen = JSON_F.createGenerator(ppContext, sw);

        gen.writeNumber(1);
        gen.writeStartArray();
        gen.writeNumber(2);
        gen.writeEndArray();
        gen.writeStartObject();
        gen.writeName("a");
        gen.writeNumber(3);
        gen.writeEndObject();
        gen.close();
        // no root separator, nor array, object
        assertEquals("1[2]{\"a\":3}", sw.toString());
    }

    private String _printTestData(final PrettyPrinter pp, boolean useBytes) throws IOException
    {
        JsonGenerator gen;
        StringWriter sw;
        ByteArrayOutputStream bytes;

        ObjectWriteContext ppContext = new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() { return pp; }
        };

        if (useBytes) {
            sw = null;
            bytes = new ByteArrayOutputStream();
            gen = JSON_F.createGenerator(ppContext, bytes);
        } else {
            sw = new StringWriter();
            bytes = null;
            gen = JSON_F.createGenerator(ppContext, sw);
        }

        gen.writeStartObject();
        gen.writeName("name");
        gen.writeString("John Doe");
        gen.writeName("age");
        gen.writeNumber(3.14);
        gen.writeEndObject();
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
