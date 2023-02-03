package perf;

abstract class ManualPerfTestBase
{
    protected int hash;

    protected <T1, T2> void test(String desc1, String desc2, int expSize)
        throws Exception
    {
        // guessing we have 500 byte
        final int REPS = (int) ((double) (10 * 1000 * 1000) / (double) expSize);

        System.out.printf("Estimating %d bytes to read; will do %d repetitions\n",
                expSize, REPS);

        int i = 0;
        int roundsDone = 0;
        final int TYPES = 2;
        final int WARMUP_ROUNDS = 5;

        final long[] times = new long[TYPES];

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % TYPES);

            String msg;
            boolean lf = (round == 0);

            long msecs;

            switch (round) {
            case 0:
                msg = desc1;
                msecs = _testRead1(REPS);
                break;
            case 1:
                msg = desc2;
                msecs = _testRead2(REPS);
                break;
            default:
                throw new Error();
            }

            // skip first 5 rounds to let results stabilize
            if (roundsDone >= WARMUP_ROUNDS) {
                times[round] += msecs;
            }

            System.out.printf("Test '%s' [hash: 0x%s] -> %d msecs\n", msg, this.hash, msecs);
            if (lf) {
                ++roundsDone;
                if ((roundsDone % 3) == 0 && roundsDone > WARMUP_ROUNDS) {
                    double den = (double) (roundsDone - WARMUP_ROUNDS);
                    System.out.printf("Averages after %d rounds ("+desc1+" / "+desc2+"): %.1f / %.1f msecs\n",
                            (int) den,
                            times[0] / den, times[1] / den);

                }
                System.out.println();
            }
            if ((i % 17) == 0) {
                System.out.println("[GC]");
                Thread.sleep(100L);
                System.gc();
                Thread.sleep(100L);
            }
        }
    }

    protected long _testRead1(int reps) throws Exception {
        final long start = System.currentTimeMillis();
        testRead1(reps);
        return System.currentTimeMillis() - start;
    }

    protected long _testRead2(int reps) throws Exception {
        final long start = System.currentTimeMillis();
        testRead2(reps);
        return System.currentTimeMillis() - start;
    }

    protected abstract void testRead1(int reps) throws Exception;

    protected abstract void testRead2(int reps) throws Exception;

    protected static String a2q(String json) {
        return json.replace("'", "\"");
    }
}

