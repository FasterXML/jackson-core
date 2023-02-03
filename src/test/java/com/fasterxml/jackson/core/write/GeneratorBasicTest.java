package com.fasterxml.jackson.core.write;

import com.fasterxml.jackson.core.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Set of basic unit tests for verifying that the basic generator
 * functionality works as expected.
 */
public class GeneratorBasicTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    // // // First, tests for primitive (non-structured) values

    public void testStringWrite() throws Exception
    {
        String[] inputStrings = new String[] { "", "X", "1234567890" };
        for (int useReader = 0; useReader < 2; ++useReader) {
            for (int writeString = 0; writeString < 2; ++writeString) {
                for (int strIx = 0; strIx < inputStrings.length; ++strIx) {
                    String input = inputStrings[strIx];
                    JsonGenerator gen;
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    if (useReader != 0) {
                        gen = JSON_F.createGenerator(new OutputStreamWriter(bout, "UTF-8"));
                    } else {
                        gen = JSON_F.createGenerator(bout, JsonEncoding.UTF8);
                    }
                    if (writeString > 0) {
                        gen.writeString(input);
                    } else {
                        int len = input.length();
                        char[] buffer = new char[len + 20];
                        // Let's use non-zero base offset too...
                        input.getChars(0, len, buffer, strIx);
                        gen.writeString(buffer, strIx, len);
                    }
                    gen.flush();
                    gen.close();
                    JsonParser jp = JSON_F.createParser(new ByteArrayInputStream(bout.toByteArray()));

                    JsonToken t = jp.nextToken();
                    assertNotNull("Document \""+utf8String(bout)+"\" yielded no tokens", t);
                    assertEquals(JsonToken.VALUE_STRING, t);
                    assertEquals(input, jp.getText());
                    assertEquals(null, jp.nextToken());
                    jp.close();
                }
            }
        }
    }

    public void testIntValueWrite() throws Exception
    {
        // char[]
        doTestIntValueWrite(false, false);
        doTestIntValueWrite(true, false);
        // byte[]
        doTestIntValueWrite(false, true);
        doTestIntValueWrite(true, true);
    }

    public void testLongValueWrite() throws Exception
    {
        // char[]
        doTestLongValueWrite(false, false);
        doTestLongValueWrite(true, false);
        // byte[]
        doTestLongValueWrite(false, true);
        doTestLongValueWrite(true, true);
    }

    public void testBooleanWrite() throws Exception
    {
        for (int i = 0; i < 4; ++i) {
            boolean state = (i & 1) == 0;
            boolean pad = (i & 2) == 0;
            StringWriter sw = new StringWriter();
            JsonGenerator gen = JSON_F.createGenerator(sw);
            gen.writeBoolean(state);
            if (pad) {
                gen.writeRaw(" ");
            }
            gen.close();
            String docStr = sw.toString();
            JsonParser jp = createParserUsingReader(docStr);
            JsonToken t = jp.nextToken();
            String exp = Boolean.valueOf(state).toString();
            if (!exp.equals(jp.getText())) {
                fail("Expected '"+exp+"', got '"+jp.getText());
            }
            assertEquals(state ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE, t);
            assertEquals(null, jp.nextToken());
            jp.close();
        }
    }

    public void testNullWrite()
        throws Exception
    {
        for (int i = 0; i < 2; ++i) {
            boolean pad = (i & 1) == 0;
            StringWriter sw = new StringWriter();
            JsonGenerator gen = JSON_F.createGenerator(sw);
            gen.writeNull();
            if (pad) {
                gen.writeRaw(" ");
            }
            gen.close();
            String docStr = sw.toString();
            JsonParser jp = createParserUsingReader(docStr);
            JsonToken t = jp.nextToken();
            String exp = "null";
            if (!exp.equals(jp.getText())) {
                fail("Expected '"+exp+"', got '"+jp.getText());
            }
            assertEquals(JsonToken.VALUE_NULL, t);
            assertEquals(null, jp.nextToken());
            jp.close();
        }
    }

    // // Then root-level output testing

    public void testRootIntsWrite() throws Exception {
        _testRootIntsWrite(false);
        _testRootIntsWrite(true);
    }

    private void _testRootIntsWrite(boolean useBytes) throws Exception
    {
         StringWriter sw = new StringWriter();
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         JsonGenerator gen;

         if (useBytes) {
             gen = JSON_F.createGenerator(bytes);
         } else {
             gen = JSON_F.createGenerator(sw);
         }

         gen.writeNumber(1);
         gen.writeNumber((short) 2); // for test coverage
         gen.writeNumber(-13);
         gen.close();

         String docStr = useBytes ? utf8String(bytes) : sw.toString();

         try {
             JsonParser jp = createParserUsingReader(docStr);
             assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
             assertEquals(1, jp.getIntValue());
             assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
             assertEquals(2, jp.getIntValue());
             assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
             assertEquals(-13, jp.getIntValue());
             jp.close();
         } catch (IOException e) {
             fail("Problem with document ["+docStr+"]: "+e.getMessage());
         }
     }

    // Convenience methods

    public void testFieldValueWrites() throws Exception {
        _testFieldValueWrites(false);
        _testFieldValueWrites(true);
    }

    public void _testFieldValueWrites(boolean useBytes) throws Exception
    {
        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator gen;

        if (useBytes) {
            gen = JSON_F.createGenerator(bytes);
        } else {
            gen = JSON_F.createGenerator(sw);
        }

        gen.writeStartObject();
        gen.writeNumberField("short", (short) 3);
        gen.writeNumberField("int", 3);
        gen.writeNumberField("long", 3L);
        gen.writeNumberField("big", new BigInteger("1707"));
        gen.writeNumberField("double", 0.25);
        gen.writeNumberField("float", -0.25f);
        gen.writeNumberField("decimal", new BigDecimal("17.07"));
        gen.writeEndObject();
        gen.close();

        String docStr = useBytes ? utf8String(bytes) : sw.toString();

        assertEquals("{\"short\":3,\"int\":3,\"long\":3,\"big\":1707,\"double\":0.25,\"float\":-0.25,\"decimal\":17.07}",
                docStr.trim());
    }

    /**
     * Test to verify that output context actually contains useful information
     */
    public void testOutputContext() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(sw);
        JsonStreamContext ctxt = gen.getOutputContext();
        assertTrue(ctxt.inRoot());

        gen.writeStartObject();
        assertTrue(gen.getOutputContext().inObject());

        gen.writeFieldName("a");
        assertEquals("a", gen.getOutputContext().getCurrentName());

        gen.writeStartArray();
        assertTrue(gen.getOutputContext().inArray());

        gen.writeStartObject();
        assertTrue(gen.getOutputContext().inObject());

        gen.writeFieldName("b");
        ctxt = gen.getOutputContext();
        assertEquals("b", ctxt.getCurrentName());
        gen.writeNumber(123);
        assertEquals("b", ctxt.getCurrentName());

        gen.writeFieldName("c");
        assertEquals("c", gen.getOutputContext().getCurrentName());
        gen.writeNumber(5);
//        assertEquals("c", gen.getOutputContext().getCurrentName());

        gen.writeFieldName("d");
        assertEquals("d", gen.getOutputContext().getCurrentName());

        gen.writeStartArray();
        ctxt = gen.getOutputContext();
        assertTrue(ctxt.inArray());
        assertEquals(0, ctxt.getCurrentIndex());
        assertEquals(0, ctxt.getEntryCount());

        gen.writeBoolean(true);
        ctxt = gen.getOutputContext();
        assertTrue(ctxt.inArray());
        // NOTE: index still refers to currently output entry
        assertEquals(0, ctxt.getCurrentIndex());
        assertEquals(1, ctxt.getEntryCount());

        gen.writeNumber(3);
        ctxt = gen.getOutputContext();
        assertTrue(ctxt.inArray());
        assertEquals(1, ctxt.getCurrentIndex());
        assertEquals(2, ctxt.getEntryCount());

        gen.writeEndArray();
        assertTrue(gen.getOutputContext().inObject());

        gen.writeEndObject();
        assertTrue(gen.getOutputContext().inArray());

        gen.writeEndArray();
        assertTrue(gen.getOutputContext().inObject());

        gen.writeEndObject();

        assertTrue(gen.getOutputContext().inRoot());

        gen.close();
    }

    public void testGetOutputTarget() throws Exception
    {
        OutputStream out = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_F.createGenerator(out);
        assertSame(out, gen.getOutputTarget());
        gen.close();

        StringWriter sw = new StringWriter();
        gen = JSON_F.createGenerator(sw);
        assertSame(sw, gen.getOutputTarget());
        gen.close();
    }

    // for [core#195]
    public void testGetOutputBufferd() throws Exception
    {
        OutputStream out = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_F.createGenerator(out);
        _testOutputBuffered(gen);
        gen.close();

        StringWriter sw = new StringWriter();
        gen = JSON_F.createGenerator(sw);
        _testOutputBuffered(gen);
        gen.close();
    }

    private void _testOutputBuffered(JsonGenerator gen) throws IOException
    {
        gen.writeStartArray(); // 1 byte
        gen.writeNumber(1234); // 4 bytes
        assertEquals(5, gen.getOutputBuffered());
        gen.flush();
        assertEquals(0, gen.getOutputBuffered());
        gen.writeEndArray();
        assertEquals(1, gen.getOutputBuffered());
        gen.close();
        assertEquals(0, gen.getOutputBuffered());
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private void doTestIntValueWrite(boolean pad, boolean useBytes) throws Exception
    {
        int[] VALUES = new int[] {
            0, 1, -9, 32, -32, 57, 189, 2017, -9999, 13240, 123456,
            1111111, 22222222, 123456789,
            7300999, -7300999,
            99300999, -99300999,
            999300999, -999300999,
            1000300999, 2000500126, -1000300999, -2000500126,
            Integer.MIN_VALUE, Integer.MAX_VALUE
        };
        for (int i = 0; i < VALUES.length; ++i) {
            int VALUE = VALUES[i];
            String docStr;
            JsonParser p;

            if (useBytes) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                JsonGenerator gen = JSON_F.createGenerator(bytes);
                gen.writeNumber(VALUE);
                if (pad) {
                    gen.writeRaw(" ");
                }
                gen.close();
                docStr = utf8String(bytes);
                p = JSON_F.createParser(bytes.toByteArray());
            } else {
                StringWriter sw = new StringWriter();
                JsonGenerator gen = JSON_F.createGenerator(sw);
                gen.writeNumber(VALUE);
                if (pad) {
                    gen.writeRaw(" ");
                }
                gen.close();
                docStr = sw.toString();
                p = JSON_F.createParser(docStr);
            }
            JsonToken t = null;
            try {
                t = p.nextToken();
            } catch (IOException e) {
                fail("Problem with value "+VALUE+", document ["+docStr+"]: "+e.getMessage());
            }
            assertNotNull("Document \""+docStr+"\" yielded no tokens", t);
            // Number are always available as lexical representation too
            String exp = ""+VALUE;
            if (!exp.equals(p.getText())) {
                fail("Expected '"+exp+"', got '"+p.getText());
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(VALUE, p.getIntValue());
            assertEquals(null, p.nextToken());
            p.close();
        }
    }

    private void doTestLongValueWrite(boolean pad, boolean useBytes) throws Exception
    {
        long[] VALUES = new long[] {
            0L, 1L, -1L, 2000100345, -12005002294L,
            5111222333L, -5111222333L,
            65111222333L, -65111222333L,
            123456789012L, -123456789012L,
            123456789012345L, -123456789012345L,
            123456789012345789L, -123456789012345789L,
            Long.MIN_VALUE, Long.MAX_VALUE
        };
        for (int i = 0; i < VALUES.length; ++i) {
            long VALUE = VALUES[i];
            String docStr;
            JsonParser p;

            if (useBytes) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                JsonGenerator gen = JSON_F.createGenerator(bytes);
                gen.writeNumber(VALUE);
                if (pad) {
                    gen.writeRaw(" ");
                }
                gen.close();
                docStr = utf8String(bytes);
                p = JSON_F.createParser(bytes.toByteArray());
            } else {
                StringWriter sw = new StringWriter();
                JsonGenerator gen = JSON_F.createGenerator(sw);
                gen.writeNumber(VALUE);
                if (pad) {
                    gen.writeRaw(" ");
                }
                gen.close();
                docStr = sw.toString();
                p = JSON_F.createParser(docStr);
            }
            JsonToken t = null;
            try {
                t = p.nextToken();
            } catch (IOException e) {
                fail("Problem with number "+VALUE+", document ["+docStr+"]: "+e.getMessage());
            }
            assertNotNull("Document \""+docStr+"\" yielded no tokens", t);
            String exp = ""+VALUE;
            if (!exp.equals(p.getText())) {
                fail("Expected '"+exp+"', got '"+p.getText());
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(VALUE, p.getLongValue());
            assertEquals(null, p.nextToken());
            p.close();
        }
    }
}

