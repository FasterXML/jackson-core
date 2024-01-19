package com.fasterxml.jackson.core.util;

import java.nio.charset.StandardCharsets;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.base.GeneratorBase;

public class ByteArrayBuilderTest extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimple() throws Exception
    {
        ByteArrayBuilder b = new ByteArrayBuilder(null, 20);
        Assert.assertArrayEquals(new byte[0], b.toByteArray());

        b.write((byte) 0);
        b.append(1);

        byte[] foo = new byte[98];
        for (int i = 0; i < foo.length; ++i) {
            foo[i] = (byte) (2 + i);
        }
        b.write(foo);

        byte[] result = b.toByteArray();
        assertEquals(100, result.length);
        for (int i = 0; i < 100; ++i) {
            assertEquals(i, (int) result[i]);
        }

        b.release();
        b.close();
    }

    // [core#1195]: Try to verify that BufferRecycler instance is indeed reused
    public void testBufferRecyclerReuse() throws Exception
    {
        JsonFactory f = new JsonFactory();
        BufferRecycler br = new BufferRecycler();

        ByteArrayBuilder bab = new ByteArrayBuilder(br, 20);
        assertSame(br, bab.bufferRecycler());

        try (JsonGenerator g = f.createGenerator(bab)) {
            assertSame(br, ((GeneratorBase) g).ioContext().bufferRecycler());
            g.writeStartArray();
            g.writeEndArray();
        }
        assertEquals("[]", new String(bab.toByteArray(), StandardCharsets.UTF_8));
    }
}
