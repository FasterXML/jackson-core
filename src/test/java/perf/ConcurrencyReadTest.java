package perf;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.*;

/**
 * Manual performance test to try out various synchronization
 * methods for symbol tables.
 */
public class ConcurrencyReadTest
{
    private final static int THREADS = 50;

    private void test() throws Exception
    {
        final JsonFactory jf = new JsonFactory();
        final byte[] INPUT = "{\"a\":1}".getBytes("UTF-8");
        final AtomicInteger count = new AtomicInteger();

        for (int i = 0; i < THREADS; ++i) {
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    try {
                        while (true) {
                            parse(jf, INPUT);
                            count.addAndGet(1);
                        }
                    } catch (IOException e) {
                        System.err.println("PROBLEM: "+e);
                    }
                }
            }).start();
        }

        // wait slightly....
        Thread.sleep(200L);

        double totalTime = 0.0;
        double totalCount = 0.0;

        while (true) {
            long start = System.currentTimeMillis();
            int startCount = count.get();

            Thread.sleep(1000L);

            int done = count.get() - startCount;
            long time = System.currentTimeMillis() - start;

            totalTime += time;
            totalCount += done;

            double rate = (double) done / (double) time;
            System.out.printf("Rate: %.1f (avg: %.1f)\n", rate, totalCount/totalTime);
        }
    }

    protected void parse(JsonFactory jf, byte[] input) throws IOException
    {
        JsonParser jp = jf.createParser(input, 0, input.length);
        while (jp.nextToken() != null) {
            ;
        }
        jp.close();
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        new ConcurrencyReadTest().test();
    }

}
