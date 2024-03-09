package com.fasterxml.jackson.core.util;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.TestBase;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.IOContext;

import static org.junit.jupiter.api.Assertions.*;

public class ByteArrayBuilderTest extends TestBase
{
    @Test
    public void testSimple() throws Exception
    {
        ByteArrayBuilder b = new ByteArrayBuilder(null, 20);
        assertArrayEquals(new byte[0], b.toByteArray());

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
            assertEquals(i, result[i]);
        }

        b.release();
        b.close();
    }

    // [core#1195]: Try to verify that BufferRecycler instance is indeed reused
    @Test
    public void testBufferRecyclerReuse() throws Exception
    {
        JsonFactory f = new JsonFactory();
        BufferRecycler br = new BufferRecycler()
                // need to link with some pool
                .withPool(JsonRecyclerPools.newBoundedPool(3));

        ByteArrayBuilder bab = new ByteArrayBuilder(br, 20);
        assertSame(br, bab.bufferRecycler());

        JsonGenerator g = f.createGenerator(bab);
        IOContext ioCtxt = ((GeneratorBase) g).ioContext();
        assertSame(br, ioCtxt.bufferRecycler());
        assertTrue(ioCtxt.bufferRecycler().isLinkedWithPool());

        g.writeStartArray();
        g.writeEndArray();
        g.close();

        // Generator.close() should NOT release buffer recycler
        assertTrue(br.isLinkedWithPool());

        byte[] result = bab.getClearAndRelease();
        assertEquals("[]", new String(result, StandardCharsets.UTF_8));
        // Nor accessing contents
        assertTrue(br.isLinkedWithPool());

        // only explicit release does
        br.releaseToPool();
        assertFalse(br.isLinkedWithPool());
    }
}
