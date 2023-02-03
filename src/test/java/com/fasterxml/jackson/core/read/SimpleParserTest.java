package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.testsupport.MockDataInput;
import com.fasterxml.jackson.core.util.JsonParserDelegate;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
@SuppressWarnings("resource")
public class SimpleParserTest extends BaseTest
{
    public void testConfig() throws Exception
    {
        JsonParser p = createParser(MODE_READER, "[ ]");
        Object src = p.getInputSource();
        assertNotNull(src);
        assertTrue(src instanceof Reader);
        p.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        assertTrue(p.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        p.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        assertFalse(p.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));

        p.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        assertTrue(p.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        p.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        assertFalse(p.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        p.close();

        p = createParser(MODE_INPUT_STREAM, "[ ]");
        src = p.getInputSource();
        assertNotNull(src);
        assertTrue(src instanceof InputStream);
        p.close();

        p = createParser(MODE_DATA_INPUT, "[ ]");
        src = p.getInputSource();
        assertNotNull(src);
        assertTrue(src instanceof DataInput);
        p.close();
    }

    public void testInterningWithStreams() throws Exception
    {
        _testIntern(true, true, "a");
        _testIntern(true, false, "b");
    }

    public void testInterningWithReaders() throws Exception
    {
        _testIntern(false, true, "c");
        _testIntern(false, false, "d");
    }

    private void _testIntern(boolean useStream, boolean enableIntern, String expName) throws IOException
    {
        JsonFactory f = JsonFactory.builder()
                .configure(JsonFactory.Feature.INTERN_FIELD_NAMES, enableIntern)
                .build();
        assertEquals(enableIntern, f.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        final String JSON = "{ \""+expName+"\" : 1}";
        JsonParser p = useStream ?
            createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        // needs to be same of cours
        String actName = p.getCurrentName();
        assertEquals(expName, actName);
        if (enableIntern) {
            assertSame(expName, actName);
        } else {
            assertNotSame(expName, actName);
        }
        p.close();
    }

    /**
     * This basic unit test verifies that example given in the JSON
     * specification (RFC-4627 or later) is properly parsed at
     * high-level, without verifying values.
     */
    public void testSpecExampleSkipping() throws Exception
    {
        _doTestSpec(false);
    }

    /**
     * Unit test that verifies that the spec example JSON is completely
     * parsed, and proper values are given for contents of all
     * events/tokens.
     */
    public void testSpecExampleFully() throws Exception
    {
        _doTestSpec(true);
    }

    /**
     * Unit test that verifies that 3 basic keywords (null, true, false)
     * are properly parsed in various contexts.
     */
    public void testKeywords() throws Exception
    {
        final String DOC = "{\n"
            +"\"key1\" : null,\n"
            +"\"key2\" : true,\n"
            +"\"key3\" : false,\n"
            +"\"key4\" : [ false, null, true ]\n"
            +"}"
            ;

        JsonParser p = createParserUsingStream(JSON_FACTORY, DOC, "UTF-8");
        _testKeywords(p, true);
        p.close();

        p = createParserUsingReader(JSON_FACTORY, DOC);
        _testKeywords(p, true);
        p.close();

        p = createParserForDataInput(JSON_FACTORY, new MockDataInput(DOC));
        _testKeywords(p, false);
        p.close();
    }

    private void _testKeywords(JsonParser p, boolean checkColumn) throws Exception
    {
        JsonStreamContext ctxt = p.getParsingContext();
        assertEquals("/", ctxt.toString());
        assertTrue(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertFalse(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        // Before advancing to content, we should have following default state...
        assertFalse(p.hasCurrentToken());
        assertNull(p.getText());
        assertNull(p.getTextCharacters());
        assertEquals(0, p.getTextLength());
        // not sure if this is defined but:
        assertEquals(0, p.getTextOffset());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/", ctxt.toString());

        assertTrue(p.hasCurrentToken());
        JsonLocation loc = p.getTokenLocation();
        assertNotNull(loc);
        assertEquals(1, loc.getLineNr());
        if (checkColumn) {
            assertEquals(1, loc.getColumnNr());
        }

        ctxt = p.getParsingContext();
        assertFalse(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertTrue(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        verifyFieldName(p, "key1");
        assertEquals("{\"key1\"}", ctxt.toString());
        assertEquals(2, p.getTokenLocation().getLineNr());

        ctxt = p.getParsingContext();
        assertFalse(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertTrue(ctxt.inObject());
        assertEquals(1, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());
        assertEquals("key1", ctxt.getCurrentName());

        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertEquals("key1", ctxt.getCurrentName());

        ctxt = p.getParsingContext();
        assertEquals(1, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        verifyFieldName(p, "key2");
        ctxt = p.getParsingContext();
        assertEquals(2, ctxt.getEntryCount());
        assertEquals(1, ctxt.getCurrentIndex());
        assertEquals("key2", ctxt.getCurrentName());

        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("key2", ctxt.getCurrentName());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        verifyFieldName(p, "key3");
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        verifyFieldName(p, "key4");

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        ctxt = p.getParsingContext();
        assertTrue(ctxt.inArray());
        assertNull(ctxt.getCurrentName());
        assertEquals("key4", ctxt.getParent().getCurrentName());

        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals("[0]", ctxt.toString());

        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        ctxt = p.getParsingContext();
        assertTrue(ctxt.inObject());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        ctxt = p.getParsingContext();
        assertTrue(ctxt.inRoot());
        assertNull(ctxt.getCurrentName());
    }

    public void testSkipping() throws Exception {
        _testSkipping(MODE_INPUT_STREAM);
        _testSkipping(MODE_INPUT_STREAM_THROTTLED);
        _testSkipping(MODE_READER);
        _testSkipping(MODE_DATA_INPUT);
    }

    private void _testSkipping(int mode) throws Exception
    {
        // InputData has some limitations to take into consideration
        boolean isInputData = (mode == MODE_DATA_INPUT);
        String DOC = a2q(
            "[ 1, 3, [ true, null ], 3, { 'a\\\\b':'quoted: \\'stuff\\'' }, [ [ ] ], { } ]"
        );
        JsonParser p = createParser(mode, DOC);

        // First, skipping of the whole thing
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        p.skipChildren();
        assertEquals(JsonToken.END_ARRAY, p.currentToken());
        if (!isInputData) {
            JsonToken t = p.nextToken();
            if (t != null) {
                fail("Expected null at end of doc, got "+t);
            }
        }
        p.close();

        // Then individual ones
        p = createParser(mode, DOC);
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        p.skipChildren();
        // shouldn't move
        assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
        assertEquals(1, p.getIntValue());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        // then skip array
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        p.skipChildren();
        assertToken(JsonToken.END_ARRAY, p.currentToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        p.skipChildren();
        assertToken(JsonToken.END_OBJECT, p.currentToken());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        p.skipChildren();
        assertToken(JsonToken.END_ARRAY, p.currentToken());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        p.skipChildren();
        assertToken(JsonToken.END_OBJECT, p.currentToken());

        assertToken(JsonToken.END_ARRAY, p.nextToken());

        p.close();
    }

    public void testNameEscaping() throws IOException
    {
        _testNameEscaping(MODE_INPUT_STREAM);
        _testNameEscaping(MODE_READER);
        _testNameEscaping(MODE_DATA_INPUT);
    }

    private void _testNameEscaping(int mode) throws IOException
    {
        final Map<String,String> NAME_MAP = new LinkedHashMap<String,String>();
        NAME_MAP.put("", "");
        NAME_MAP.put("\\\"funny\\\"", "\"funny\"");
        NAME_MAP.put("\\\\", "\\");
        NAME_MAP.put("\\r", "\r");
        NAME_MAP.put("\\n", "\n");
        NAME_MAP.put("\\t", "\t");
        NAME_MAP.put("\\r\\n", "\r\n");
        NAME_MAP.put("\\\"\\\"", "\"\"");
        NAME_MAP.put("Line\\nfeed", "Line\nfeed");
        NAME_MAP.put("Yet even longer \\\"name\\\"!", "Yet even longer \"name\"!");

        int entry = 0;
        for (Map.Entry<String,String> en : NAME_MAP.entrySet()) {
            ++entry;
            String input = en.getKey();
            String expResult = en.getValue();
            final String DOC = "{ \""+input+"\":null}";
            JsonParser p = createParser(mode, DOC);

            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            // first, sanity check (field name == getText()
            String act = p.getCurrentName();
            assertEquals(act, getAndVerifyText(p));
            if (!expResult.equals(act)) {
                String msg = "Failed for name #"+entry+"/"+NAME_MAP.size();
                if (expResult.length() != act.length()) {
                    fail(msg+": exp length "+expResult.length()+", actual "+act.length());
                }
                assertEquals(msg, expResult, act);
            }
            assertToken(JsonToken.VALUE_NULL, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            p.close();
        }
    }

    /**
     * Unit test that verifies that long text segments are handled
     * correctly; mostly to stress-test underlying segment-based
     * text buffer(s).
     */
    public void testLongText() throws Exception {
        // lengths chosen to tease out problems with buffer allocation...
        _testLongText(310);
        _testLongText(7700);
        _testLongText(49000);
        _testLongText(96000);
    }

    private void _testLongText(int LEN) throws Exception
    {
        StringBuilder sb = new StringBuilder(LEN + 100);
        Random r = new Random(LEN);
        while (sb.length() < LEN) {
            sb.append(r.nextInt());
            sb.append(" xyz foo");
            if (r.nextBoolean()) {
                sb.append(" and \"bar\"");
            } else if (r.nextBoolean()) {
                sb.append(" [whatever].... ");
            } else {
                // Let's try some more 'exotic' chars
                sb.append(" UTF-8-fu: try this {\u00E2/\u0BF8/\uA123!} (look funny?)");
            }
            if (r.nextBoolean()) {
                if (r.nextBoolean()) {
                    sb.append('\n');
                } else if (r.nextBoolean()) {
                    sb.append('\r');
                } else {
                    sb.append("\r\n");
                }
            }
        }
        final String VALUE = sb.toString();

        // Let's use real generator to get JSON done right
        StringWriter sw = new StringWriter(LEN + (LEN >> 2));
        JsonGenerator g = JSON_FACTORY.createGenerator(sw);
        g.writeStartObject();
        g.writeFieldName("doc");
        g.writeString(VALUE);
        g.writeEndObject();
        g.close();

        final String DOC = sw.toString();

        for (int type = 0; type < 4; ++type) {
            JsonParser p;
            switch (type) {
            case MODE_INPUT_STREAM:
            case MODE_READER:
            case MODE_DATA_INPUT:
                p = createParser(type, DOC);
                break;
            default:
                p = JSON_FACTORY.createParser(encodeInUTF32BE(DOC));
            }
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("doc", p.getCurrentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());

            String act = getAndVerifyText(p);
            if (act.length() != VALUE.length()) {
                fail("Expected length "+VALUE.length()+", got "+act.length()+" (mode = "+type+")");
            }
            if (!act.equals(VALUE)) {
                fail("Long text differs");
            }

            // should still know the field name
            assertEquals("doc", p.getCurrentName());
            assertToken(JsonToken.END_OBJECT, p.nextToken());

            // InputDate somewhat special, so:
            if (type != MODE_DATA_INPUT) {
                assertNull(p.nextToken());
            }
            p.close();
        }
    }

    /**
     * Simple unit test that verifies that passing in a byte array
     * as source works as expected.
     */
    public void testBytesAsSource() throws Exception
    {
        String JSON = "[ 1, 2, 3, 4 ]";
        byte[] b = JSON.getBytes("UTF-8");
        int offset = 50;
        int len = b.length;
        byte[] src = new byte[offset + len + offset];

        System.arraycopy(b, 0, src, offset, len);

        JsonParser p = JSON_FACTORY.createParser(src, offset, len);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(4, p.getIntValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    public void testUtf8BOMHandling() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // first, write BOM:
        bytes.write(0xEF);
        bytes.write(0xBB);
        bytes.write(0xBF);
        bytes.write("[ 1 ]".getBytes("UTF-8"));
        byte[] input = bytes.toByteArray();

        JsonParser p = JSON_FACTORY.createParser(input);
        assertEquals(JsonToken.START_ARRAY, p.nextToken());

        JsonLocation loc = p.getTokenLocation();
        assertEquals(3, loc.getByteOffset());
        assertEquals(-1, loc.getCharOffset());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        p = JSON_FACTORY.createParser(new MockDataInput(input));
        assertEquals(JsonToken.START_ARRAY, p.nextToken());
        // same BOM, but DataInput is more restrictive so can skip but offsets
        // are not reliable...
        loc = p.getTokenLocation();
        assertNotNull(loc);
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    // [core#48]
    public void testSpacesInURL() throws Exception
    {
        File f = File.createTempFile("pre fix&stuff", ".txt");
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
        w.write("{ }");
        w.close();
        URL url = f.toURI().toURL();

        JsonParser p = JSON_FACTORY.createParser(url);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    public void testGetValueAsTextBytes() throws Exception
    {
        _testGetValueAsText(MODE_INPUT_STREAM, false);
        _testGetValueAsText(MODE_INPUT_STREAM, true);
    }

    public void testGetValueAsTextDataInput() throws Exception
    {
        _testGetValueAsText(MODE_DATA_INPUT, false);
        _testGetValueAsText(MODE_DATA_INPUT, true);
    }

    public void testGetValueAsTextChars() throws Exception
    {
        _testGetValueAsText(MODE_READER, false);
        _testGetValueAsText(MODE_READER, true);
    }

    private void _testGetValueAsText(int mode, boolean delegate) throws Exception
    {
        String JSON = "{\"a\":1,\"b\":true,\"c\":null,\"d\":\"foo\"}";
        JsonParser p = createParser(mode, JSON);
        if (delegate) {
            p = new JsonParserDelegate(p);
        }

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertNull(p.getValueAsString());
        assertEquals("foobar", p.getValueAsString("foobar"));

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.getText());
        assertEquals("a", p.getValueAsString());
        assertEquals("a", p.getValueAsString("default"));
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("1", p.getValueAsString());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getValueAsString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("true", p.getValueAsString());
        assertEquals("true", p.getValueAsString("foobar"));

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("c", p.getValueAsString());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        // null token returned as Java null, as per javadoc
        assertNull(p.getValueAsString());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("d", p.getValueAsString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("foo", p.getValueAsString("default"));
        assertEquals("foo", p.getValueAsString());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.getValueAsString());

        // InputData can't peek into end-of-input so:
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();
    }

    public void testGetTextViaWriter() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testGetTextViaWriter(mode);
        }
    }

    private void _testGetTextViaWriter(int mode) throws Exception
    {
        final String INPUT_TEXT = "this is a sample text for json parsing using readText() method";
        final String JSON = "{\"a\":\""+INPUT_TEXT+"\",\"b\":true,\"c\":null,\"d\":\"foobar!\"}";
        JsonParser parser = createParser(mode, JSON);
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        _getAndVerifyText(parser, "a");
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        _getAndVerifyText(parser, INPUT_TEXT);
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("b", parser.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, parser.nextToken());
        _getAndVerifyText(parser, "true");
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("c", parser.getCurrentName());
        assertToken(JsonToken.VALUE_NULL, parser.nextToken());
        _getAndVerifyText(parser, "null");
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("d", parser.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        _getAndVerifyText(parser, "foobar!");

        parser.close();
    }

    private void _getAndVerifyText(JsonParser p, String exp) throws Exception
    {
        Writer writer = new StringWriter();
        int len = p.getText(writer);
        String resultString = writer.toString();
        assertEquals(len, resultString.length());
        assertEquals(exp, resultString);
    }

    public void testLongerReadText() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testLongerReadText(mode);
        }
    }

    private void _testLongerReadText(int mode) throws Exception
    {
        StringBuilder builder = new StringBuilder();
        for(int i= 0; i < 1000; i++) {
            builder.append("Sample Text"+i);
        }
        String longText = builder.toString();
        final String JSON = "{\"a\":\""+ longText +"\",\"b\":true,\"c\":null,\"d\":\"foo\"}";
        JsonParser parser = createParser(MODE_READER, JSON);
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());

        Writer writer = new StringWriter();
        int len = parser.getText(writer);
        String resultString = writer.toString();
        assertEquals(len, resultString.length());
        assertEquals(longText, resultString);
        parser.close();
    }

    /*
    /**********************************************************
    /* Tests for Invalid input
    /**********************************************************
     */

    // [core#142]
    public void testHandlingOfInvalidSpaceByteStream() throws Exception {
        _testHandlingOfInvalidSpace(MODE_INPUT_STREAM);
        _testHandlingOfInvalidSpaceFromResource(true);
    }

    // [core#142]
    public void testHandlingOfInvalidSpaceChars() throws Exception {
        _testHandlingOfInvalidSpace(MODE_READER);
        _testHandlingOfInvalidSpaceFromResource(false);
    }

    // [core#142]
    public void testHandlingOfInvalidSpaceDataInput() throws Exception {
        _testHandlingOfInvalidSpace(MODE_DATA_INPUT);
    }

    private void _testHandlingOfInvalidSpace(int mode) throws Exception
    {
        final String JSON = "{ \u00A0 \"a\":1}";

        JsonParser p = createParser(mode, JSON);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Should have failed");
        } catch (JsonParseException e) {
            verifyException(e, "unexpected character");
            // and correct error code
            verifyException(e, "code 160");
        }
        p.close();
    }

    private void _testHandlingOfInvalidSpaceFromResource(boolean useStream) throws Exception
    {
        InputStream in = getClass().getResourceAsStream("/test_0xA0.json");
        JsonParser p = useStream
                ? JSON_FACTORY.createParser(in)
                : JSON_FACTORY.createParser(new InputStreamReader(in, "UTF-8"));
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("request", p.getCurrentName());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("mac", p.getCurrentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertNotNull(p.getText());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("data", p.getCurrentName());
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            // ... and from there on, just loop

            while (p.nextToken()  != null) { }
            fail("Should have failed");
        } catch (JsonParseException e) {
            verifyException(e, "unexpected character");
            // and correct error code
            verifyException(e, "code 160");
        }
        p.close();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _doTestSpec(boolean verify) throws IOException
    {
        JsonParser p;

        // First, using a StringReader:
        p = createParserUsingReader(JSON_FACTORY, SAMPLE_DOC_JSON_SPEC);
        verifyJsonSpecSampleDoc(p, verify);
        p.close();

        // Then with streams using supported encodings:
        p = createParserUsingStream(JSON_FACTORY, SAMPLE_DOC_JSON_SPEC, "UTF-8");
        verifyJsonSpecSampleDoc(p, verify);
        p.close();
        p = createParserUsingStream(JSON_FACTORY, SAMPLE_DOC_JSON_SPEC, "UTF-16BE");
        verifyJsonSpecSampleDoc(p, verify);
        p.close();
        p = createParserUsingStream(JSON_FACTORY, SAMPLE_DOC_JSON_SPEC, "UTF-16LE");
        verifyJsonSpecSampleDoc(p, verify);
        p.close();

        // Hmmh. UTF-32 is harder only because JDK doesn't come with
        // a codec for it. Can't test it yet using this method
        p = createParserUsingStream(JSON_FACTORY, SAMPLE_DOC_JSON_SPEC, "UTF-32");
        verifyJsonSpecSampleDoc(p, verify);
        p.close();

        // and finally, new (as of May 2016) source, DataInput:
        p = createParserForDataInput(JSON_FACTORY, new MockDataInput(SAMPLE_DOC_JSON_SPEC));
        verifyJsonSpecSampleDoc(p, verify);
        p.close();
    }
}
