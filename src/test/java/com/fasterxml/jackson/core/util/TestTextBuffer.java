package com.fasterxml.jackson.core.util;

public class TestTextBuffer
    extends com.fasterxml.jackson.core.BaseTest
{
    /**
     * Trivially simple basic test to ensure all basic append
     * methods work
     */
    public void testSimple()
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

    public void testLonger()
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

    public void testLongAppend()
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
    public void testExpand()
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
    public void testEmpty() {
        TextBuffer tb = new TextBuffer(new BufferRecycler());
        tb.resetWithEmpty();

        assertTrue(tb.getTextBuffer().length == 0);
        tb.contentsAsString();
        assertTrue(tb.getTextBuffer().length == 0);
    }

    public void testResetWithAndSetCurrentAndReturn() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWith('l');
        textBuffer.setCurrentAndReturn(349);
    }

    public void testGetCurrentSegment() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.emptyAndGetCurrentSegment();
        // 26-Aug-2019, tatu: Value depends on "minimum segment size":
        textBuffer.setCurrentAndReturn(500);
        textBuffer.getCurrentSegment();

        assertEquals(500, textBuffer.size());
    }

    public void testAppendTakingTwoAndThreeInts() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.ensureNotShared();
        char[] charArray = textBuffer.getTextBuffer();
        textBuffer.append(charArray, 0, 200);
        textBuffer.append("5rmk0rx(C@aVYGN@Q", 2, 3);

        assertEquals(3, textBuffer.getCurrentSegmentSize());
    }

    public void testEnsureNotSharedAndResetWithString() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.resetWithString("");

        assertFalse(textBuffer.hasTextAsCharacters());

        textBuffer.ensureNotShared();

        assertEquals(0, textBuffer.getCurrentSegmentSize());
    }

    public void testGetTextBufferAndEmptyAndGetCurrentSegmentAndFinishCurrentSegment() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.emptyAndGetCurrentSegment();
        textBuffer.finishCurrentSegment();
        textBuffer.getTextBuffer();

        assertEquals(200, textBuffer.size());
    }

    public void testGetTextBufferAndAppendTakingCharAndContentsAsArray() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.append('(');
        textBuffer.contentsAsArray();
        textBuffer.getTextBuffer();

        assertEquals(1, textBuffer.getCurrentSegmentSize());
    }

    public void testGetTextBufferAndResetWithString() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.resetWithString("");

        assertFalse(textBuffer.hasTextAsCharacters());

        textBuffer.getTextBuffer();

        assertTrue(textBuffer.hasTextAsCharacters());
    }

    public void testResetWithString() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TextBuffer textBuffer = new TextBuffer(bufferRecycler);
        textBuffer.ensureNotShared();
        textBuffer.finishCurrentSegment();

        assertEquals(200, textBuffer.size());

        textBuffer.resetWithString("asdf");

        assertEquals(0, textBuffer.getTextOffset());
    }

    public void testGetCurrentSegmentSizeResetWith() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWith('.');
        textBuffer.resetWith('q');

        assertEquals(1, textBuffer.getCurrentSegmentSize());
    }

    public void testGetSizeFinishCurrentSegmentAndResetWith() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWith('.');
        textBuffer.finishCurrentSegment();
        textBuffer.resetWith('q');

        assertEquals(2, textBuffer.size());
    }

}
