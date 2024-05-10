package tools.jackson.core.io;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.base.GeneratorBase;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [core#1064] wrt custom `BufferRecycler`
class BufferRecyclerPoolTest extends JUnit5TestBase
{
    @Test
    void noOp() throws Exception {
        // no-op pool doesn't actually pool anything, so avoid checking it
        checkBufferRecyclerPoolImpl(JsonRecyclerPools.nonRecyclingPool(), false, true);
    }

    @Test
    void threadLocal() throws Exception {
        checkBufferRecyclerPoolImpl(JsonRecyclerPools.threadLocalPool(), true, false);
    }

    @Test
    void concurrentDequeue() throws Exception {
        checkBufferRecyclerPoolImpl(JsonRecyclerPools.newConcurrentDequePool(), true, true);
    }

    @Test
    void bounded() throws Exception {
        checkBufferRecyclerPoolImpl(JsonRecyclerPools.newBoundedPool(1), true, true);
    }

    @Test
    void pluggingPool() throws Exception {
        checkBufferRecyclerPoolImpl(new TestPool(), true, true);
    }

    private void checkBufferRecyclerPoolImpl(RecyclerPool<BufferRecycler> pool,
            boolean checkPooledResource,
            boolean implementsClear)
        throws Exception
    {
        JsonFactory jsonFactory = JsonFactory.builder()
                .recyclerPool(pool)
                .build();
        BufferRecycler usedBufferRecycler = write("test", jsonFactory, 6);

        if (checkPooledResource) {
            // acquire the pooled BufferRecycler again and check if it is the same instance used before
            BufferRecycler pooledBufferRecycler = pool.acquireAndLinkPooled();
            assertSame(usedBufferRecycler, pooledBufferRecycler);
            // might as well return it back
            pooledBufferRecycler.releaseToPool();
        }

        // Also: check `clear()` method -- optional, but supported by all impls
        // except for ThreadLocal-based one
        if (implementsClear) {
            assertTrue(pool.clear());
    
            // cannot easily verify anything else except that we do NOT get the same recycled instance
            BufferRecycler br2 = pool.acquireAndLinkPooled();
            assertNotNull(br2);
            assertNotSame(usedBufferRecycler, br2);
        } else {
            assertFalse(pool.clear());
        }
    }

    protected final BufferRecycler write(String value, JsonFactory jsonFactory, int expectedSize) {
        BufferRecycler bufferRecycler;
        NopOutputStream out = new NopOutputStream();
        try (JsonGenerator gen = jsonFactory.createGenerator(ObjectWriteContext.empty(), out)) {
            bufferRecycler = ((GeneratorBase) gen).ioContext().bufferRecycler();
            gen.writeString(value);
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

    @SuppressWarnings("serial")
    class TestPool implements RecyclerPool<BufferRecycler>
    {
        private BufferRecycler bufferRecycler;

        @Override
        public BufferRecycler acquirePooled() {
            if (bufferRecycler != null) {
                BufferRecycler tmp = bufferRecycler;
                this.bufferRecycler = null;
                return tmp;
            }
            return new BufferRecycler();
        }

        @Override
        public void releasePooled(BufferRecycler r) {
            if (bufferRecycler == r) { // just sanity check for this test
                throw new IllegalStateException("BufferRecyler released more than once");
            }
            bufferRecycler = r;
        }

        @Override
        public int pooledCount() {
            return (bufferRecycler == null) ? 0 : 1;
        }

        @Override
        public boolean clear() {
            bufferRecycler = null;
            return true;
        }
    }
}
