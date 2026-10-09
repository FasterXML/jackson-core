package tools.jackson.core.unittest.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonRecyclerPools.StripedArrayPool} and
 * {@link JsonRecyclerPools.HybridPool}.
 */
class StripedAndHybridPoolTest extends JacksonCoreTestBase
{
    /*
    /**********************************************************************
    /* StripedArrayPool: bounds, capacity, clear
    /**********************************************************************
     */

    @Test
    void stripedPoolLifecycle() {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newStripedArrayPool(4);

        BufferRecycler br = pool.acquireAndLinkPooled();
        assertNotNull(br);
        assertEquals(0, pool.pooledCount());
        br.releaseToPool();
        assertEquals(1, pool.pooledCount());
        // Same thread gets the same instance back
        assertSame(br, pool.acquireAndLinkPooled());
        br.releaseToPool();
    }

    @Test
    void stripedPoolDoesNotExceedCapacity() {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newStripedArrayPool(4);

        List<BufferRecycler> burst = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            burst.add(pool.acquireAndLinkPooled());
        }
        for (BufferRecycler br : burst) {
            br.releaseToPool();
        }
        assertEquals(4, pool.pooledCount());
    }

    @Test
    void stripedPoolCapacityRoundsUpToPowerOfTwo() {
        assertEquals(4, capacityOf(JsonRecyclerPools.newStripedArrayPool(3)));
        assertEquals(4, capacityOf(JsonRecyclerPools.newStripedArrayPool(4)));
        assertEquals(16, capacityOf(JsonRecyclerPools.newStripedArrayPool(10)));
        assertEquals(16, capacityOf(JsonRecyclerPools.newStripedArrayPool()));
        assertThrows(IllegalArgumentException.class,
                () -> JsonRecyclerPools.newStripedArrayPool(0));
    }

    private static int capacityOf(RecyclerPool<BufferRecycler> pool) {
        return ((RecyclerPool.StripedArrayPoolBase<BufferRecycler>) pool).capacity();
    }

    @Test
    void stripedPoolClearDropsRetainedRecyclers() {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newStripedArrayPool(4);

        BufferRecycler br1 = pool.acquireAndLinkPooled();
        BufferRecycler br2 = pool.acquireAndLinkPooled();
        br1.releaseToPool();
        br2.releaseToPool();
        assertEquals(2, pool.pooledCount());

        assertTrue(pool.clear());
        assertEquals(0, pool.pooledCount());
        assertNotSame(br1, pool.acquireAndLinkPooled());
    }

    /*
    /**********************************************************************
    /* HybridPool: platform-thread path
    /**********************************************************************
     */

    @Test
    void hybridPoolPlatformThreadLeaveIn() {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newHybridPool(4);

        BufferRecycler br = pool.acquireAndLinkPooled();
        assertFalse(br.isLinkedWithPool(),
                "platform-thread instance must be unlinked (leave-in)");
        br.releaseToPool(); // no-op
        assertSame(br, pool.acquireAndLinkPooled(),
                "same platform thread must get its leave-in instance back");
        // Leave-in instances are not tracked by the shared slots
        assertEquals(0, pool.pooledCount());
        // clear() drops the shared slots but cannot drop leave-ins: false
        assertFalse(pool.clear());
        assertSame(br, pool.acquireAndLinkPooled());
    }

    @Test
    void hybridPoolDistinctInstancePerPlatformThread() throws Exception {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newHybridPool(4);
        BufferRecycler own = pool.acquireAndLinkPooled();

        BufferRecycler[] other = new BufferRecycler[1];
        Thread t = new Thread(() -> other[0] = pool.acquireAndLinkPooled());
        t.start();
        t.join();

        assertNotNull(other[0]);
        assertNotSame(own, other[0]);
    }

    /*
    /**********************************************************************
    /* HybridPool: virtual-thread path (skipped below JDK 21)
    /**********************************************************************
     */

    @Test
    void hybridPoolVirtualThreadsShareSlots() throws Exception {
        Assumptions.assumeTrue(VIRTUAL_THREADS_AVAILABLE, "virtual threads unavailable");
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newHybridPool(4);

        BufferRecycler[] acquired = new BufferRecycler[2];
        boolean[] linked = new boolean[1];
        runInVirtualThread(() -> {
            acquired[0] = pool.acquireAndLinkPooled();
            linked[0] = acquired[0].isLinkedWithPool();
            acquired[0].releaseToPool();
        });
        assertTrue(linked[0], "virtual-thread instance must be linked to the pool");
        assertEquals(1, pool.pooledCount(),
                "released virtual-thread instance must land in the shared slots");

        runInVirtualThread(() -> acquired[1] = pool.acquireAndLinkPooled());
        assertSame(acquired[0], acquired[1],
                "second virtual thread must recycle the first one's instance");
    }

    /*
    /**********************************************************************
    /* Double-lease stress (linked instances only)
    /**********************************************************************
     */

    @Test
    void stripedPoolStressNoDoubleLease() throws Exception {
        assertNoDoubleLease(JsonRecyclerPools.newStripedArrayPool(4), false);
    }

    @Test
    void hybridPoolStressNoDoubleLease() throws Exception {
        assertNoDoubleLease(JsonRecyclerPools.newHybridPool(4), VIRTUAL_THREADS_AVAILABLE);
    }

    // Hammers acquire/release from platform threads (and virtual threads when
    // available) and verifies no linked instance is ever held by two owners.
    // Platform-thread leave-ins from HybridPool are unlinked and thread-confined,
    // so only linked instances participate in ownership tracking.
    private void assertNoDoubleLease(RecyclerPool<BufferRecycler> pool, boolean useVirtual)
        throws Exception
    {
        final int threads = 8;
        final int iterations = 2000;
        final ConcurrentHashMap<BufferRecycler, Thread> owners = new ConcurrentHashMap<>();
        final AtomicInteger doubleLeases = new AtomicInteger();
        final CountDownLatch start = new CountDownLatch(1);

        Runnable work = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            for (int i = 0; i < iterations; ++i) {
                BufferRecycler br = pool.acquireAndLinkPooled();
                if (br.isLinkedWithPool()) {
                    if (owners.putIfAbsent(br, Thread.currentThread()) != null) {
                        doubleLeases.incrementAndGet();
                    }
                    // Touch a buffer like real use does
                    br.releaseByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER,
                            br.allocByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER));
                    if (ThreadLocalRandom.current().nextInt(8) == 0) {
                        Thread.yield();
                    }
                    owners.remove(br);
                }
                br.releaseToPool();
            }
        };

        ExecutorService exec = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; ++i) {
                futures.add(exec.submit(work));
            }
            List<Thread> virtualThreads = new ArrayList<>();
            if (useVirtual) {
                for (int i = 0; i < threads; ++i) {
                    virtualThreads.add(startVirtualThread(work));
                }
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
            for (Thread t : virtualThreads) {
                t.join(TimeUnit.SECONDS.toMillis(60));
                assertFalse(t.isAlive(), "virtual worker did not finish");
            }
        } finally {
            exec.shutdownNow();
        }

        assertEquals(0, doubleLeases.get(), "instance leased to two owners at once");
        int pooled = pool.pooledCount();
        assertTrue(pooled >= 0 && pooled <= 4, "retention bound exceeded: "+pooled);
    }

    /*
    /**********************************************************************
    /* JDK serialization
    /**********************************************************************
     */

    @Test
    void stripedPoolSerialization() throws Exception {
        // Shared instance resolves back to the global one
        assertSame(JsonRecyclerPools.sharedStripedArrayPool(),
                jdkRoundTrip(JsonRecyclerPools.sharedStripedArrayPool()));

        // Non-shared resolves to a fresh, working pool with the same capacity
        RecyclerPool<BufferRecycler> orig = JsonRecyclerPools.newStripedArrayPool(8);
        RecyclerPool<BufferRecycler> copy = jdkRoundTrip(orig);
        assertNotSame(orig, copy);
        assertEquals(8, capacityOf(copy));
        BufferRecycler br = copy.acquireAndLinkPooled();
        br.releaseToPool();
        assertEquals(1, copy.pooledCount());
    }

    @Test
    void hybridPoolSerialization() throws Exception {
        assertSame(JsonRecyclerPools.sharedHybridPool(),
                jdkRoundTrip(JsonRecyclerPools.sharedHybridPool()));

        RecyclerPool<BufferRecycler> orig = JsonRecyclerPools.newHybridPool(8);
        RecyclerPool<BufferRecycler> copy = jdkRoundTrip(orig);
        assertNotSame(orig, copy);
        assertEquals(8, capacityOf(copy));
        assertNotNull(copy.acquireAndLinkPooled());
    }

    @SuppressWarnings("unchecked")
    private static RecyclerPool<BufferRecycler> jdkRoundTrip(RecyclerPool<BufferRecycler> pool)
        throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(pool);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (RecyclerPool<BufferRecycler>) in.readObject();
        }
    }

    /*
    /**********************************************************************
    /* Virtual-thread reflection helpers (JDK 17 source level)
    /**********************************************************************
     */

    private static final boolean VIRTUAL_THREADS_AVAILABLE = _virtualThreadsAvailable();

    private static boolean _virtualThreadsAvailable() {
        try {
            Thread.class.getMethod("ofVirtual");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Thread startVirtualThread(Runnable r) throws Exception {
        Method m = Thread.class.getMethod("startVirtualThread", Runnable.class);
        return (Thread) m.invoke(null, r);
    }

    private static void runInVirtualThread(Runnable r) throws Exception {
        Thread t = startVirtualThread(r);
        t.join(TimeUnit.SECONDS.toMillis(30));
        assertFalse(t.isAlive(), "virtual thread did not finish");
    }
}
