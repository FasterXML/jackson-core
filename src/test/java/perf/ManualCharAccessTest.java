package perf;

public class ManualCharAccessTest
{
    protected int hash;

    protected final static byte[] SMALL_BYTE_CODES = new byte[256];

    protected final static int[] SMALL_INT_CODES = new int[256];

    protected final static int[] INT_CODES = new int[0x10000];
    protected final static byte[] BYTE_CODES = new byte[0x10000];

    static {
        for (int i = 0; i < 32; ++i) {
            if (!(i == '\r' || i == '\n' || i == '\t')) {
                INT_CODES[i] = 1;
                BYTE_CODES[i] = 1;
                SMALL_BYTE_CODES[i] = 1;
                SMALL_INT_CODES[i] = 1;
            }
        }
        INT_CODES['\\'] = 2;
        BYTE_CODES['\\'] = 2;
        SMALL_BYTE_CODES['\\'] = 2;
        SMALL_INT_CODES['\\'] = 2;
    }

    protected String generateString(int len)
    {
        int counter = 0;
        StringBuilder sb = new StringBuilder(len + 20);
        do {
            sb.append("Some stuff: ").append(len).append("\n");
            if ((++counter % 31) == 0) {
                sb.append("\\");
            }
        } while (sb.length() < len);
        return sb.toString();
    }

    private void test() throws Exception
    {
        final String INPUT_STR = generateString(23000);
        final char[] INPUT_CHARS = INPUT_STR.toCharArray();
        final char[] OUTPUT = new char[INPUT_CHARS.length];

        // Let's try to guestimate suitable size, N megs of output
        final int REPS = (int) ((double) (80 * 1000 * 1000) / (double) INPUT_CHARS.length);
        System.out.printf("%d bytes to scan, will do %d repetitions\n",
                INPUT_CHARS.length, REPS);

        int i = 0;
        int roundsDone = 0;
        final int TYPES = 3;
        final int WARMUP_ROUNDS = 5;

        final long[] times = new long[TYPES];

        while (true) {
            int round = (i++ % TYPES);

            String msg;
            boolean lf = (round == 0);

            long msecs;

            switch (round) {
            case 0:
                msg = "Read classic";
                msecs = readClassic(REPS, INPUT_CHARS, OUTPUT);
                break;
            case 1:
                msg = "Read, byte[]";
                msecs = readWithByte(REPS, INPUT_CHARS, OUTPUT);
                break;
            case 2:
                msg = "Read, int[]";
                msecs = readWithInt(REPS, INPUT_CHARS, OUTPUT);
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
                if ((roundsDone % 7) == 0 && roundsDone > WARMUP_ROUNDS) {
                    double den = (double) (roundsDone - WARMUP_ROUNDS);
                    System.out.printf("Averages after %d rounds (classic, byte[], int[]): "
                            +"%.1f / %.1f / %.1f msecs\n",
                            (int) den
                            ,times[0] / den, times[1] / den, times[2] / den
                            );

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

    private final long readClassic(int REPS, char[] input, char[] output) throws Exception
    {
        long start = System.currentTimeMillis();
        final byte[] codes = SMALL_BYTE_CODES;
        final int MAX = 256;

        while (--REPS >= 0) {
            int outPtr = 0;
            for (int i = 0, end = input.length; i < end; ++i) {
                int ch = input[i];
                if (ch < MAX && codes[ch] == NULL_BYTE) {
                    output[outPtr++] = (char) ch;
                    continue;
                }
                if (ch == '\\') {
                    output[outPtr++] = '_';
                } else if (ch == '\n') {
                    output[outPtr++] = '_';
                }
            }
        }
        long time = System.currentTimeMillis() - start;
        return time;
    }

    private final long readWithByte(int REPS, char[] input, char[] output) throws Exception
    {
        long start = System.currentTimeMillis();
        final byte[] codes = BYTE_CODES;
        while (--REPS >= 0) {
            int outPtr = 0;
            for (int i = 0, end = input.length; i < end; ++i) {
                char ch = input[i];
                if (codes[ch] == NULL_BYTE) {
                    output[outPtr++] = ch;
                    continue;
                }
                if (ch == '\\') {
                    output[outPtr++] = '_';
                } else if (ch == '\n') {
                    output[outPtr++] = '_';
                }
            }
        }
        long time = System.currentTimeMillis() - start;
        return time;
    }

    final static byte NULL_BYTE = (byte) 0;

    private final long readWithInt(int REPS, char[] input, char[] output) throws Exception
    {
        long start = System.currentTimeMillis();
        final int[] codes = INT_CODES;
        while (--REPS >= 0) {
            int outPtr = 0;

            for (int i = 0, end = input.length; i < end; ++i) {
                char ch = input[i];
                if (codes[ch] == 0) {
                    output[outPtr++] = ch;
                    continue;
                }
                if (ch == '\\') {
                    output[outPtr++] = '_';
                } else if (ch == '\n') {
                    output[outPtr++] = '_';
                }
            }
        }
        long time = System.currentTimeMillis() - start;
        return time;
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        new ManualCharAccessTest().test();
    }
}
