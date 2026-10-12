package perf;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.Named;

/**
 * Manually run micro-benchmark comparing the two-call
 * {@code nextNameMatch()} + {@code nextToken()} sequence against the fused
 * {@code nextNameMatchAndToken()}, over both byte- and char-backed parsers.
 * Approximates what generated per-bean deserializers do: dispatch on the
 * match index, then read the value.
 *
 * @since 3.3
 */
public class ManualNameMatchPerfTest
{
    private final static String[] NAMES = new String[] {
        "id", "name", "type", "size", "enabled", "score", "label", "flag"
    };

    private final JsonFactory _factory = new JsonFactory();
    private final byte[] _docBytes;
    private final String _docString;

    private ManualNameMatchPerfTest(int entries) {
        StringBuilder sb = new StringBuilder(entries * 100);
        sb.append('[');
        for (int i = 0; i < entries; ++i) {
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(i)
                .append(",\"name\":\"value").append(i & 0xFF).append('"')
                .append(",\"type\":\"t").append(i & 7).append('"')
                .append(",\"size\":").append(i * 3)
                .append(",\"enabled\":").append((i & 1) == 0)
                .append(",\"score\":").append(i % 97)
                .append(",\"label\":\"L").append(i & 15).append('"')
                .append(",\"flag\":").append((i & 3) == 0)
                .append('}');
        }
        sb.append(']');
        _docString = sb.toString();
        _docBytes = _docString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private PropertyNameMatcher matcher() {
        List<Named> names = new ArrayList<>();
        for (String n : NAMES) {
            names.add(Named.fromString(n));
        }
        return _factory.constructNameMatcher(names, true);
    }

    private JsonParser parser(boolean bytes) {
        return bytes ? _factory.createParser(ObjectReadContext.empty(), _docBytes)
                : _factory.createParser(ObjectReadContext.empty(), _docString);
    }

    // Two-call sequence: nextNameMatch() then nextToken()
    private long readTwoCall(boolean bytes, PropertyNameMatcher m) {
        long sum = 0;
        try (JsonParser p = parser(bytes)) {
            p.nextToken();
            while (p.nextToken() == JsonToken.START_OBJECT) {
                while (true) {
                    int ix = p.nextNameMatch(m);
                    if (ix < 0) {
                        break;
                    }
                    p.nextToken();
                    sum += _value(p, ix);
                }
            }
        }
        return sum;
    }

    // Fused: nextNameMatchAndToken()
    private long readFused(boolean bytes, PropertyNameMatcher m) {
        long sum = 0;
        try (JsonParser p = parser(bytes)) {
            p.nextToken();
            while (p.nextToken() == JsonToken.START_OBJECT) {
                while (true) {
                    int ix = p.nextNameMatchAndToken(m);
                    if (ix < 0) {
                        break;
                    }
                    sum += _value(p, ix);
                }
            }
        }
        return sum;
    }

    // Stand-in for what a generated deserializer does with the match index
    private static long _value(JsonParser p, int ix) {
        switch (ix) {
        case 0: case 3: case 5:
            return p.getIntValue();
        case 1: case 2: case 6:
            return p.getString().length();
        default:
            return p.getBooleanValue() ? 1 : 0;
        }
    }

    private void run(int rounds, int repsPerRound) throws Exception
    {
        final PropertyNameMatcher m = matcher();
        System.out.printf("Document: %d bytes, %d properties/round%n",
                _docBytes.length, repsPerRound);

        for (boolean bytes : new boolean[] { true, false }) {
            final String desc = bytes ? "UTF8StreamJsonParser (byte[])"
                    : "ReaderBasedJsonParser (String)";
            // warmup, both paths, to get C2 to steady state
            long h = 0;
            for (int i = 0; i < 20; ++i) {
                h += readTwoCall(bytes, m) + readFused(bytes, m);
            }
            long bestTwo = Long.MAX_VALUE, bestFused = Long.MAX_VALUE;
            for (int i = 0; i < rounds; ++i) {
                long start = System.nanoTime();
                for (int j = 0; j < repsPerRound; ++j) { h += readTwoCall(bytes, m); }
                bestTwo = Math.min(bestTwo, System.nanoTime() - start);

                start = System.nanoTime();
                for (int j = 0; j < repsPerRound; ++j) { h += readFused(bytes, m); }
                bestFused = Math.min(bestFused, System.nanoTime() - start);
            }
            System.out.printf("%n%s [hash 0x%x]%n", desc, h);
            System.out.printf("  two-call : %6.2f msecs%n", bestTwo / 1000000.0);
            System.out.printf("  fused    : %6.2f msecs (%+.1f%%)%n",
                    bestFused / 1000000.0,
                    100.0 * (bestFused - bestTwo) / bestTwo);
        }
    }

    public static void main(String[] args) throws Exception {
        new ManualNameMatchPerfTest(2000).run(12, 20);
    }
}
