package tools.jackson.core.jmh;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

/**
 * Throughput of {@code writeString} / {@code writeName} for the UTF-8 and
 * {@code Writer}-backed JSON generators, with {@code String} and {@code char[]} input
 * [core#1680].
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class StringWriteBench
{
    static final int COUNT = 200;

    /**
     * ascii: no escaping needed; escaped: one '"' near the end of each value;
     * latin1: one 'é' (U+00E9) near the end of each value, so the
     * value is still a Latin-1 {@code String} but leaves the ASCII fast path.
     */
    @Param({"ascii", "escaped", "latin1"})
    public String content;

    @Param({"8", "24", "100", "1000"})
    public int length;

    /**
     * If true, calls {@code charAt} once on a non-Latin-1 (UTF-16) {@code String}
     * before measuring, so {@code String} code paths see both coders in their
     * profile, as they would in most real applications. Without this, JIT can
     * specialize {@code charAt} loops for Latin-1 only, which overstates per-char
     * loops relative to bulk copies. Run with {@code -p polluteProfile=false} to compare.
     */
    @Param({"true"})
    public boolean polluteProfile;

    final JsonFactory f = new JsonFactory();
    String[] values;
    char[][] charValues;

    static final OutputStream NULL_OUT = new OutputStream() {
        @Override public void write(int b) { }
        @Override public void write(byte[] b, int off, int len) { }
    };
    static final Writer NULL_W = new Writer() {
        @Override public void write(char[] c, int off, int len) { }
        @Override public void write(String s, int off, int len) { }
        @Override public void flush() { }
        @Override public void close() { }
    };

    @Setup
    public void setup(Blackhole bh) {
        if (polluteProfile) {
            String utf16 = new String(new char[] { (char) 0x100, 'x' });
            bh.consume(utf16.charAt(0));
        }
        Random r = new Random(123);
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,:;-_/";
        values = new String[COUNT];
        charValues = new char[COUNT][];
        for (int i = 0; i < COUNT; ++i) {
            char[] c = new char[length];
            for (int j = 0; j < length; ++j) {
                c[j] = alphabet.charAt(r.nextInt(alphabet.length()));
            }
            switch (content) {
            case "escaped":
                c[length - 2] = '"';
                break;
            case "latin1":
                c[length - 2] = (char) 0xE9;
                break;
            }
            values[i] = new String(c);
            charValues[i] = c;
        }
    }

    @Benchmark
    public void utf8String() {
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), NULL_OUT)) {
            g.writeStartArray();
            for (String s : values) {
                g.writeString(s);
            }
            g.writeEndArray();
        }
    }

    @Benchmark
    public void utf8Chars() {
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), NULL_OUT)) {
            g.writeStartArray();
            for (char[] c : charValues) {
                g.writeString(c, 0, c.length);
            }
            g.writeEndArray();
        }
    }

    @Benchmark
    public void utf8Name() {
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), NULL_OUT)) {
            g.writeStartObject();
            for (String s : values) {
                g.writeName(s);
                g.writeNumber(1);
            }
            g.writeEndObject();
        }
    }

    @Benchmark
    public void writerString() {
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), NULL_W)) {
            g.writeStartArray();
            for (String s : values) {
                g.writeString(s);
            }
            g.writeEndArray();
        }
    }

    @Benchmark
    public void writerChars() {
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), NULL_W)) {
            g.writeStartArray();
            for (char[] c : charValues) {
                g.writeString(c, 0, c.length);
            }
            g.writeEndArray();
        }
    }
}
