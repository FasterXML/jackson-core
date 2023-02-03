package com.fasterxml.jackson.core.base64;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.testsupport.ThrottledInputStream;

public class Base64GenerationTest
    extends com.fasterxml.jackson.core.BaseTest
{
    /* The usual sample input string, from Thomas Hobbes's "Leviathan"
     * (via Wikipedia)
     */
    private final static String WIKIPEDIA_BASE64_TEXT = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
    private final static byte[] WIKIPEDIA_BASE64_AS_BYTES;
    static {
        try {
            WIKIPEDIA_BASE64_AS_BYTES = WIKIPEDIA_BASE64_TEXT.getBytes("US-ASCII");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final String WIKIPEDIA_BASE64_ENCODED =
"TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz"
+"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg"
+"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu"
+"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo"
+"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
        ;


    private final static Base64Variant[] VARIANTS = {
            Base64Variants.MIME,
            Base64Variants.MIME_NO_LINEFEEDS,
            Base64Variants.MODIFIED_FOR_URL,
            Base64Variants.PEM
    };

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final JsonFactory JSON_F = new JsonFactory();

    public void testStreamingBinaryWrites() throws Exception
    {
        _testStreamingWrites(JSON_F, true);
        _testStreamingWrites(JSON_F, false);
    }

    // For [core#55]
    public void testIssue55() throws Exception
    {
        final JsonFactory f = new JsonFactory();

        // First,  byte-backed:
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        JsonGenerator gen = f.createGenerator(bytes);
        ByteArrayInputStream data = new ByteArrayInputStream(new byte[2000]);
        gen.writeBinary(data, 1999);
        gen.close();

        final int EXP_LEN = 2670;

        assertEquals(EXP_LEN, bytes.size());

        // Then char-backed
        StringWriter sw = new StringWriter();

        gen = f.createGenerator(sw);
        data = new ByteArrayInputStream(new byte[2000]);
        gen.writeBinary(data, 1999);
        gen.close();

        assertEquals(EXP_LEN, sw.toString().length());
    }

    /**
     * This is really inadequate test, all in all, but should serve
     * as some kind of sanity check. Reader-side should more thoroughly
     * test things, as it does need writers to construct the data first.
     */
    public void testSimpleBinaryWrite() throws Exception
    {
        _testSimpleBinaryWrite(false);
        _testSimpleBinaryWrite(true);
    }

    // for [core#318]
    public void testBinaryAsEmbeddedObject() throws Exception
    {
        JsonGenerator g;

        StringWriter sw = new StringWriter();
        g = JSON_F.createGenerator(sw);
        g.writeEmbeddedObject(WIKIPEDIA_BASE64_AS_BYTES);
        g.close();
        assertEquals(q(WIKIPEDIA_BASE64_ENCODED), sw.toString());

        ByteArrayOutputStream bytes =  new ByteArrayOutputStream(100);
        g = JSON_F.createGenerator(bytes);
        g.writeEmbeddedObject(WIKIPEDIA_BASE64_AS_BYTES);
        g.close();
        assertEquals(q(WIKIPEDIA_BASE64_ENCODED), bytes.toString("UTF-8"));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testSimpleBinaryWrite(boolean useCharBased) throws Exception
    {
        /* Let's only test the standard base64 variant; but write
         * values in root, array and object contexts.
         */
        Base64Variant b64v = Base64Variants.getDefaultVariant();
        JsonFactory jf = new JsonFactory();

        for (int i = 0; i < 3; ++i) {
            JsonGenerator gen;
            ByteArrayOutputStream bout = new ByteArrayOutputStream(200);
            if (useCharBased) {
                gen = jf.createGenerator(new OutputStreamWriter(bout, "UTF-8"));
            } else {
                gen = jf.createGenerator(bout, JsonEncoding.UTF8);
            }

            switch (i) {
            case 0: // root
                gen.writeBinary(b64v, WIKIPEDIA_BASE64_AS_BYTES, 0, WIKIPEDIA_BASE64_AS_BYTES.length);
                break;
            case 1: // array
                gen.writeStartArray();
                gen.writeBinary(b64v, WIKIPEDIA_BASE64_AS_BYTES, 0, WIKIPEDIA_BASE64_AS_BYTES.length);
                gen.writeEndArray();
                break;
            default: // object
                gen.writeStartObject();
                gen.writeFieldName("field");
                gen.writeBinary(b64v, WIKIPEDIA_BASE64_AS_BYTES, 0, WIKIPEDIA_BASE64_AS_BYTES.length);
                gen.writeEndObject();
                break;
            }
            gen.close();

            JsonParser jp = jf.createParser(new ByteArrayInputStream(bout.toByteArray()));

            // Need to skip other events before binary data:
            switch (i) {
            case 0:
                break;
            case 1:
                assertEquals(JsonToken.START_ARRAY, jp.nextToken());
                break;
            default:
                assertEquals(JsonToken.START_OBJECT, jp.nextToken());
                assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
                break;
            }
            assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
            String actualValue = jp.getText();
            jp.close();
            assertEquals(WIKIPEDIA_BASE64_ENCODED, actualValue);
        }
    }

    private final static String TEXT = "Some content so that we can test encoding of base64 data; must"
            +" be long enough include a line wrap or two...";
    private final static String TEXT4 = TEXT + TEXT + TEXT + TEXT;

    @SuppressWarnings("resource")
    private void _testStreamingWrites(JsonFactory jf, boolean useBytes) throws Exception
    {
        final byte[] INPUT = TEXT4.getBytes("UTF-8");
        for (Base64Variant variant : VARIANTS) {
            final String EXP_OUTPUT = "[" + q(variant.encode(INPUT))+"]";
            for (boolean passLength : new boolean[] { true, false }) {
                for (int chunkSize : new int[] { 1, 2, 3, 4, 7, 11, 29, 5000 }) {
//System.err.println(""+variant+", length "+passLength+", chunk "+chunkSize);

                    JsonGenerator jgen;

                    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    if (useBytes) {
                        jgen = jf.createGenerator(bytes);
                    } else {
                        jgen = jf.createGenerator(new OutputStreamWriter(bytes, "UTF-8"));
                    }
                    jgen.writeStartArray();
                    int length = passLength ? INPUT.length : -1;
                    InputStream data = new ThrottledInputStream(INPUT, chunkSize);
                    jgen.writeBinary(variant, data, length);
                    jgen.writeEndArray();
                    jgen.close();
                    String JSON = bytes.toString("UTF-8");
                    assertEquals(EXP_OUTPUT, JSON);
                }
            }
        }
    }
}
