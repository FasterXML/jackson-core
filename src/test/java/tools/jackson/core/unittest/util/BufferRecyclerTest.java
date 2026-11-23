package tools.jackson.core.unittest.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

class BufferRecyclerTest extends JacksonCoreTestBase
{
    private final static int SLOT_PADDING = 32;

    static class CustomCountRecycler extends BufferRecycler {
        CustomCountRecycler() {
            super(5, 6);
        }

        @Override
        protected int byteBufferLength(int ix) {
            return 10 + ix;
        }

        @Override
        protected int charBufferLength(int ix) {
            return 20 + ix;
        }
    }

    static class CountingPool implements RecyclerPool<BufferRecycler> {
        private static final long serialVersionUID = 1L;

        final AtomicInteger released = new AtomicInteger();

        @Override
        public BufferRecycler acquirePooled() {
            return new BufferRecycler();
        }

        @Override
        public void releasePooled(BufferRecycler pooled) {
            released.incrementAndGet();
        }
    }

    private final static int THREADS = 8;

    @Test
    void writtenSlotsArePadded() throws Exception {
        BufferRecycler br = new CustomCountRecycler()
                .withPool(JsonRecyclerPools.newBoundedPool(1));
        for (int ix = 0; ix < 5; ++ix) {
            br.releaseByteBuffer(ix, br.allocByteBuffer(ix));
        }
        for (int ix = 0; ix < 6; ++ix) {
            br.releaseCharBuffer(ix, br.allocCharBuffer(ix));
        }

        Field f = BufferRecycler.class.getDeclaredField("_slots");
        f.setAccessible(true);
        Object[] slots = (Object[]) f.get(br);
        // pool link, 5 byte buffers, 6 char buffers
        assertEquals(SLOT_PADDING + 1 + 5 + 6 + SLOT_PADDING, slots.length);
        for (int i = 0; i < slots.length; ++i) {
            boolean used = (i >= SLOT_PADDING) && (i < slots.length - SLOT_PADDING);
            assertEquals(used, slots[i] != null, "slot #" + i);
        }

        br.releaseToPool();
        assertNull(slots[SLOT_PADDING]);
    }

    // Non-recycling pool hands out one-off recyclers, which need no padding
    @Test
    void nonRecyclingPoolSlotsAreNotPadded() throws Exception {
        BufferRecycler br = JsonRecyclerPools.nonRecyclingPool().acquireAndLinkPooled();
        Field f = BufferRecycler.class.getDeclaredField("_slots");
        f.setAccessible(true);
        Object[] slots = (Object[]) f.get(br);
        // pool link, 4 byte buffers, 4 char buffers
        assertEquals(1 + 4 + 4, slots.length);
        // not linked by this pool; link to another to check the pool slot
        assertFalse(br.isLinkedWithPool());
        CountingPool pool = new CountingPool();
        br.withPool(pool);
        assertSame(pool, slots[0]);

        byte[] bytes = br.allocByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER);
        char[] chars = br.allocCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER);
        br.releaseByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER, bytes);
        br.releaseCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER, chars);
        assertSame(bytes, br.allocByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER));
        assertSame(chars, br.allocCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER));

        br.releaseToPool();
        assertNull(slots[0]);
        assertEquals(1, pool.released.get());
    }

    @Test
    void byteAndCharSlotsDoNotOverlap() {
        BufferRecycler br = new CustomCountRecycler();
        byte[][] bytes = new byte[5][];
        char[][] chars = new char[6][];
        for (int ix = 0; ix < bytes.length; ++ix) {
            bytes[ix] = new byte[100];
            br.releaseByteBuffer(ix, bytes[ix]);
        }
        for (int ix = 0; ix < chars.length; ++ix) {
            chars[ix] = new char[100];
            br.releaseCharBuffer(ix, chars[ix]);
        }

        for (int ix = 0; ix < bytes.length; ++ix) {
            assertSame(bytes[ix], br.allocByteBuffer(ix), "byte buffer #" + ix);
        }
        for (int ix = 0; ix < chars.length; ++ix) {
            assertSame(chars[ix], br.allocCharBuffer(ix), "char buffer #" + ix);
        }
    }

    @Test
    void releaseKeepsLargerBuffer() {
        BufferRecycler br = new BufferRecycler();
        byte[] large = new byte[9000];
        br.releaseByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER, large);
        br.releaseByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER, new byte[100]);
        assertSame(large, br.allocByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER));

        char[] small = new char[100];
        char[] larger = new char[200];
        br.releaseCharBuffer(BufferRecycler.CHAR_NAME_COPY_BUFFER, small);
        br.releaseCharBuffer(BufferRecycler.CHAR_NAME_COPY_BUFFER, larger);
        assertSame(larger, br.allocCharBuffer(BufferRecycler.CHAR_NAME_COPY_BUFFER, 10));
    }

    @Test
    void bufferIndexOutOfRange() {
        BufferRecycler br = new CustomCountRecycler();
        assertThrows(IndexOutOfBoundsException.class, () -> br.allocByteBuffer(5));
        assertThrows(IndexOutOfBoundsException.class, () -> br.allocCharBuffer(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> br.releaseByteBuffer(-1, new byte[1]));
        assertThrows(IndexOutOfBoundsException.class, () -> br.releaseCharBuffer(6, new char[1]));
        assertFalse(br.isLinkedWithPool());
    }

    @Test
    void negativeBufferCount() {
        assertThrows(IllegalArgumentException.class, () -> new BufferRecycler(-1, 4) { });
        assertThrows(IllegalArgumentException.class, () -> new BufferRecycler(4, -1) { });
    }

    @Test
    void releaseOfEqualLengthKeepsCurrentBuffer() {
        BufferRecycler br = new BufferRecycler();
        byte[] firstBytes = new byte[8000];
        br.releaseByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER, firstBytes);
        br.releaseByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER, new byte[8000]);
        assertSame(firstBytes, br.allocByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER));

        char[] firstChars = new char[4000];
        br.releaseCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER, firstChars);
        br.releaseCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER, new char[4000]);
        assertSame(firstChars, br.allocCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER));
    }

    @Test
    void noBuffers() {
        BufferRecycler br = new BufferRecycler(0, 0) { };
        assertThrows(IndexOutOfBoundsException.class, () -> br.allocByteBuffer(0));
        assertThrows(IndexOutOfBoundsException.class, () -> br.allocCharBuffer(0));
        assertThrows(IndexOutOfBoundsException.class, () -> br.releaseByteBuffer(0, new byte[1]));
        assertThrows(IndexOutOfBoundsException.class, () -> br.releaseCharBuffer(0, new char[1]));

        CountingPool pool = new CountingPool();
        assertSame(br, br.withPool(pool));
        assertTrue(br.isLinkedWithPool());
        br.releaseToPool();
        assertFalse(br.isLinkedWithPool());
        assertEquals(1, pool.released.get());
    }

    @Test
    void poolLinkIsReleasedOnce() {
        CountingPool pool = new CountingPool();
        BufferRecycler br = new BufferRecycler().withPool(pool);
        assertThrows(IllegalStateException.class, () -> br.withPool(pool));
        assertTrue(br.isLinkedWithPool());

        br.releaseToPool();
        br.releaseToPool();
        assertEquals(1, pool.released.get());
        assertFalse(br.isLinkedWithPool());
    }

    @Test
    void concurrentPoolReleaseReturnsRecyclerOnce() throws Exception {
        final int trials = 50_000;
        final BufferRecycler br = new BufferRecycler();
        final CountingPool pool = new CountingPool();
        final AtomicInteger bad = new AtomicInteger();
        final Phaser phases = new Phaser(2) {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                if ((phase & 1) == 0) {
                    pool.released.set(0);
                    br.withPool(pool);
                } else if (pool.released.get() != 1 || br.isLinkedWithPool()) {
                    bad.incrementAndGet();
                }
                return false;
            }
        };

        runThreads(2, ix -> {
            for (int i = 0; i < trials; ++i) {
                phases.awaitAdvanceInterruptibly(phases.arrive(), 5, TimeUnit.SECONDS);
                br.releaseToPool();
                phases.awaitAdvanceInterruptibly(phases.arrive(), 5, TimeUnit.SECONDS);
            }
        });

        assertEquals(0, bad.get(), "Each pool link must be released exactly once");
    }

    @Test
    void concurrentPoolLinkHasOneOwner() throws Exception {
        final int trials = 50_000;
        final BufferRecycler br = new BufferRecycler();
        final CountingPool[] pools = { new CountingPool(), new CountingPool() };
        final AtomicInteger linked = new AtomicInteger();
        final AtomicInteger bad = new AtomicInteger();
        final Phaser phases = new Phaser(2) {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                if ((phase & 1) == 0) {
                    linked.set(0);
                    for (CountingPool pool : pools) {
                        pool.released.set(0);
                    }
                } else {
                    if (linked.get() != 1 || !br.isLinkedWithPool()) {
                        bad.incrementAndGet();
                    }
                    br.releaseToPool();
                    if (pools[0].released.get() + pools[1].released.get() != 1) {
                        bad.incrementAndGet();
                    }
                }
                return false;
            }
        };

        runThreads(2, ix -> {
            for (int i = 0; i < trials; ++i) {
                phases.awaitAdvanceInterruptibly(phases.arrive(), 5, TimeUnit.SECONDS);
                try {
                    br.withPool(pools[ix]);
                    linked.incrementAndGet();
                } catch (IllegalStateException e) {
                    // Only one caller may acquire the pool link.
                }
                phases.awaitAdvanceInterruptibly(phases.arrive(), 5, TimeUnit.SECONDS);
            }
        });

        assertEquals(0, bad.get(), "Only one pool may own each link");
    }

    // Releases of different sizes race into one empty slot; the slot must keep the largest.
    @Test
    void concurrentReleaseKeepsLargestBuffer() throws Exception {
        final int threads = Math.max(2, Math.min(THREADS, Runtime.getRuntime().availableProcessors()));
        final int trials = 1000;
        final AtomicReference<BufferRecycler> current = new AtomicReference<>();
        final AtomicInteger notReady = new AtomicInteger();
        final List<String> failures = Collections.synchronizedList(new ArrayList<>());
        final CyclicBarrier barrier = new CyclicBarrier(threads, () -> {
            BufferRecycler br = current.get();
            if (br != null) {
                int bytes = br.allocByteBuffer(BufferRecycler.BYTE_WRITE_CONCAT_BUFFER).length;
                int chars = br.allocCharBuffer(BufferRecycler.CHAR_NAME_COPY_BUFFER).length;
                if (bytes != 3000 + threads - 1 || chars != 300 + threads - 1) {
                    failures.add("bytes=" + bytes + " chars=" + chars);
                }
            }
            current.set(new BufferRecycler());
            notReady.set(threads);
        });
        runThreads(threads, ix -> {
            for (int i = 0; i < trials; ++i) {
                byte[] bytes = new byte[3000 + ix];
                char[] chars = new char[300 + ix];
                barrier.await();
                BufferRecycler br = current.get();
                // start all releases at the same time
                notReady.decrementAndGet();
                while (notReady.get() > 0) {
                    // exit on shutdownNow() if another thread failed
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    Thread.yield();
                }
                br.releaseByteBuffer(BufferRecycler.BYTE_WRITE_CONCAT_BUFFER, bytes);
                br.releaseCharBuffer(BufferRecycler.CHAR_NAME_COPY_BUFFER, chars);
            }
            barrier.await();
        });
        assertEquals(0, failures.size(),
                () -> failures.size() + " of " + trials + " trials lost the largest buffer, first: " + failures.get(0));
    }

    // Allocations and releases race on one slot; no buffer may go to two threads at once.
    @Test
    void concurrentAllocNeverSharesBuffer() throws Exception {
        final int threads = Math.max(2, Math.min(THREADS, Runtime.getRuntime().availableProcessors()));
        final int rounds = 5_000;
        final BufferRecycler br = new BufferRecycler();
        final Set<Object> inUse = ConcurrentHashMap.newKeySet();
        final AtomicInteger bad = new AtomicInteger();
        runThreads(threads, ix -> {
            for (int i = 0; i < rounds; ++i) {
                byte[] bytes = br.allocByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER, 8000 + ix);
                char[] chars = br.allocCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER, 4000 + ix);
                // Track both buffers, and only remove what this thread added
                final boolean ownBytes = inUse.add(bytes);
                final boolean ownChars = inUse.add(chars);
                if (!ownBytes || !ownChars
                        || bytes.length < 8000 + ix || chars.length < 4000 + ix) {
                    bad.incrementAndGet();
                }
                if (ownBytes) {
                    inUse.remove(bytes);
                }
                if (ownChars) {
                    inUse.remove(chars);
                }
                br.releaseByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER, bytes);
                br.releaseCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER, chars);
            }
        });
        assertEquals(0, bad.get());
    }

    interface IndexedTask {
        void run(int ix) throws Exception;
    }

    private static void runThreads(int threads, IndexedTask task) throws Exception {
        // daemon threads, so a stuck one cannot keep the test JVM alive
        ExecutorService exec = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; ++t) {
                final int ix = t;
                futures.add(exec.submit(() -> { task.run(ix); return null; }));
            }
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            exec.shutdownNow();
        }
    }
}
