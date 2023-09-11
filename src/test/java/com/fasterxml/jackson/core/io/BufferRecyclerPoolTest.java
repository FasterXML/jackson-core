package com.fasterxml.jackson.core.io;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonGeneratorImpl;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.BufferRecyclerPool;

import java.io.IOException;
import java.io.OutputStream;

public class BufferRecyclerPoolTest extends BaseTest
{
    public void testNoOp() throws Exception {
        // no-op pool doesn't actually pool anything, so avoid checking it
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.NonRecyclingPool.shared(), false);
    }

    public void testThreadLocal() throws Exception {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.ThreadLocalPool.shared(), true);
    }

    public void testLockFree() throws Exception {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.LockFreePool.nonShared(), true);
    }

    public void testConcurrentDequeue() throws Exception {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.ConcurrentDequePool.nonShared(), true);
    }

    public void testBounded() throws Exception {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.BoundedPool.nonShared(1), true);
    }

    public void testPluggingPool() throws Exception {
        checkBufferRecyclerPoolImpl(new TestPool(), true);
    }

    private void checkBufferRecyclerPoolImpl(BufferRecyclerPool pool, boolean checkPooledResource) throws Exception {
        JsonFactory jsonFactory = JsonFactory.builder()
                .bufferRecyclerPool(pool)
                .build();
        BufferRecycler usedBufferRecycler = write("test", jsonFactory, 6);

        if (checkPooledResource) {
            // acquire the pooled BufferRecycler again and check if it is the same instance used before
            BufferRecycler pooledBufferRecycler = pool._internalAcquire();
            try {
                assertSame(usedBufferRecycler, pooledBufferRecycler);
            } finally {
                pooledBufferRecycler.release();
            }
        }
    }

    private BufferRecycler write(Object value, JsonFactory jsonFactory, int expectedSize) throws Exception {
        BufferRecycler bufferRecycler;
        NopOutputStream out = new NopOutputStream();
        try (JsonGenerator gen = jsonFactory.createGenerator(out)) {
            bufferRecycler = ((JsonGeneratorImpl) gen).ioContext()._bufferRecycler;
            gen.writeObject(value);
        }
        assertEquals(expectedSize, out.size);
        return bufferRecycler;
    }

    private static class NopOutputStream extends OutputStream {
        protected int size = 0;

        NopOutputStream() { }

        @Override
        public void write(int b) throws IOException { ++size; }

        @Override
        public void write(byte[] b) throws IOException { size += b.length; }

        @Override
        public void write(byte[] b, int offset, int len) throws IOException { size += len; }
    }


    class TestPool implements BufferRecyclerPool {

        private BufferRecycler bufferRecycler;

        @Override
        public BufferRecycler acquireBufferRecycler() {
            if (bufferRecycler != null) {
                BufferRecycler tmp = bufferRecycler;
                this.bufferRecycler = null;
                return tmp;
            }
            return new BufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler recycler) {
            this.bufferRecycler = recycler;
        }
    }
}
