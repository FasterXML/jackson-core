package tools.jackson.core.unittest.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.base.GeneratorBase;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [core#1064] wrt custom `BufferRecycler`
class BufferRecyclerPoolTest extends JacksonCoreTestBase
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
    void boundedPoolDoesNotExceedCapacity() {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(2);

        BufferRecycler br1 = pool.acquireAndLinkPooled();
        BufferRecycler br2 = pool.acquireAndLinkPooled();
        BufferRecycler br3 = pool.acquireAndLinkPooled();

        br1.releaseToPool();
        br2.releaseToPool();
        br3.releaseToPool();

        assertEquals(2, pool.pooledCount());
    }

    @Test
    void boundedPoolClearDropsRetainedRecyclers() {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(2);

        BufferRecycler br1 = pool.acquireAndLinkPooled();
        BufferRecycler br2 = pool.acquireAndLinkPooled();

        br1.releaseToPool();
        br2.releaseToPool();

        assertEquals(2, pool.pooledCount());

        assertTrue(pool.clear());
        assertEquals(0, pool.pooledCount());
    }

    @Test
    void bufferRecyclerReleaseToPoolIsIdempotent() {
        TestPool pool = new TestPool();
        BufferRecycler recycler = pool.acquireAndLinkPooled();

        recycler.releaseToPool();
        recycler.releaseToPool();

        assertEquals(1, pool.pooledCount());
    }

    // NOTE: parser/generator close() releasing the recycler exactly once is covered
    // by `JsonBufferRecyclersTest`, for all pool implementations

    @Test
    void boundedPoolConcurrentUseDoesNotShareRecyclers() throws Exception {
        final int capacity = 4;
        final int threadCount = 8;
        final int iterations = 1_000;

        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(capacity);
        JsonFactory jsonFactory = JsonFactory.builder()
                .recyclerPool(pool)
                .build();

        // Recyclers held by a live generator: pool must never hand the same instance
        // to two holders at once. `BufferRecycler.withPool()` already throws on an
        // overlapping acquisition; this tracks it independently, and also covers
        // sharing that would slip past that linkage check. Identity-based since
        // `BufferRecycler` does not override equals()/hashCode(). Parsers exercise the
        // pool too, but only `GeneratorBase` exposes its `IOContext`, so only
        // generators can be tracked.
        final Set<BufferRecycler> inUse = ConcurrentHashMap.newKeySet();
        final AtomicInteger sharedCount = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            ArrayList<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; ++i) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < iterations; ++j) {
                        read(jsonFactory);

                        NopOutputStream out = new NopOutputStream();
                        JsonGenerator gen = jsonFactory.createGenerator(
                                ObjectWriteContext.empty(), out);
                        BufferRecycler br = ((GeneratorBase) gen).ioContext().bufferRecycler();
                        if (!inUse.add(br)) {
                            sharedCount.incrementAndGet();
                        }
                        gen.writeString("test");
                        // Must stop tracking before close(), which returns it to the pool
                        inUse.remove(br);
                        gen.close();
                    }
                    return null;
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(0, sharedCount.get(),
                "BufferRecycler handed out to more than one holder at a time");
        assertEquals(0, inUse.size());
        // Bounded queue must retain releases, up to but not beyond capacity
        int pooled = pool.pooledCount();
        assertTrue(pooled > 0 && pooled <= capacity,
                "Unexpected pooledCount(): "+pooled);
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

    private void read(JsonFactory jsonFactory) throws Exception {
        try (JsonParser p = createParser(jsonFactory, MODE_INPUT_STREAM,
                a2q("{'a':123,'b':'foobar'}"))) {
            while (p.nextToken() != null) { }
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
