package perf;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.CharTypes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Benchmarks the performance of writing UTF-8 encoded bytes, in particular the difference between using a 7-bit wide
 * lookup table for escapes, versus a full 8-bit wide table. The latter is beneficial when processing encoded UTF-8
 * bytes, as the byte itself can directly be used as table index instead of needing an additional branch.
 * <p>
 * This benchmark implements the escaping UTF-8 write loops using both 7-bit and 8-bit tables to show their respective
 * differences, as well as testing {@link JsonGenerator#writeUTF8String} for benchmarking the production implementation.
 *
 * @see <a href="https://github.com/FasterXML/jackson-core/pull/1349">Github PR</a>
 */
public class ManualUtf8WriteTest
{
    private String test(byte[] utf8) throws Exception
    {
        final byte[] OUTPUT = new byte[utf8.length * 2];
        ByteArrayOutputStream OUTPUT_STREAM = new ByteArrayOutputStream(utf8.length * 2);
        JsonGenerator generator = new JsonFactory().createGenerator(OUTPUT_STREAM);

        // Let's try to guestimate suitable size, N megs of output
        final int REPS = (int) ((double) (80 * 1000 * 1000) / (double) utf8.length);
        System.out.printf("%d bytes to scan, will do %d repetitions\n",
                utf8.length, REPS);

        int i = 0;
        int roundsDone = 0;
        final int TYPES = 3;
        final int WARMUP_ROUNDS = 5;
        final int ROUNDS = WARMUP_ROUNDS + 10;

        final long[] times = new long[TYPES];

        while (i < ROUNDS * TYPES) {
            int round = i++ % TYPES;

            String msg;

            long msecs;
            switch (round) {
            case 0:
                msg = "Write UTF-8 [7-bit escaping table]";
                msecs = writeUtf8_7BitEscapingTable(REPS, utf8, OUTPUT);
                break;
            case 1:
                msg = "Write UTF-8 [8-bit escaping table]";
                msecs = writeUtf8_8BitEscapingTable(REPS, utf8, OUTPUT);
                break;
            case 2:
                msg = "JsonGenerator.writeUTF8String     ";
                msecs = writeUtf8_JsonGenerator(REPS, utf8, OUTPUT_STREAM, generator);
                break;
            default:
                throw new Error();
            }
            // skip first 5 rounds to let results stabilize
            if (roundsDone >= WARMUP_ROUNDS) {
                times[round] += msecs;
            }

            System.out.printf("Test '%s' -> %3d msecs\n", msg, msecs);
            if (round == TYPES - 1) {
                ++roundsDone;
                if ((roundsDone % 3) == 0) {
                    System.out.println("[GC]");
                    Thread.sleep(100L);
                    System.gc();
                    Thread.sleep(100L);
                }
                System.out.println();
            }
        }
        double den = roundsDone - WARMUP_ROUNDS;

        return String.format("(7-bit, 8-bit, JsonGenerator): %5.1f / %5.1f / %5.1f msecs",
          times[0] / den, times[1] / den, times[2] / den);
    }

    private final long writeUtf8_7BitEscapingTable(int REPS, byte[] input, byte[] output)
    {
        long start = System.currentTimeMillis();
        int[] outputEscapes = CharTypes.get7BitOutputEscapes();

        while (--REPS >= 0) {
            int inOffset = 0;
            int outOffset = 0;
            int len = input.length;

            while (inOffset < len) {
                byte b = input[inOffset++];
                int ch = b;
                if (ch < 0 || outputEscapes[ch] == 0) {
                    output[outOffset++] = b;
                    continue;
                }
                int escape = outputEscapes[ch];
                if (escape > 0) {
                    output[outOffset++] = (byte) '\\';
                    output[outOffset++] = (byte) escape;
                } else {
                    throw new UnsupportedOperationException("ctrl character escapes are not covered in test");
                }
            }
        }
        long time = System.currentTimeMillis() - start;
        return time;
    }

    private final long writeUtf8_8BitEscapingTable(int REPS, byte[] input, byte[] output)
    {
        long start = System.currentTimeMillis();

        int[] outputEscapes = CharTypes.get7BitOutputEscapes();
        int[] extendedOutputEscapes = new int[0xFF];
        System.arraycopy(outputEscapes, 0, extendedOutputEscapes, 0, outputEscapes.length);

        while (--REPS >= 0) {
            int inOffset = 0;
            int outOffset = 0;
            int len = input.length;

            while (inOffset < len) {
                byte b = input[inOffset++];
                int ch = b & 0xFF;
                int escape = extendedOutputEscapes[ch];
                if (escape == 0) {
                    output[outOffset++] = b;
                    continue;
                }
                if (escape > 0) {
                    output[outOffset++] = (byte) '\\';
                    output[outOffset++] = (byte) escape;
                } else {
                    throw new UnsupportedOperationException("ctrl character escapes are not covered in test");
                }
            }
        }

        long time = System.currentTimeMillis() - start;
        return time;
    }

    private final long writeUtf8_JsonGenerator(int REPS, byte[] input, ByteArrayOutputStream output, JsonGenerator generator) throws IOException {
        long start = System.currentTimeMillis();

        while (--REPS >= 0) {
            output.reset();
            generator.writeUTF8String(input, 0, input.length);
            generator.flush();
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

        final int[] LENGTHS = new int[]{8, 16, 32, 256, 512, 1024, 1024 * 8};
        final String[] ESCAPE_VARIANTS = new String[] {"none", "start", "end"};
        final List<String> results = new ArrayList<String>();
        for (int length : LENGTHS){
            final byte[] buffer = new byte[length];

            for (int j = 0; j < ESCAPE_VARIANTS.length; j++) {
                Arrays.fill(buffer, (byte) 'a');

                if (j == 1) {
                    buffer[0] = '"';
                } else if (j == 2) {
                    buffer[buffer.length - 1] = '"';
                }

                String LABEL = String.format("Length %4d, %5s escape", length, ESCAPE_VARIANTS[j]);

                System.out.printf("Starting %s %n", LABEL);
                String result = new ManualUtf8WriteTest().test(buffer);
                System.out.printf("Finished %s %n", LABEL);
                System.out.println("================================================================================");

                results.add(String.format("%s: %s", LABEL, result));
            }
        }

        for (String result : results) {
            System.out.println(result);
        }
    }
}
