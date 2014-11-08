package com.fasterxml.jackson.core.main;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.BufferRecycler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class TestRawByteWriting extends com.fasterxml.jackson.core.BaseTest
{

    private static final Charset[] CHARSETS;

    static {
        List<Charset> charsets = new ArrayList<Charset>();

        for (JsonEncoding enc : JsonEncoding.values()) {
            charsets.add(Charset.forName(enc.getJavaName()));
        }
        charsets.add(Charset.forName("ISO-8859-7"));
        charsets.add(Charset.forName("windows-1253"));

        CHARSETS = charsets.toArray(new Charset[charsets.size()]);
    }


    /**
     * Test for direct byte writing when the value is known to be well-formed utf-8 encoded JSON.
     *
     * Tests the cartesian product of CHARSETS with JsonEncoding.values() for input and output encodings respectively.
     */
    public void testDirectByteWrites() throws Exception
    {
        String value = "εδλ";
        assertTrue("Check that `value` has some non-trivial code points", value.length() < value.getBytes("utf8").length);

        String rawStringValue = '"' + value + '"';
        ByteArrayOutputStream[] outs = new ByteArrayOutputStream[JsonEncoding.values().length];
        JsonFactory jf = new JsonFactory();
        List<JsonGenerator> generators = makeGenerators(jf, outs);

        for (JsonGenerator jgen : generators) {
            jgen.writeStartArray();
            for (Charset charset : CHARSETS) {
                final byte[] valueBytes = rawStringValue.getBytes(charset);
                // should be ignored
                jgen.writeRaw(new byte[0], charset);

                // valid writes
                jgen.writeRawValue(valueBytes, charset);
                jgen.writeRawValue(Arrays.copyOf(valueBytes, valueBytes.length + 2), 0, valueBytes.length, charset);
            }
            jgen.writeEndArray();
            jgen.close();
        }

        for (int i = 0; i < outs.length; i++) {
            final JsonEncoding output = JsonEncoding.values()[i];
            byte[] json = outs[i].toByteArray();

            // Ok: let's verify that stuff was written out ok
            JsonParser jp = jf.createParser(json);
            assertToken(JsonToken.START_ARRAY, jp.nextToken());

            for (Charset input : CHARSETS) {
                assertToken(JsonToken.VALUE_STRING, jp.nextToken());
                assertEquals(String.format("failed to write string to output encoding %s with input encoding %s", output, input), value, jp.getText());

                assertToken(JsonToken.VALUE_STRING, jp.nextToken());
                assertEquals(String.format("failed to write string to output encoding %s with input encoding %s (using offset+len)", output, input), value, jp.getText());
            }

            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }
    }

    public void testWriterFlushesCharset() throws IOException {
        // This seems to be the only charset that implements a real CharsetDecoder#flush method
        final Charset charset = Charset.forName("ISCII91");
        String longValue = new String(new BufferRecycler().allocCharBuffer(BufferRecycler.CHAR_CONCAT_BUFFER)).replace("\0", "a")  + "ऋ";

        ByteArrayOutputStream[] outs = new ByteArrayOutputStream[JsonEncoding.values().length];
        JsonFactory jf = new JsonFactory();
        List<JsonGenerator> generators = makeGenerators(jf, outs);

        for (JsonGenerator jgen : generators) {
            jgen.writeRaw('"');
            jgen.flush();
            jgen.writeRawValue(longValue.getBytes(charset), charset);
            jgen.writeRaw('"');
            jgen.close();
        }

        for (ByteArrayOutputStream out : outs) {
            byte[] json = out.toByteArray();

            JsonParser jp = jf.createParser(json);

            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertArrayEquals(longValue.toCharArray(), jp.getText().toCharArray());
        }
    }

    private static List<JsonGenerator> makeGenerators(JsonFactory jf, ByteArrayOutputStream[] outs) throws IOException {
        if (JsonEncoding.values().length != outs.length) throw new IllegalArgumentException("wrong number of outputs");
        JsonGenerator[] jgens = new JsonGenerator[outs.length];
        for (int i = 0; i < outs.length; i++) {
            jgens[i] = jf.createGenerator((outs[i] = new ByteArrayOutputStream(16000)), JsonEncoding.values()[i]);
        }
        return Arrays.asList(jgens);
    }
}
