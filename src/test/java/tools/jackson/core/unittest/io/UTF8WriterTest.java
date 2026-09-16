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

    // Surrogate pair split across two write() calls: first half must be
    // carried over and combined with the second half from the next call
    @Test
    void surrogatesSplitAcrossWrites() throws Exception
    {
        final String text = "a\uD83D\uDE00b\uD800\uDC00c\uDBFF\uDFFF";
        final byte[] exp = text.getBytes("UTF-8");
        final char[] chars = text.toCharArray();

        for (int split = 1; split < chars.length; ++split) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            UTF8Writer w = new UTF8Writer(_ioContext(), out);
            w.write(chars, 0, split);
            w.write(chars, split, chars.length - split);
            w.close();
            assertArrayEquals(exp, out.toByteArray(), "char[], split="+split);

            out = new ByteArrayOutputStream();
            w = new UTF8Writer(_ioContext(), out);
            w.write(text, 0, split);
            w.write(text, split, chars.length - split);
            w.close();
            assertArrayEquals(exp, out.toByteArray(), "String, split="+split);

            out = new ByteArrayOutputStream();
            w = new UTF8Writer(_ioContext(), out);
            w.write(chars, 0, split);
            for (int i = split; i < chars.length; ++i) {
                w.write(chars[i]);
            }
            w.close();
            assertArrayEquals(exp, out.toByteArray(), "mixed, split="+split);
        }
    }

    // Surrogate pair landing on the internal output buffer flush point
    @Test
    void surrogatesAtOutputBufferEdge() throws Exception
    {
        final String pairs = "\uD83D\uDE00\uD800\uDC00";
        // write buffer is 8000 bytes; flush happens when fewer than 4 bytes left
        for (int prefix = 7990; prefix <= 8010; ++prefix) {
            StringBuilder sb = new StringBuilder(prefix + 4);
            for (int i = 0; i < prefix; ++i) {
                sb.append('x');
            }
            sb.append(pairs);
            String text = sb.toString();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            UTF8Writer w = new UTF8Writer(_ioContext(), out);
            w.write(text);
            w.close();
            assertArrayEquals(text.getBytes("UTF-8"), out.toByteArray(), "prefix="+prefix);

            out = new ByteArrayOutputStream();
            w = new UTF8Writer(_ioContext(), out);
            w.write(text.toCharArray());
            w.close();
            assertArrayEquals(text.getBytes("UTF-8"), out.toByteArray(), "char[], prefix="+prefix);
        }
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

    private IOContext _ioContext() {
        return testIOContext();
    }
}
