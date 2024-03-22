package com.fasterxml.jackson.core.io;

import java.io.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UTF8WriterTest
        extends com.fasterxml.jackson.core.JUnit5TestBase
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

    private IOContext _ioContext() {
        return testIOContext();
    }
}
