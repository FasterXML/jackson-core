package com.fasterxml.jackson.core.json;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import com.fasterxml.jackson.core.*;

/**
 * Basic testing for scalar-array write methods added in 2.8.
 */
public class ArrayGenerationTest extends BaseTest
{
    private final JsonFactory FACTORY = new JsonFactory();
    
    public void testIntArray() throws Exception
    {
        _testIntArray(false);
        _testIntArray(true);
    }

    public void testLongArray() throws Exception
    {
        _testLongArray(false);
        _testLongArray(true);
    }

    public void testDoubleArray() throws Exception
    {
        _testDoubleArray(false);
        _testDoubleArray(true);
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

    private void _testIntArray(boolean useBytes, int elements, int pre, int post) throws Exception
    {
        int[] values = new int[elements+pre+post];
        for (int i = pre, end = pre+elements; i < end; ++i) {
            values[i] = i-pre;
        }
        
        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        JsonGenerator gen = useBytes ? FACTORY.createGenerator(bytes)
                : FACTORY.createGenerator(sw);

        gen.writeArray(values, pre, elements);
        gen.close();

        String json;
        if (useBytes) {
            json = bytes.toString("UTF-8");
        } else {
            json = sw.toString();
        }

        JsonParser p = useBytes ? FACTORY.createParser(bytes.toByteArray())
                : FACTORY.createParser(json);
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
        p.close();
    }

    private void _testLongArray(boolean useBytes, int elements, int pre, int post) throws Exception
    {
        long[] values = new long[elements+pre+post];
        for (int i = pre, end = pre+elements; i < end; ++i) {
            values[i] = i-pre;
        }
        
        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        JsonGenerator gen = useBytes ? FACTORY.createGenerator(bytes)
                : FACTORY.createGenerator(sw);

        gen.writeArray(values, pre, elements);
        gen.close();

        String json;
        if (useBytes) {
            json = bytes.toString("UTF-8");
        } else {
            json = sw.toString();
        }

        JsonParser p = useBytes ? FACTORY.createParser(bytes.toByteArray())
                : FACTORY.createParser(json);
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
        p.close();
    }

    private void _testDoubleArray(boolean useBytes, int elements, int pre, int post) throws Exception
    {
        double[] values = new double[elements+pre+post];
        for (int i = pre, end = pre+elements; i < end; ++i) {
            values[i] = i-pre;
        }

        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        JsonGenerator gen = useBytes ? FACTORY.createGenerator(bytes)
                : FACTORY.createGenerator(sw);

        gen.writeArray(values, pre, elements);
        gen.close();

        String json;
        if (useBytes) {
            json = bytes.toString("UTF-8");
        } else {
            json = sw.toString();
        }

        JsonParser p = useBytes ? FACTORY.createParser(bytes.toByteArray())
                : FACTORY.createParser(json);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        for (int i = 0; i < elements; ++i) {
            JsonToken t = p.nextToken();
            if (t != JsonToken.VALUE_NUMBER_FLOAT) {
                fail("Expected floating-point number, got "+t+", element #"+i);
            }
            assertEquals((double) i, p.getDoubleValue());
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }
}
