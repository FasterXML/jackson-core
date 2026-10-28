package tools.jackson.core.unittest.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    // Oversized requests must clamp, not overflow the slot array
    @Test
    void stripedPoolCapacityClampedToMax() {
        final int max = RecyclerPool.StripedArrayPoolBase.MAX_CAPACITY;
        assertEquals(max, capacityOf(JsonRecyclerPools.newStripedArrayPool(max)));
        assertEquals(max, capacityOf(JsonRecyclerPools.newStripedArrayPool(max + 1)));
        for (int size : new int[] { 1_000_000, 100_000_000, (1 << 28) - 1,
                (1 << 30) + 1, Integer.MAX_VALUE }) {
            RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newStripedArrayPool(size);
            assertEquals(max, capacityOf(pool));
            BufferRecycler br = pool.acquireAndLinkPooled();
            br.releaseToPool();
            assertEquals(1, pool.pooledCount());
        }
        assertEquals(max, capacityOf(JsonRecyclerPools.newHybridPool(Integer.MAX_VALUE)));
    }

    private static int capacityOf(RecyclerPool<BufferRecycler> pool) {
        if (pool instanceof RecyclerPool.HybridPoolBase<BufferRecycler> hybrid) {
            return hybrid.capacity();
        }
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

    // Direct (unlinked) API uses the shared slots, never the leave-in
    @Test
    void hybridPoolPlatformThreadDirectAcquireRelease() {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newHybridPool(4);

        BufferRecycler leaveIn = pool.acquireAndLinkPooled();
        BufferRecycler br = pool.acquirePooled();
        assertNotSame(leaveIn, br);
        pool.releasePooled(br);
        assertEquals(1, pool.pooledCount());
        assertSame(br, pool.acquirePooled());
        assertSame(leaveIn, pool.acquireAndLinkPooled());
    }

    // Platform threads use the ThreadLocalPool leave-in: one per thread for all
    // pools, not one per pool
    @Test
    void hybridPoolSharesThreadLocalPoolLeaveIn() {
        BufferRecycler leaveIn = JsonRecyclerPools.threadLocalPool().acquireAndLinkPooled();
        assertSame(leaveIn, JsonRecyclerPools.newHybridPool(4).acquireAndLinkPooled());
        assertSame(leaveIn, JsonRecyclerPools.sharedHybridPool().acquireAndLinkPooled());
    }

    // capacity() and pooledCount() describe the shared slots only
    @Test
    void hybridPoolCountsSharedSlotsOnly() {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newHybridPool(10);
        assertEquals(16, capacityOf(pool));

        pool.acquireAndLinkPooled();
        assertEquals(0, pool.pooledCount());
        BufferRecycler br = pool.acquirePooled();
        pool.releasePooled(br);
        assertEquals(1, pool.pooledCount());
        assertFalse(pool.clear());
        assertEquals(0, pool.pooledCount());
        assertEquals(16, capacityOf(pool));
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

    // An instance acquired directly on a platform thread and released on a
    // virtual thread must not stay the platform thread's leave-in
    @Test
    void hybridPoolNoDoubleLeaseAcrossThreadTypes() throws Exception {
        Assumptions.assumeTrue(VIRTUAL_THREADS_AVAILABLE, "virtual threads unavailable");
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newHybridPool(4);

        BufferRecycler br = pool.acquirePooled();
        runInVirtualThread(() -> pool.releasePooled(br));
        BufferRecycler[] onVirtual = new BufferRecycler[1];
        runInVirtualThread(() -> onVirtual[0] = pool.acquireAndLinkPooled());

        assertSame(br, onVirtual[0]);
        assertNotSame(onVirtual[0], pool.acquireAndLinkPooled(),
                "instance leased to a virtual thread and a platform thread at once");
        assertNotSame(onVirtual[0], pool.acquirePooled(),
                "instance leased to a virtual thread and a platform thread at once");
    }

    // A linked instance acquired on a virtual thread and released on a
    // platform thread must return to the shared slots
    @Test
    void hybridPoolVirtualAcquirePlatformRelease() throws Exception {
        Assumptions.assumeTrue(VIRTUAL_THREADS_AVAILABLE, "virtual threads unavailable");
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newHybridPool(4);

        BufferRecycler[] acquired = new BufferRecycler[2];
        runInVirtualThread(() -> acquired[0] = pool.acquireAndLinkPooled());
        assertTrue(acquired[0].isLinkedWithPool());
        acquired[0].releaseToPool();
        assertEquals(1, pool.pooledCount(),
                "release on a platform thread must return the instance to the slots");

        runInVirtualThread(() -> acquired[1] = pool.acquireAndLinkPooled());
        assertSame(acquired[0], acquired[1]);
    }

    /*
    /**********************************************************************
    /* Double-lease stress
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
    // available) and verifies no instance is ever held by two owners. Half the
    // cycles use the direct API; some instances are released by another thread
    // (of either type) than the one that acquired them. Platform-thread
    // leave-ins from HybridPool are unlinked and thread-confined, so they are
    // not tracked.
    private void assertNoDoubleLease(RecyclerPool<BufferRecycler> pool, boolean useVirtual)
        throws Exception
    {
        final int threads = 8;
        final int iterations = 2000;
        final ConcurrentHashMap<BufferRecycler, Thread> owners = new ConcurrentHashMap<>();
        final AtomicInteger doubleLeases = new AtomicInteger();
        final CountDownLatch start = new CountDownLatch(1);
        // Acquired but not yet released; any thread may release them
        final Queue<BufferRecycler> linkedHandoff = new ConcurrentLinkedQueue<>();
        final Queue<BufferRecycler> directHandoff = new ConcurrentLinkedQueue<>();
        // Virtual threads are not run via executor, so failures must be captured
        final Queue<Throwable> virtualFailures = new ConcurrentLinkedQueue<>();

        Runnable work = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            for (int i = 0; i < iterations; ++i) {
                releaseHandedOff(pool, owners, linkedHandoff, directHandoff);
                final boolean direct = (i & 1) != 0;
                BufferRecycler br = direct ? pool.acquirePooled() : pool.acquireAndLinkPooled();
                if (!direct && !br.isLinkedWithPool()) {
                    br.releaseToPool(); // leave-in: no-op
                    continue;
                }
                if (owners.putIfAbsent(br, Thread.currentThread()) != null) {
                    doubleLeases.incrementAndGet();
                }
                // Touch a buffer like real use does
                br.releaseByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER,
                        br.allocByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER));
                final int r = ThreadLocalRandom.current().nextInt(8);
                if (r == 0) {
                    Thread.yield();
                } else if (r == 1) {
                    (direct ? directHandoff : linkedHandoff).add(br);
                    continue;
                }
                owners.remove(br);
                if (direct) {
                    pool.releasePooled(br);
                } else {
                    br.releaseToPool();
                }
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
                    virtualThreads.add(startVirtualThread(work, virtualFailures));
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
        while (releaseHandedOff(pool, owners, linkedHandoff, directHandoff)) { }

        assertNoFailures(virtualFailures);
        assertEquals(0, doubleLeases.get(), "instance leased to two owners at once");
        int pooled = pool.pooledCount();
        assertTrue(pooled >= 0 && pooled <= 4, "retention bound exceeded: "+pooled);
    }

    private static boolean releaseHandedOff(RecyclerPool<BufferRecycler> pool,
            ConcurrentHashMap<BufferRecycler, Thread> owners,
            Queue<BufferRecycler> linkedHandoff, Queue<BufferRecycler> directHandoff)
    {
        BufferRecycler br = linkedHandoff.poll();
        if (br != null) {
            owners.remove(br);
            br.releaseToPool();
            return true;
        }
        br = directHandoff.poll();
        if (br != null) {
            owners.remove(br);
            pool.releasePooled(br);
            return true;
        }
        return false;
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

    // Runs `r` in a new virtual thread, recording anything it throws into `failures`
    private static Thread startVirtualThread(Runnable r, Queue<Throwable> failures)
        throws Exception
    {
        Runnable wrapped = () -> {
            try {
                r.run();
            } catch (Throwable t) {
                failures.add(t);
            }
        };
        Method m = Thread.class.getMethod("startVirtualThread", Runnable.class);
        return (Thread) m.invoke(null, wrapped);
    }

    private static void runInVirtualThread(Runnable r) throws Exception {
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        Thread t = startVirtualThread(r, failures);
        t.join(TimeUnit.SECONDS.toMillis(30));
        assertFalse(t.isAlive(), "virtual thread did not finish");
        assertNoFailures(failures);
    }

    private static void assertNoFailures(Queue<Throwable> failures) {
        Throwable first = failures.peek();
        if (first != null) {
            AssertionError e = new AssertionError(failures.size()
                    +" virtual thread(s) failed; first: "+first, first);
            failures.stream().skip(1).forEach(e::addSuppressed);
            throw e;
        }
    }
}
