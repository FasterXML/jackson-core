package tools.jackson.core.unittest.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.TextBuffer;

import static org.junit.jupiter.api.Assertions.*;

class TextBufferTest
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    /**
     * Trivially simple basic test to ensure all basic append
     * methods work
     */
    @Test
    void simple() throws Exception
    {
        TextBuffer tb = new TextBuffer(new BufferRecycler());
        tb.append('a');
        tb.append(new char[] { 'X', 'b' }, 1, 1);
        tb.append("c", 0, 1);
        // all fits within one buffer so it is efficient...
        assertTrue(tb.hasTextAsCharacters());

        assertEquals(3, tb.contentsAsArray().length);
        assertEquals("abc", tb.toString());

        assertNotNull(tb.expandCurrentSegment());
    }

    @Test
    void longer() throws Exception
    {
        TextBuffer tb = new TextBuffer(null);
        for (int i = 0; i < 2000; ++i) {
            tb.append("abc", 0, 3);
        }
        String str = tb.contentsAsString();
        assertEquals(6000, str.length());
        assertEquals(6000, tb.contentsAsArray().length);

        tb.resetWithShared(new char[] { 'a' }, 0, 1);
        assertEquals(1, tb.toString().length());
        assertTrue(tb.hasTextAsCharacters());
    }

    @Test
    void longAppend() throws Exception
    {
        final int len = TextBuffer.MAX_SEGMENT_LEN * 3 / 2;
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append('x');
        }
        final String STR = sb.toString();
        final String EXP = "a" + STR + "c";

        // ok: first test with String:
        TextBuffer tb = new TextBuffer(new BufferRecycler());
        tb.append('a');
        tb.append(STR, 0, len);
        tb.append('c');
        assertEquals(len+2, tb.size());
        assertEquals(EXP, tb.contentsAsString());

        // then char[]
        tb = new TextBuffer(new BufferRecycler());
        tb.append('a');
        tb.append(STR.toCharArray(), 0, len);
        tb.append('c');
        assertEquals(len+2, tb.size());
        assertEquals(EXP, tb.contentsAsString());
    }

    // [core#152]
    @Test
    void expand() throws Exception
    {
        TextBuffer tb = new TextBuffer(new BufferRecycler());
        char[] buf = tb.getCurrentSegment();

        while (buf.length < 500 * 1000) {
            char[] old = buf;
            buf = tb.expandCurrentSegment();
            if (old.length >= buf.length) {
                fail("Expected buffer of "+old.length+" to expand, did not, length now "+buf.length);
            }
        }
        tb.resetWithString("Foobar");
        assertEquals("Foobar", tb.contentsAsString());
    }

    // [core#182]
    @Test
    void empty() throws Exception {
        TextBuffer tb = new TextBuffer(new BufferRecycler());
        tb.resetWithEmpty();

        assertEquals(0, tb.getTextBuffer().length);
        tb.contentsAsString();
        assertEquals(0, tb.getTextBuffer().length);
    }

    @Test
    void resetWithAndSetCurrentAndReturn() throws Exception {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWith('l');
        textBuffer.setCurrentAndReturn(349);
    }

    @Test
    void getCurrentSegment() throws Exception {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.emptyAndGetCurrentSegment();
        // 26-Aug-2019, tatu: Value depends on "minimum segment size":
        textBuffer.setCurrentAndReturn(500);
        textBuffer.getCurrentSegment();

        assertEquals(500, textBuffer.size());
    }

    @Test
    void appendTakingTwoAndThreeInts() throws Exception {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.ensureNotShared();
        char[] charArray = textBuffer.getTextBuffer();
        textBuffer.append(charArray, 0, 200);
        textBuffer.append("5rmk0rx(C@aVYGN@Q", 2, 3);

        assertEquals(3, textBuffer.getCurrentSegmentSize());
    }

    @Test
    void ensureNotSharedAndResetWithString() throws Exception {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.resetWithString("");

        assertFalse(textBuffer.hasTextAsCharacters());

        textBuffer.ensureNotShared();

        assertEquals(0, textBuffer.getCurrentSegmentSize());
    }

    @Test
    void getTextBufferAndEmptyAndGetCurrentSegmentAndFinishCurrentSegment() throws Exception {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.emptyAndGetCurrentSegment();
        textBuffer.finishCurrentSegment();
        textBuffer.getTextBuffer();

        assertEquals(200, textBuffer.size());
    }

    @Test
    void getTextBufferAndAppendTakingCharAndContentsAsArray() throws Exception {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.append('(');
        textBuffer.contentsAsArray();
        textBuffer.getTextBuffer();

        assertEquals(1, textBuffer.getCurrentSegmentSize());
    }

    @Test
    void getTextBufferAndResetWithString() throws Exception {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.resetWithString("");

        assertFalse(textBuffer.hasTextAsCharacters());

        textBuffer.getTextBuffer();

        assertTrue(textBuffer.hasTextAsCharacters());
    }

    @Test
    void resetWithString() throws Exception {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.ensureNotShared();
        textBuffer.finishCurrentSegment();

        assertEquals(200, textBuffer.size());

        textBuffer.resetWithString("asdf");

        assertEquals(0, textBuffer.getTextOffset());
    }

    // 3.1
    @Test
    void resetWithAsciiBytes() throws Exception {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);

        assertEquals("abc", textBuffer.resetWithASCII(new byte[] { 'a', 'b', 'c' }, 0, 3));
        assertEquals(0, textBuffer.getTextOffset());
        assertEquals("abc", textBuffer.contentsAsString());
    }

    // 3.1
    @Test
    void resetWithUTF8Bytes() throws Exception {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);

        final String UTF8_STR = "\u00E9\u00E8\u00E0"; // "éèà"
        final byte[] BYTES = UTF8_STR.getBytes(StandardCharsets.UTF_8);
        assertEquals(6, BYTES.length);
        assertEquals(UTF8_STR, textBuffer.resetWithUTF8(BYTES, 0, BYTES.length));
        assertEquals(UTF8_STR, textBuffer.contentsAsString());
    }

    @Test
    void getCurrentSegmentSizeResetWith() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWith('.');
        textBuffer.resetWith('q');

        assertEquals(1, textBuffer.getCurrentSegmentSize());
    }

    @Test
    void getSizeFinishCurrentSegmentAndResetWith() throws Exception {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWith('.');
        textBuffer.finishCurrentSegment();
        textBuffer.resetWith('q');

        assertEquals(2, textBuffer.size());
    }

    public void testContentsAsFloat() throws IOException {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithString("1.2345678");
        assertEquals(1.2345678f,  textBuffer.contentsAsFloat(false));
    }

    public void testContentsAsFloatFastParser() throws IOException {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithString("1.2345678");
        assertEquals(1.2345678f,  textBuffer.contentsAsFloat(true));
    }

    public void testContentsAsDouble() throws IOException {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithString("1.234567890123456789");
        assertEquals(1.234567890123456789d,  textBuffer.contentsAsDouble(false));
    }

    public void testContentsAsDoubleFastParser() throws IOException {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithString("1.234567890123456789");
        assertEquals(1.234567890123456789d,  textBuffer.contentsAsDouble(true));
    }
}
