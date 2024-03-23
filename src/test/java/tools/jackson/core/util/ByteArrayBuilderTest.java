package tools.jackson.core.util;

import java.nio.charset.StandardCharsets;

import org.junit.Assert;

import tools.jackson.core.*;
import tools.jackson.core.base.GeneratorBase;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.*;

public class ByteArrayBuilderTest extends JUnit5TestBase
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

    public void testAppendFourBytesWithPositive() {
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

    public void testAppendTwoBytesWithZero() {
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(0);

        assertEquals(0, byteArrayBuilder.size());

        byteArrayBuilder.appendTwoBytes(0);

        assertEquals(2, byteArrayBuilder.size());
        assertEquals(0, byteArrayBuilder.toByteArray()[0]);
        byteArrayBuilder.close();
    }

    public void testFinishCurrentSegment() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler, 2);
        byteArrayBuilder.appendThreeBytes(2);

        assertEquals(3, byteArrayBuilder.getCurrentSegmentLength());

        /*byte[] byteArray =*/ byteArrayBuilder.finishCurrentSegment();

        assertEquals(0, byteArrayBuilder.getCurrentSegmentLength());
        byteArrayBuilder.close();
    }

    // [core#1195]: Try to verify that BufferRecycler instance is indeed reused
    public void testBufferRecyclerReuse() throws Exception
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