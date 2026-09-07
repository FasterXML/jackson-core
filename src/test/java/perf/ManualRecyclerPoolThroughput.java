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
 * N virtual threads in a second pass) do small write+read cycles for a fixed
 * time per pool, and the harness prints operations per second.
 *<p>
 * Indicative numbers only: single fork, no statistical protocol. For
 * publishable comparisons use a paired JMH setup.
 */
public class ManualRecyclerPoolThroughput
{
    final static int THREAD_COUNT = 4;

    final static int WARMUP_SECS = 5;

    final static int MEASURE_SECS = 10;

    public static void main(String[] args) throws Exception
    {
        Map<String, RecyclerPool<BufferRecycler>> pools = new LinkedHashMap<>();
        pools.put("nonRecycling", JsonRecyclerPools.nonRecyclingPool());
        pools.put("threadLocal", JsonRecyclerPools.threadLocalPool());
        pools.put("concurrentDeque", JsonRecyclerPools.newConcurrentDequePool());
        pools.put("bounded", JsonRecyclerPools.newBoundedPool(100));
        pools.put("stripedArray", JsonRecyclerPools.newStripedArrayPool());
        pools.put("hybrid", JsonRecyclerPools.newHybridPool());

        System.out.printf("Platform threads (%d):%n", THREAD_COUNT);
        for (Map.Entry<String, RecyclerPool<BufferRecycler>> e : pools.entrySet()) {
            long opsPerSec = runPass(e.getValue(), false);
            System.out.printf(" * %-16s %,12d ops/s%n", e.getKey(), opsPerSec);
        }

        if (virtualThreadsAvailable()) {
            System.out.printf("%nVirtual threads (%d):%n", THREAD_COUNT);
            for (Map.Entry<String, RecyclerPool<BufferRecycler>> e : pools.entrySet()) {
                long opsPerSec = runPass(e.getValue(), true);
                System.out.printf(" * %-16s %,12d ops/s%n", e.getKey(), opsPerSec);
            }
        } else {
            System.out.println("\n(virtual threads unavailable; platform pass only)");
        }
    }

    private static long runPass(RecyclerPool<BufferRecycler> pool, boolean virtual)
        throws Exception
    {
        JsonFactory jsonF = JsonFactory.builder().recyclerPool(pool).build();
        runThreads(jsonF, virtual, WARMUP_SECS, new AtomicLong());
        AtomicLong ops = new AtomicLong();
        runThreads(jsonF, virtual, MEASURE_SECS, ops);
        return ops.get() / MEASURE_SECS;
    }

    private static void runThreads(JsonFactory jsonF, boolean virtual,
            int seconds, AtomicLong ops)
        throws Exception
    {
        final long endMsecs = System.currentTimeMillis()
                + TimeUnit.SECONDS.toMillis(seconds);
        final CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; ++i) {
            Runnable work = () -> {
                try {
                    while (System.currentTimeMillis() < endMsecs) {
                        oneCycle(jsonF);
                        ops.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("ERROR: worker failed: "+e);
                } finally {
                    done.countDown();
                }
            };
            threads.add(virtual ? startVirtualThread(work) : startPlatformThread(work));
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
