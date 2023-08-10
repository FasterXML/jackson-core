package com.fasterxml.jackson.core.json;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

public class CharBufferReaderTest extends com.fasterxml.jackson.core.BaseTest {

    public void testSingleCharRead() throws IOException {
        CharBuffer buffer = CharBuffer.wrap("aB\u00A0\u1AE9\uFFFC");
        Reader charBufferReader = new CharBufferReader(buffer);
        try (Reader reader = charBufferReader) {
            assertEquals('a', reader.read());
            assertEquals('B', reader.read());
            assertEquals('\u00A0', reader.read());
            assertEquals('\u1AE9', reader.read());
            assertEquals('￼', reader.read());
            assertEquals(-1, reader.read());
        }
        assertEquals(-1, charBufferReader.read());
    }

    public void testBulkRead() throws IOException {
        CharBuffer buffer = CharBuffer.wrap("abcdefghijklmnopqrst\u00A0");
        char[] chars = new char[12];
        Reader charBufferReader = new CharBufferReader(buffer);
        try (Reader reader = charBufferReader) {
            assertEquals(12, reader.read(chars));
            assertArrayEquals("abcdefghijkl".toCharArray(), chars);
            assertEquals(9, reader.read(chars));
            assertArrayEquals("mnopqrst\u00A0".toCharArray(), Arrays.copyOf(chars, 9));
            assertEquals(-1, reader.read(chars));
        }
        assertEquals(-1, charBufferReader.read(chars));
    }

    public void testSkip() throws IOException {
        CharBuffer buffer = CharBuffer.wrap("abcdefghijklmnopqrst\u00A0");
        Reader charBufferReader = new CharBufferReader(buffer);
        char[] chars = new char[12];
        try (Reader reader = charBufferReader) {
            assertEquals(12, reader.read(chars));
            assertArrayEquals("abcdefghijkl".toCharArray(), chars);
            assertEquals(4, reader.skip(4));
            assertEquals(4, reader.read(chars, 3, 4));
            assertArrayEquals("qrst".toCharArray(), Arrays.copyOfRange(chars, 3, 7));
            assertEquals(1, reader.skip(Long.MAX_VALUE));
            assertEquals(0, reader.skip(Integer.MAX_VALUE));
            assertEquals(0, reader.skip(Long.MAX_VALUE));
            assertEquals(0, reader.skip(0));
            assertEquals(0, reader.skip(1));
        }
        assertEquals(0, charBufferReader.skip(1));
    }

    public void testInvalidSkip() throws IOException {
        try (Reader reader = new CharBufferReader(CharBuffer.wrap("test"))) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> reader.skip(-1));
            assertEquals("number of characters to skip cannot be negative", exception.getMessage());
        }
    }

    public void testReady() throws IOException {
        try (Reader reader = new CharBufferReader(CharBuffer.wrap("test"))) {
            assertEquals(true, reader.ready());
        }
    }

    public void testMarkReset() throws IOException {
        char[] chars = new char[8];
        try (Reader reader = new CharBufferReader(CharBuffer.wrap("test"))) {
            assertEquals(true, reader.markSupported());
            reader.mark(3);
            assertEquals(3, reader.read(chars, 0, 3));
            assertArrayEquals("tes".toCharArray(), Arrays.copyOf(chars, 3));
            reader.reset();
            reader.mark(Integer.MAX_VALUE);
            assertEquals(4, reader.read(chars));
            assertArrayEquals("test".toCharArray(), Arrays.copyOf(chars, 4));
            reader.reset();
            Arrays.fill(chars, '\0');
            assertEquals(4, reader.read(chars));
        }
    }

    public void testInvalidMark() throws IOException {
        try (Reader reader = new CharBufferReader(CharBuffer.wrap("test"))) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> reader.mark(-1));
            assertEquals("read ahead limit cannot be negative", exception.getMessage());
        }
    }

    public void testClose() throws IOException {
        char[] chars = new char[2];
        try (Reader reader = new CharBufferReader(CharBuffer.wrap("test"))) {
            assertEquals(2, reader.read(chars));
            assertArrayEquals("te".toCharArray(), chars);
            reader.close();
            Arrays.fill(chars, '\0');
            assertEquals(-1, reader.read(chars));
            assertArrayEquals("\0\0".toCharArray(), chars);
        }
    }
}
