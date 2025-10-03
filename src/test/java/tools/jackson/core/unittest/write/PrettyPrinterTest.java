package tools.jackson.core.unittest.write;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;
import tools.jackson.core.util.*;
import tools.jackson.core.util.Separators.Spacing;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Set of basic unit tests for verifying that indenting
 * option of generator works correctly
 */
@SuppressWarnings("serial")
class PrettyPrinterTest
    extends JacksonCoreTestBase
{
    static class CountPrinter extends MinimalPrettyPrinter
    {
        @Override
        public void writeEndObject(JsonGenerator jg, int nrOfEntries)
        {
            jg.writeRaw("("+nrOfEntries+")}");
        }

        @Override
        public void writeEndArray(JsonGenerator jg, int nrOfValues)
        {
            jg.writeRaw("("+nrOfValues+")]");
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void objectCount() throws Exception
    {
        final String EXP = "{\"x\":{\"a\":1,\"b\":2(2)}(1)}";

        for (int i = 0; i < 2; ++i) {
            boolean useBytes = (i > 0);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            StringWriter sw = new StringWriter();

            ObjectWriteContext ppContext = new ObjectWriteContext.Base() {
                @Override
                public PrettyPrinter getPrettyPrinter() { return new CountPrinter(); }
            };

            JsonGenerator gen = useBytes ? JSON_F.createGenerator(ppContext, bytes)
                    : JSON_F.createGenerator(ppContext, sw);
            gen.writeStartObject();
            gen.writeName("x");
            gen.writeStartObject();
            gen.writeNumberProperty("a", 1);
            gen.writeNumberProperty("b", 2);
            gen.writeEndObject();
            gen.writeEndObject();
            gen.close();

            String json = useBytes ? bytes.toString("UTF-8") : sw.toString();
            assertEquals(EXP, json);
        }
    }

    @Test
    void arrayCount() throws Exception
    {
        final String EXP = "[6,[1,2,9(3)](2)]";

        for (int i = 0; i < 2; ++i) {
            boolean useBytes = (i > 0);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            StringWriter sw = new StringWriter();
            ObjectWriteContext ppContext = new ObjectWriteContext.Base() {
                @Override
                public PrettyPrinter getPrettyPrinter() { return new CountPrinter(); }
            };

            JsonGenerator gen = useBytes ? JSON_F.createGenerator(ppContext, bytes)
                    : JSON_F.createGenerator(ppContext, sw);
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

    @SuppressWarnings("resource")
    @Test
    void simpleDocWithMinimal() throws Exception
    {
        final PrettyPrinter MINIMAL = new MinimalPrettyPrinter();
        StringWriter sw = new StringWriter();
        // first with standard minimal
        ObjectWriteContext ppContext = new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() { return MINIMAL; }
        };
        JsonGenerator gen = JSON_F.createGenerator(ppContext, sw);

        // Verify instance uses stateless PP
        assertSame(MINIMAL, gen.getPrettyPrinter());

        String docStr = _verifyPrettyPrinter(gen, sw);
        // which should have no linefeeds, tabs
        assertEquals(-1, docStr.indexOf('\n'));
        assertEquals(-1, docStr.indexOf('\t'));

        // And then with slightly customized variant
        ppContext = new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() {
                return new MinimalPrettyPrinter() {
                    @Override
                    // use TAB between array values
                    public void beforeArrayValues(JsonGenerator g)
                    {
                        g.writeRaw("\t");
                    }
                };
            }
        };

        gen = new JsonFactory().createGenerator(ppContext, sw);
        docStr = _verifyPrettyPrinter(gen, sw);
        assertEquals(-1, docStr.indexOf('\n'));
        assertTrue(docStr.indexOf('\t') >= 0);
        gen.close();
    }

    // [core#26]
    @Test
    void rootSeparatorWithoutPP() throws Exception
    {
        // no pretty-printing (will still separate root values with a space!)
        assertEquals("{} {} []", _generateRoot(JSON_F, null));
    }

    // [core#26]
    @Test
    void defaultRootSeparatorWithPP() throws Exception
    {
        assertEquals("{ } { } [ ]", _generateRoot(JSON_F, new DefaultPrettyPrinter()));
    }

    // [core#26]
    @Test
    void customRootSeparatorWithPPNew() throws Exception
    {
        Separators separators = Separators.createDefaultInstance()
                .withRootSeparator("|");
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(separators);
        assertEquals("{ }|{ }|[ ]", _generateRoot(JSON_F, pp));
    }

    // Alternative solution for [jackson-core#26]
    @Test
    void customRootSeparatorWithFactory() throws Exception
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

    @Test
    void customSeparatorsWithMinimal() throws Exception
    {
        StringWriter sw = new StringWriter();
        ObjectWriteContext ppContext = new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() {
                return new MinimalPrettyPrinter().setSeparators(Separators.createDefaultInstance()
                        .withObjectNameValueSeparator('=')
                        .withObjectEntrySeparator(';')
                        .withArrayElementSeparator('|'));
            }
        };
        JsonGenerator gen = JSON_F.createGenerator(ppContext, sw);
        _writeTestDocument(gen);
        gen.close();
        assertEquals("[3|\"abc\"|[true]|{\"f\"=null;\"f2\"=null}]", sw.toString());

        // and with byte-backed too
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        gen = JSON_F.createGenerator(ppContext, bytes);
        _writeTestDocument(gen);
        gen.close();

        assertEquals("[3|\"abc\"|[true]|{\"f\"=null;\"f2\"=null}]", bytes.toString("UTF-8"));
    }

    @Test
    void customSeparatorsWithPP() throws Exception
    {
        StringWriter sw = new StringWriter();
        ObjectWriteContext ppContext = new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() {
                return new DefaultPrettyPrinter().withSeparators(Separators.createDefaultInstance()
                        .withObjectNameValueSeparator('=')
                        .withObjectEntrySeparator(';')
                        .withArrayElementSeparator('|'));
            }
        };

        JsonGenerator gen = new JsonFactory().createGenerator(ppContext, sw);
        _writeTestDocument(gen);
        gen.close();
        assertEquals("[ 3| \"abc\"| [ true ]| {" + DefaultIndenter.SYS_LF +
                "  \"f\" = null;" + DefaultIndenter.SYS_LF +
                "  \"f2\" = null" + DefaultIndenter.SYS_LF +
                "} ]", sw.toString());
    }

    private static final String EXPECTED_CUSTOM_SEPARATORS_WITH_PP_WITHOUT_SPACES =
            "[ 3| \"abc\"| [ true ]| {" + DefaultIndenter.SYS_LF +
            "  \"f\"=null;" + DefaultIndenter.SYS_LF +
            "  \"f2\"=null" + DefaultIndenter.SYS_LF +
            "} ]";

    @Test
    void customSeparatorsWithPPWithoutSpacesNew() throws Exception
    {
        final Separators separators = Separators.createDefaultInstance()
                .withObjectNameValueSeparator('=')
                .withObjectNameValueSpacing(Spacing.NONE)
                .withObjectEntrySeparator(';')
                .withArrayElementSeparator('|');
        ObjectWriteContext ppContext = new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() {
                return new DefaultPrettyPrinter(separators);
            }
        };

        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = new JsonFactory().createGenerator(ppContext, sw)) {
            _writeTestDocument(gen);
        }
        assertEquals(EXPECTED_CUSTOM_SEPARATORS_WITH_PP_WITHOUT_SPACES, sw.toString());
    }

    // [core#1480]: access to configured/actual PrettyPrinter
    @Test
    void accessToPrettyPrinterInUse() throws Exception
    {
        // By default, no pretty-printer
        try (JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(),
                new StringWriter())) {
            assertNull(gen.getPrettyPrinter());
        }

        // But we can configure Default PP
        final PrettyPrinter DEFAULT_PP = new DefaultPrettyPrinter();
        try (JsonGenerator gen = JSON_F.createGenerator(objectWriteContext(DEFAULT_PP),
                new StringWriter())) {
            PrettyPrinter pp = gen.getPrettyPrinter();
            assertNotNull(pp);
            // Note: stateful, new instance created by our OWC
            assertEquals(DEFAULT_PP.getClass(), pp.getClass());
            assertNotSame(DEFAULT_PP, pp);
        }    
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
        assertEquals("abc", jp.getString());

        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(JsonToken.VALUE_TRUE, jp.nextToken());
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());

        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        assertEquals(JsonToken.PROPERTY_NAME, jp.nextToken());
        assertEquals("f", jp.getString());
        assertEquals(JsonToken.VALUE_NULL, jp.nextToken());
        assertEquals(JsonToken.PROPERTY_NAME, jp.nextToken());
        assertEquals("f2", jp.getString());
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
        gen.writeName("f");
        gen.writeNull();
        // for better test coverage also use alt method
        gen.writeName(new SerializedString("f2"));
        gen.writeNull();
        gen.writeEndObject();

        gen.writeEndArray();
        gen.close();
    }

    protected String _generateRoot(TokenStreamFactory f, PrettyPrinter pp) throws IOException
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = f.createGenerator(objectWriteContext(pp), sw)) {
            gen.writeStartObject();
            gen.writeEndObject();
            gen.writeStartObject();
            gen.writeEndObject();
            gen.writeStartArray();
            gen.writeEndArray();
        }
        return sw.toString();
    }

    protected ObjectWriteContext objectWriteContext(final PrettyPrinter pp) {
        return new ObjectWriteContext.Base() {
            @SuppressWarnings("unchecked")
            @Override
            public PrettyPrinter getPrettyPrinter() {
                if (pp instanceof Instantiatable) {
                    return ((Instantiatable<PrettyPrinter>) pp).createInstance();
                }
                return pp;
            }
        };
    }
}
