package tools.jackson.core.unittest.io;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.UTF8Writer;

import static org.junit.jupiter.api.Assertions.*;

class UTF8WriterTest
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    @Test
    void simple() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(_ioContext(), out);

        String str = "AB\u00A0\u1AE9\uFFFC";
        char[] ch = str.toCharArray();

        // Let's write 3 times, using different methods
        w.write(str);

        w.append(ch[0]);
        w.write(ch[1]);
        w.write(ch, 2, 3);
        w.flush();

        w.write(str, 0, str.length());
        w.close();

        // and thus should have 3 times contents
        byte[] data = out.toByteArray();
        assertEquals(3*10, data.length);
        String act = utf8String(out);
        assertEquals(15, act.length());

        assertEquals(3 * str.length(), act.length());
        assertEquals(str+str+str, act);
    }

    @Test
    void simpleAscii() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(_ioContext(), out);

        String str = "abcdefghijklmnopqrst\u00A0";
        char[] ch = str.toCharArray();

        w.write(ch, 0, ch.length);
        w.flush(); // trigger different code path for close
        w.close();

        byte[] data = out.toByteArray();
        // one 2-byte encoded char
        assertEquals(ch.length+1, data.length);
        String act = utf8String(out);
        assertEquals(str, act);
    }

    @Test
    void flushAfterClose() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(_ioContext(), out);

        w.write('X');
        char[] ch = { 'Y' };
        w.write(ch);

        w.close();
        assertEquals(2, out.size());

        // and this ought to be fine...
        w.flush();
        // as well as some more...
        w.close();
        w.flush();
    }

    @Test
    void surrogatesOk() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(_ioContext(), out);

        // First, valid case, char by char
        w.write(0xD83D);
        w.write(0xDE03);
        w.close();
        assertEquals(4, out.size());
        final byte[] EXP_SURROGATES = new byte[] { (byte) 0xF0, (byte) 0x9F,
               (byte) 0x98, (byte) 0x83 };
        assertArrayEquals(EXP_SURROGATES, out.toByteArray());

        // and then as String
        out = new ByteArrayOutputStream();
        w = new UTF8Writer(_ioContext(), out);
        w.write("\uD83D\uDE03");
        w.close();
        assertEquals(4, out.size());
        assertArrayEquals(EXP_SURROGATES, out.toByteArray());
    }

    @SuppressWarnings("resource")
    @Test
    void surrogatesFail() throws Exception
    {
        ByteArrayOutputStream out;

        out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(_ioContext(), out)) {
            w.write(0xDE03);
            fail("should not pass");
        } catch (IOException e) {
            verifyException(e, "Unmatched second part");
        }

        out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(_ioContext(), out)) {
            w.write(0xD83D);
            w.write('a');
            fail("should not pass");
        } catch (IOException e) {
            verifyException(e, "Broken surrogate pair");
        }

        out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(_ioContext(), out)) {
            w.write("\uDE03");
            fail("should not pass");
        } catch (IOException e) {
            verifyException(e, "Unmatched second part");
        }

        out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(_ioContext(), out)) {
            w.write("\uD83Da");
            fail("should not pass");
        } catch (IOException e) {
            verifyException(e, "Broken surrogate pair");
        }
    }

    // For [core#1218]
    // @since 2.17
    @Test
    void surrogateConversion()
    {
        for (int first = UTF8Writer.SURR1_FIRST; first <= UTF8Writer.SURR1_LAST; first++) {
            for (int second = UTF8Writer.SURR2_FIRST; second <= UTF8Writer.SURR2_LAST; second++) {
                int expected = 0x10000 + ((first - UTF8Writer.SURR1_FIRST) << 10) + (second - UTF8Writer.SURR2_FIRST);
                int actual = (first << 10) + second + UTF8Writer.SURROGATE_BASE;
                if (expected != actual) {
                    fail("Mismatch on: "+Integer.toHexString(first) + " " + Integer.toHexString(second)
                        +"; expected: "+expected+", actual: "+actual);
                }
            }
        }
    }

    // Failure to write out pending content must not prevent releasing of the
    // encoding buffer and closing of the underlying stream
    @Test
    void releasesResourcesOnFailedClose() throws Exception
    {
        FailingOutputStream out = new FailingOutputStream();
        UTF8Writer w = new UTF8Writer(_ioContext(), out);
        w.write("abc");
        // nothing pushed to stream yet, so failure occurs during close():
        assertEquals(0, out.writeCount);
        try {
            w.close();
            fail("should not pass");
        } catch (IOException e) {
            verifyException(e, "write() failing");
        }
        assertTrue(out.closed, "Underlying stream should have been closed");

        // and second close() is a no-op, not a retry of the failed write
        w.close();
        assertEquals(1, out.writeCount);
    }

    // Same for unchecked failures
    @Test
    void releasesResourcesOnFailedCloseUnchecked() throws Exception
    {
        FailingOutputStream out = new FailingOutputStream(true);
        UTF8Writer w = new UTF8Writer(_ioContext(), out);
        w.write("abc");
        try {
            w.close();
            fail("should not pass");
        } catch (UncheckedIOException e) {
            verifyException(e, "write() failing");
        }
        assertTrue(out.closed, "Underlying stream should have been closed");
        w.close();
        assertEquals(1, out.writeCount);
    }

    static class FailingOutputStream extends OutputStream {
        private final boolean _unchecked;
        public int writeCount;
        public boolean closed;

        public FailingOutputStream() { this(false); }

        public FailingOutputStream(boolean unchecked) { _unchecked = unchecked; }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            ++writeCount;
            IOException e = new IOException("write() failing");
            if (_unchecked) {
                throw new UncheckedIOException(e);
            }
            throw e;
        }

        @Override
        public void close() { closed = true; }
    }

    private IOContext _ioContext() {
        return testIOContext();
    }
}
