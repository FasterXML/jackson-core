package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class ReadConstrainedTextBufferTest {
    @Test
    public void appendCharArray() {
        TextBuffer constrained = makeConstrainedBuffer(100);
        char[] chars = new char[100];
        Arrays.fill(chars, 'A');
        constrained.append(chars, 0, 100);
        Assertions.assertThrows(IllegalStateException.class, () -> constrained.append(chars, 0, 100));
    }

    @Test
    public void appendString() {
        TextBuffer constrained = makeConstrainedBuffer(100);
        char[] chars = new char[100];
        Arrays.fill(chars, 'A');
        constrained.append(new String(chars), 0, 100);
        Assertions.assertThrows(IllegalStateException.class, () -> constrained.append(new String(chars), 0, 100));
    }

    @Test
    public void appendSingle() {
        TextBuffer constrained = makeConstrainedBuffer(100);
        char[] chars = new char[100];
        Arrays.fill(chars, 'A');
        constrained.append(chars, 0, 100);
        Assertions.assertThrows(IllegalStateException.class, () -> constrained.append('x'));
    }

    private static TextBuffer makeConstrainedBuffer(int maxStringLen) {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxStringLength(maxStringLen)
                .build();
        IOContext ioContext = new IOContext(
                constraints,
                new BufferRecycler(),
                ContentReference.rawReference("N/A"), true);
        TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
        return constrained;
    }
}