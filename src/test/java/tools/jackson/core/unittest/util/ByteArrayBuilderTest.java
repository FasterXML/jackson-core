package tools.jackson.core.unittest.util;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.base.GeneratorBase;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.ByteArrayBuilder;
import tools.jackson.core.util.JsonRecyclerPools;

import static org.junit.jupiter.api.Assertions.*;

public class ByteArrayBuilderTest extends JacksonCoreTestBase
{
    @Test
    void testSimple() throws Exception
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
            assertEquals(i, (int) result[i]);
        }
        b.release();
        b.close();
    }

    @Test
    void testAppendFourBytesWithPositive() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler);

        assertEquals(0, byteArrayBuilder.size());

        byteArrayBuilder.appendFourBytes(2);

        assertEquals(4, byteArrayBuilder.size());
        assertEquals(0, byteArrayBuilder.toByteArray()[0]);
        assertEquals(0, byteArrayBuilder.toByteArray()[1]);
        assertEquals(0, byteArrayBuilder.toByteArray()[2]);
        assertEquals(2, byteArrayBuilder.toByteArray()[3]);
        byteArrayBuilder.close();
    }

    @Test
    void testAppendTwoBytesWithZero() {
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(0);

        assertEquals(0, byteArrayBuilder.size());

        byteArrayBuilder.appendTwoBytes(0);

        assertEquals(2, byteArrayBuilder.size());
        assertEquals(0, byteArrayBuilder.toByteArray()[0]);
        byteArrayBuilder.close();
    }

    @Test
    void testFinishCurrentSegment() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler, 2);
        byteArrayBuilder.appendThreeBytes(2);

        assertEquals(3, byteArrayBuilder.getCurrentSegmentLength());

        /*byte[] byteArray =*/ byteArrayBuilder.finishCurrentSegment();

        assertEquals(0, byteArrayBuilder.getCurrentSegmentLength());
        byteArrayBuilder.close();
    }

    // Content spanning multiple blocks, then reuse of the same builder: exercises
    // the lazily-created past-block list, including reset() before it exists
    @Test
    void testMultipleBlocksAndReuse() throws Exception
    {
        ByteArrayBuilder b = new ByteArrayBuilder(null, 10);

        // First round: single block only, no overflow
        b.write(new byte[] { 1, 2, 3 });
        assertArrayEquals(new byte[] { 1, 2, 3 }, b.toByteArray());

        // Second round: enough to overflow into several blocks
        b.reset();
        byte[] input = new byte[5000];
        for (int i = 0; i < input.length; ++i) {
            input[i] = (byte) i;
        }
        b.write(input);
        assertEquals(input.length, b.size());
        assertArrayEquals(input, b.toByteArray());

        // Third round: back to a single block; past blocks must not linger
        b.reset();
        b.append(42);
        assertArrayEquals(new byte[] { 42 }, b.toByteArray());

        b.release();
        b.close();
    }

    // [core#1195]: Try to verify that BufferRecycler instance is indeed reused
    @Test
    void testBufferRecyclerReuse() throws Exception
    {
        JsonFactory f = new JsonFactory();
        BufferRecycler br = new BufferRecycler()
                // need to link with some pool
                .withPool(JsonRecyclerPools.newBoundedPool(3));

        ByteArrayBuilder bab = new ByteArrayBuilder(br, 20);
        assertSame(br, bab.bufferRecycler());

        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), bab);
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