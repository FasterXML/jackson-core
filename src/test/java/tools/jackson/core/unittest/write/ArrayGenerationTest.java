package tools.jackson.core.unittest.write;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Basic testing for scalar-array write methods added in 2.8.
 */
class ArrayGenerationTest extends JacksonCoreTestBase
{
    // 17-Sep-2024, tatu: [core#223] change to surrogates, let's use old behavior
    //   for now for simpler testing
    private final JsonFactory FACTORY = streamFactoryBuilder()
            .disable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
            .build();

    protected TokenStreamFactory jsonFactory() {
        return FACTORY;
    }

    @Test
    void intArray() throws Exception
    {
        _testIntArray(false);
        _testIntArray(true);
    }

    @Test
    void longArray() throws Exception
    {
        _testLongArray(false);
        _testLongArray(true);
    }

    @Test
    void doubleArray() throws Exception
    {
        _testDoubleArray(false);
        _testDoubleArray(true);
    }

    @Test
    void stringArray() throws Exception
    {
        _testStringArray(false);
        _testStringArray(true);
    }

    private void _testIntArray(boolean useBytes) throws Exception {
        // first special cases of 0, 1 values
        _testIntArray(useBytes, 0, 0, 0);
        _testIntArray(useBytes, 0, 1, 1);

        _testIntArray(useBytes, 1, 0, 0);
        _testIntArray(useBytes, 1, 1, 1);

        // and then some bigger data
        _testIntArray(useBytes, 15, 0, 0);
        _testIntArray(useBytes, 15, 2, 3);
        _testIntArray(useBytes, 39, 0, 0);
        _testIntArray(useBytes, 39, 4, 0);
        _testIntArray(useBytes, 271, 0, 0);
        _testIntArray(useBytes, 271, 0, 4);
        _testIntArray(useBytes, 5009, 0, 0);
        _testIntArray(useBytes, 5009, 0, 1);
    }

    private void _testLongArray(boolean useBytes) throws Exception {
        // first special cases of 0, 1 values
        _testLongArray(useBytes, 0, 0, 0);
        _testLongArray(useBytes, 0, 1, 1);

        _testLongArray(useBytes, 1, 0, 0);
        _testLongArray(useBytes, 1, 1, 1);

        // and then some bigger data
        _testLongArray(useBytes, 15, 0, 0);
        _testLongArray(useBytes, 15, 2, 3);
        _testLongArray(useBytes, 39, 0, 0);
        _testLongArray(useBytes, 39, 4, 0);
        _testLongArray(useBytes, 271, 0, 0);
        _testLongArray(useBytes, 271, 0, 4);
        _testLongArray(useBytes, 5009, 0, 0);
        _testLongArray(useBytes, 5009, 0, 1);
    }

    private void _testDoubleArray(boolean useBytes) throws Exception {
        // first special cases of 0, 1 values
        _testDoubleArray(useBytes, 0, 0, 0);
        _testDoubleArray(useBytes, 0, 1, 1);

        _testDoubleArray(useBytes, 1, 0, 0);
        _testDoubleArray(useBytes, 1, 1, 1);

        // and then some bigger data
        _testDoubleArray(useBytes, 15, 0, 0);
        _testDoubleArray(useBytes, 15, 2, 3);
        _testDoubleArray(useBytes, 39, 0, 0);
        _testDoubleArray(useBytes, 39, 4, 0);
        _testDoubleArray(useBytes, 271, 0, 0);
        _testDoubleArray(useBytes, 271, 0, 4);
        _testDoubleArray(useBytes, 5009, 0, 0);
        _testDoubleArray(useBytes, 5009, 0, 1);
    }

    private void _testStringArray(boolean useBytes) throws Exception {
        // first special cases of 0, 1 values
        _testStringArray(useBytes, 0, 0, 0);
        _testStringArray(useBytes, 0, 1, 1);

        _testStringArray(useBytes, 1, 0, 0);
        _testStringArray(useBytes, 1, 1, 1);

        // and then some bigger data
        _testStringArray(useBytes, 15, 0, 0);
        _testStringArray(useBytes, 15, 2, 3);
        _testStringArray(useBytes, 39, 0, 0);
        _testStringArray(useBytes, 39, 4, 0);
        _testStringArray(useBytes, 271, 0, 0);
        _testStringArray(useBytes, 271, 0, 4);
        _testStringArray(useBytes, 5009, 0, 0);
        _testStringArray(useBytes, 5009, 0, 1);
    }

    private void _testIntArray(boolean useBytes, int elements, int pre, int post) throws Exception
    {
        int[] values = new int[elements+pre+post];
        for (int i = pre, end = pre+elements; i < end; ++i) {
            values[i] = i-pre;
        }

        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (JsonGenerator gen = _generator(jsonFactory(), useBytes, bytes, sw)) {
            gen.writeArray(values, pre, elements);
        }
        String json = useBytes ? bytes.toString("UTF-8") : sw.toString();

        try (JsonParser p = _parser(jsonFactory(), useBytes, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i < elements; ++i) {
                if ((i & 1) == 0) { // alternate
                    JsonToken t = p.nextToken();
                    if (t != JsonToken.VALUE_NUMBER_INT) {
                        fail("Expected number, got "+t+", element #"+i);
                    }
                    assertEquals(i, p.getIntValue());
                } else {
                    assertEquals(i, p.nextIntValue(-1));
                }
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    private void _testLongArray(boolean useBytes, int elements, int pre, int post) throws Exception
    {
        long[] values = new long[elements+pre+post];
        for (int i = pre, end = pre+elements; i < end; ++i) {
            values[i] = i-pre;
        }

        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (JsonGenerator gen = _generator(jsonFactory(), useBytes, bytes, sw)) {
            gen.writeArray(values, pre, elements);
        }
        String json = useBytes ? bytes.toString("UTF-8") : sw.toString();

        try (JsonParser p = _parser(jsonFactory(), useBytes, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i < elements; ++i) {
                if ((i & 1) == 0) { // alternate
                    JsonToken t = p.nextToken();
                    if (t != JsonToken.VALUE_NUMBER_INT) {
                        fail("Expected number, got "+t+", element #"+i);
                    }
                    assertEquals(i, p.getLongValue());
                } else {
                    assertEquals(i, p.nextLongValue(-1));
                }
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    private void _testDoubleArray(boolean useBytes, int elements, int pre, int post) throws Exception
    {
        double[] values = new double[elements+pre+post];
        for (int i = pre, end = pre+elements; i < end; ++i) {
            values[i] = i-pre;
        }

        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (JsonGenerator gen = _generator(jsonFactory(), useBytes, bytes, sw)) {
            gen.writeArray(values, pre, elements);
        }
        String json = useBytes ? bytes.toString("UTF-8") : sw.toString();

        try (JsonParser p = _parser(jsonFactory(), useBytes, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i < elements; ++i) {
                JsonToken t = p.nextToken();
                if (t != JsonToken.VALUE_NUMBER_FLOAT) {
                    fail("Expected floating-point number, got "+t+", element #"+i);
                }
                assertEquals((double) i, p.getDoubleValue());
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    private void _testStringArray(boolean useBytes, int elements, int pre, int post) throws Exception
    {
        int byteLength = 16;
        Random random = new Random();
        Charset utf8 = Charset.forName("UTF-8");
        String[] values = new String[elements+pre+post];
        for (int i = pre, end = pre+elements; i < end; ++i) {
            byte[] content = new byte[byteLength];
            random.nextBytes(content);
            values[i] = new String(content, utf8);
        }

        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (JsonGenerator gen = _generator(jsonFactory(), useBytes, bytes, sw)) {
            gen.writeArray(values, pre, elements);
        }
        String json = useBytes ? bytes.toString("UTF-8") : sw.toString();

        try (JsonParser p = _parser(jsonFactory(), useBytes, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i < elements; ++i) {
                JsonToken t = p.nextToken();
                if (t != JsonToken.VALUE_STRING) {
                    fail("Expected string, got "+t+", element #"+i);
                }
                assertEquals(values[pre+i], p.getValueAsString());
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    private JsonGenerator _generator(TokenStreamFactory f, boolean useBytes,
            ByteArrayOutputStream bytes, Writer w)
        throws Exception
    {
        if (useBytes) {
            return f.createGenerator(ObjectWriteContext.empty(), bytes);
        }
        return f.createGenerator(ObjectWriteContext.empty(), w);
    }

    private JsonParser _parser(TokenStreamFactory f, boolean useBytes, String json)
        throws Exception
    {
        if (useBytes) {
            return f.createParser(ObjectReadContext.empty(),
                    json.getBytes(StandardCharsets.UTF_8));
        }
        return jsonFactory().createParser(ObjectReadContext.empty(), json);
    }
}
