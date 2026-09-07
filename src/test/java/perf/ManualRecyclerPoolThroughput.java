package perf;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

/**
 * Manually-run throughput comparison of the {@link RecyclerPool}
 * implementations under contention: N platform threads (and, on JDK 21+,
 * virtual threads in two more passes) do small write+read cycles for a fixed
 * time per pool, and the harness prints operations per second.
 *<p>
 * The two virtual-thread passes cover different lifecycles. The long-lived
 * pass keeps N virtual threads looping, so per-thread state survives across
 * cycles. The per-task pass starts a fresh virtual thread for every cycle
 * (N in flight at a time) - the thread-per-task lifecycle that abandons any
 * per-thread pool state after a single use, which is the case a shared
 * structure for virtual threads exists for.
 *<p>
 * Usage: {@code ManualRecyclerPoolThroughput [threads [warmupSecs [measureSecs]]]}
 * (defaults: 4, 5, 10).
 *<p>
 * Indicative numbers only: single fork, no statistical protocol. For
 * publishable comparisons use a paired JMH setup.
 */
public class ManualRecyclerPoolThroughput
{
    private enum Lifecycle {
        PLATFORM,
        VIRTUAL_LONG_LIVED,
        VIRTUAL_PER_TASK
    }

    final int _threadCount;
    final int _warmupSecs;
    final int _measureSecs;

    ManualRecyclerPoolThroughput(int threadCount, int warmupSecs, int measureSecs) {
        _threadCount = threadCount;
        _warmupSecs = warmupSecs;
        _measureSecs = measureSecs;
    }

    public static void main(String[] args) throws Exception
    {
        final int threads = (args.length > 0) ? Integer.parseInt(args[0]) : 4;
        final int warmupSecs = (args.length > 1) ? Integer.parseInt(args[1]) : 5;
        final int measureSecs = (args.length > 2) ? Integer.parseInt(args[2]) : 10;
        new ManualRecyclerPoolThroughput(threads, warmupSecs, measureSecs).run();
    }

    void run() throws Exception
    {
        Map<String, RecyclerPool<BufferRecycler>> pools = new LinkedHashMap<>();
        pools.put("nonRecycling", JsonRecyclerPools.nonRecyclingPool());
        pools.put("threadLocal", JsonRecyclerPools.threadLocalPool());
        pools.put("concurrentDeque", JsonRecyclerPools.newConcurrentDequePool());
        pools.put("bounded", JsonRecyclerPools.newBoundedPool(100));
        pools.put("stripedArray", JsonRecyclerPools.newStripedArrayPool());
        pools.put("hybrid", JsonRecyclerPools.newHybridPool());

        System.out.printf("Platform threads (%d):%n", _threadCount);
        runPools(pools, Lifecycle.PLATFORM);

        if (virtualThreadsAvailable()) {
            System.out.printf("%nVirtual threads, long-lived (%d):%n", _threadCount);
            runPools(pools, Lifecycle.VIRTUAL_LONG_LIVED);
            System.out.printf("%nVirtual threads, per-task (%d in flight):%n", _threadCount);
            runPools(pools, Lifecycle.VIRTUAL_PER_TASK);
        } else {
            System.out.println("\n(virtual threads unavailable; platform pass only)");
        }
    }

    private void runPools(Map<String, RecyclerPool<BufferRecycler>> pools,
            Lifecycle lifecycle)
        throws Exception
    {
        for (Map.Entry<String, RecyclerPool<BufferRecycler>> e : pools.entrySet()) {
            long opsPerSec = runPass(e.getValue(), lifecycle);
            System.out.printf(" * %-16s %,12d ops/s%n", e.getKey(), opsPerSec);
        }
    }

    private long runPass(RecyclerPool<BufferRecycler> pool, Lifecycle lifecycle)
        throws Exception
    {
        JsonFactory jsonF = JsonFactory.builder().recyclerPool(pool).build();
        runThreads(jsonF, lifecycle, _warmupSecs, new AtomicLong());
        AtomicLong ops = new AtomicLong();
        runThreads(jsonF, lifecycle, _measureSecs, ops);
        return ops.get() / _measureSecs;
    }

    private void runThreads(JsonFactory jsonF, Lifecycle lifecycle,
            int seconds, AtomicLong ops)
        throws Exception
    {
        final long endMsecs = System.currentTimeMillis()
                + TimeUnit.SECONDS.toMillis(seconds);
        final CountDownLatch done = new CountDownLatch(_threadCount);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < _threadCount; ++i) {
            Runnable work = () -> {
                try {
                    if (lifecycle == Lifecycle.VIRTUAL_PER_TASK) {
                        // Fresh virtual thread per cycle: pool interactions
                        // happen on a thread that dies after one use.
                        while (System.currentTimeMillis() < endMsecs) {
                            Thread vt = startVirtualThread(() -> {
                                try {
                                    oneCycle(jsonF);
                                } catch (Exception e) {
                                    System.err.println("ERROR: worker failed: "+e);
                                }
                            });
                            vt.join();
                            ops.incrementAndGet();
                        }
                    } else {
                        while (System.currentTimeMillis() < endMsecs) {
                            oneCycle(jsonF);
                            ops.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("ERROR: worker failed: "+e);
                } finally {
                    done.countDown();
                }
            };
            threads.add((lifecycle == Lifecycle.VIRTUAL_LONG_LIVED)
                    ? startVirtualThread(work) : startPlatformThread(work));
        }
        done.await();
        for (Thread t : threads) {
            t.join();
        }
    }

    private final static byte[] JSON_INPUT = "{\"a\":42,\"b\":\"foobar\"} "
            .getBytes(StandardCharsets.UTF_8);

    private static void oneCycle(JsonFactory jsonF) throws Exception
    {
        StringWriter w = new StringWriter(24);
        try (JsonGenerator g = jsonF.createGenerator(ObjectWriteContext.empty(), w)) {
            g.writeStartObject();
            g.writeNumberProperty("a", 42);
            g.writeStringProperty("b", "foobar");
            g.writeEndObject();
        }
        try (JsonParser p = jsonF.createParser(ObjectReadContext.empty(),
                new ByteArrayInputStream(JSON_INPUT))) {
            while (p.nextToken() != null) {
                ;
            }
        }
    }

    private static Thread startPlatformThread(Runnable r) {
        Thread t = new Thread(r);
        t.start();
        return t;
    }

    // Reflective so the harness compiles and runs on the JDK 17 floor
    private static boolean virtualThreadsAvailable() {
        try {
            Thread.class.getMethod("startVirtualThread", Runnable.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Thread startVirtualThread(Runnable r) {
        try {
            Method m = Thread.class.getMethod("startVirtualThread", Runnable.class);
            return (Thread) m.invoke(null, r);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
