package tools.jackson.core.io;

import tools.jackson.core.BaseTest;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonGeneratorBase;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.BufferRecyclerPool;

import java.io.IOException;
import java.io.OutputStream;

public class BufferRecyclerPoolTest extends BaseTest
{
    public void testNoOp() {
        // no-op pool doesn't actually pool anything, so avoid checking it
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.NonRecyclingPool.shared(), false);
    }

    public void testThreadLocal() {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.ThreadLocalPool.shared(), true);
    }

    public void testLockFree() {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.LockFreePool.nonShared(), true);
    }

    public void testConcurrentDequeue() {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.ConcurrentDequePool.nonShared(), true);
    }

    public void testBounded() {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.BoundedPool.nonShared(1), true);
    }

    private void checkBufferRecyclerPoolImpl(BufferRecyclerPool pool, boolean checkPooledResource) {
        JsonFactory jsonFactory = JsonFactory.builder()
                .bufferRecyclerPool(pool)
                .build();
        BufferRecycler usedBufferRecycler = write("test", jsonFactory, 6);

        if (checkPooledResource) {
            // acquire the pooled BufferRecycler again and check if it is the same instance used before
            BufferRecycler pooledBufferRecycler = pool.acquireBufferRecycler();
            try {
                assertSame(usedBufferRecycler, pooledBufferRecycler);
            } finally {
                pooledBufferRecycler.release();
            }
        }
    }

    protected final BufferRecycler write(String value, JsonFactory jsonFactory, int expectedSize) {
        BufferRecycler bufferRecycler;
        NopOutputStream out = new NopOutputStream();
        try (JsonGenerator gen = jsonFactory.createGenerator(ObjectWriteContext.empty(), out)) {
            bufferRecycler = ((JsonGeneratorBase) gen).ioContext()._bufferRecycler;
            gen.writeString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(expectedSize, out.size);
        return bufferRecycler;
    }

    public class NopOutputStream extends OutputStream {
        protected int size = 0;

        public NopOutputStream() { }

        @Override
        public void write(int b) throws IOException { ++size; }

        @Override
        public void write(byte[] b) throws IOException { size += b.length; }

        @Override
        public void write(byte[] b, int offset, int len) throws IOException { size += len; }

        public NopOutputStream reset() {
            size = 0;
            return this;
        }
        public int size() { return size; }
    }
}
