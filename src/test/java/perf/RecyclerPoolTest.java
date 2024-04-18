package perf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
 * High-concurrency test that tries to see if unbounded {@link RecyclerPool}
 * implementations grow without bounds or not.
 */
public class RecyclerPoolTest
{
    final static int THREAD_COUNT = 100;

    final static int RUNTIME_SECS = 60;

    private final int _threadCount;

    RecyclerPoolTest(int threadCount) {
        _threadCount = threadCount;
    }
    
    public String testPool(JsonFactory jsonF, int runtimeMinutes)
        throws InterruptedException
    {
        RecyclerPool<BufferRecycler> poolImpl = jsonF._getRecyclerPool();

        final String poolName = poolImpl.getClass().getSimpleName();
        final ExecutorService exec = Executors.newFixedThreadPool(_threadCount);
        final AtomicLong calls = new AtomicLong();
        final long startTime = System.currentTimeMillis();
        final long runtimeMsecs = TimeUnit.SECONDS.toMillis(runtimeMinutes);
        final long endtimeMsecs = startTime + runtimeMsecs;
        final AtomicInteger threadsRunning = new AtomicInteger();

        System.out.printf("Starting test of '%s' with %d threads, for %d seconds.\n",
                poolImpl.getClass().getName(),
                _threadCount, runtimeMsecs / 1000L);
        
        for (int i = 0; i < _threadCount; ++i) {
            final int id = i;
            threadsRunning.incrementAndGet();
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    testUntil(jsonF, endtimeMsecs, id, calls);
                    threadsRunning.decrementAndGet();
                }
            });
        }

        long currentTime;
        long nextPrint = 0L;
        // Print if exceeds threshold (3 x threadcount), otherwise every 2.5 seconds
        final int thresholdToPrint = _threadCount * 3;
        int maxPooled = 0;

        while ((currentTime = System.currentTimeMillis()) < endtimeMsecs) {
            int poolSize;

            if ((poolSize = poolImpl.pooledCount()) > thresholdToPrint
                    || (currentTime > nextPrint)) {
                double secs = (currentTime - startTime) / 1000.0;
                System.out.printf(" (%s) %.1fs, %dk calls; %d threads; pool size: %d (max seen: %d)\n",
                        poolName, secs, calls.get()>>10, threadsRunning.get(), poolSize, maxPooled);
                if (poolSize > maxPooled) {
                    maxPooled = poolSize;
                }
                Thread.sleep(100L);
                nextPrint = currentTime + 2500L;
            }
        }

        String desc = String.format("Completed test of '%s': max size seen = %d",
                poolName, maxPooled);
        System.out.printf("%s. Wait termination of threads..\n", desc);
        if (!exec.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
            System.out.printf("WARNING: ExecutorService.awaitTermination() failed: %d threads left; will shut down.\n",
                    threadsRunning.get());
            exec.shutdown();
        }
        return desc;
    }

    void testUntil(JsonFactory jsonF,
             long endTimeMsecs, int threadId, AtomicLong calls)
    {
        final Random rnd = new Random(threadId);
        final byte[] JSON_INPUT = "\"abc\"".getBytes(StandardCharsets.UTF_8);

        while (System.currentTimeMillis() < endTimeMsecs) {
            try {
                // Randomize call order a bit
                switch (rnd.nextInt() & 3) {
                case 0:
                    _testRead(jsonF, JSON_INPUT);
                    break;
                case 1:
                    _testWrite(jsonF);
                    break;
                case 2:
                    _testRead(jsonF, JSON_INPUT);
                    _testWrite(jsonF);
                    break;
                default:
                    _testWrite(jsonF);
                    _testRead(jsonF, JSON_INPUT);
                    break;
                }
            } catch (Exception e) {
                System.err.printf("ERROR: thread %d fail, will exit: (%s) %s\n",
                        threadId, e.getClass().getName(), e.getMessage());
                break;
            }
            calls.incrementAndGet();
        }
    }

    private void _testRead(JsonFactory jsonF, byte[] input) throws Exception
    {
        JsonParser p = jsonF.createParser(ObjectReadContext.empty(),
                new ByteArrayInputStream(input));
        while (p.nextToken() != null) {
            ;
        }
        p.close();
    }
    
    private void _testWrite(JsonFactory jsonF) throws Exception
    {
        StringWriter w = new StringWriter(16);
        JsonGenerator g = jsonF.createGenerator(ObjectWriteContext.empty(), w);
        g.writeStartArray();
        g.writeString("foobar");
        g.writeEndArray();
        g.close();
    }

    public static void main(String[] args) throws Exception
    {
        RecyclerPoolTest test = new RecyclerPoolTest(THREAD_COUNT);
        List<String> results = Arrays.asList(
            test.testPool(JsonFactory.builder()
                    .recyclerPool(JsonRecyclerPools.newConcurrentDequePool())
                    .build(),
                RUNTIME_SECS),
            test.testPool(JsonFactory.builder()
                    .recyclerPool(JsonRecyclerPools.newBoundedPool(THREAD_COUNT - 5))
                    .build(),
                RUNTIME_SECS),
            test.testPool(JsonFactory.builder()
                    .recyclerPool(JsonRecyclerPools.newLockFreePool())
                    .build(),
                RUNTIME_SECS)
        );

        System.out.println("Tests complete! Results:\n");
        for (String result : results) {
            System.out.printf(" * %s\n", result);
        }
    }
}
