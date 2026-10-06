package tools.jackson.core.unittest.io;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.GuardedOutputStream;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.*;

// [core#1738]
class GuardedOutputStreamTest
    extends JacksonCoreTestBase
{
    static class TrackingStream extends ByteArrayOutputStream {
        int flushCount, closeCount, singleByteWrites;

        @Override
        public void write(int b) {
            ++singleByteWrites;
            super.write(b);
        }

        @Override
        public void flush() { ++flushCount; }

        @Override
        public void close() { ++closeCount; }
    }

    @Test
    void writesPassedThrough() throws Exception
    {
        TrackingStream target = new TrackingStream();
        try (OutputStream out = new GuardedOutputStream(target)) {
            out.write('a');
            out.write("bcd".getBytes(StandardCharsets.UTF_8));
            out.write("xefx".getBytes(StandardCharsets.UTF_8), 1, 2);
        }
        assertEquals("abcdef", target.toString(StandardCharsets.UTF_8));
        // only the explicit single-byte write; arrays must not be split
        assertEquals(1, target.singleByteWrites);
    }

    @Test
    void flushAndCloseNotPassed() throws Exception
    {
        TrackingStream target = new TrackingStream();
        GuardedOutputStream out = new GuardedOutputStream(target);
        out.write('a');
        out.flush();
        out.close();
        assertEquals(0, target.flushCount);
        assertEquals(0, target.closeCount);
    }

    @Test
    void writerOverGuardedStream() throws Exception
    {
        TrackingStream target = new TrackingStream();
        Writer w = new OutputStreamWriter(new GuardedOutputStream(target), StandardCharsets.UTF_8);
        w.write("abc");
        w.flush();
        assertEquals("abc", target.toString(StandardCharsets.UTF_8));
        w.write("def");
        w.close();
        assertEquals("abcdef", target.toString(StandardCharsets.UTF_8));
        assertEquals(0, target.flushCount);
        assertEquals(0, target.closeCount);
    }
}
