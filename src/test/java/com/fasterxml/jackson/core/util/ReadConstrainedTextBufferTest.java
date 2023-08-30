package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

class ReadConstrainedTextBufferTest {
    private static final int SEGMENT_SIZE = TextBuffer.MIN_SEGMENT_LEN;

    @Test
    public void appendCharArray() throws Exception {
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] chars = new char[SEGMENT_SIZE];
            Arrays.fill(chars, 'A');
            constrained.append(chars, 0, SEGMENT_SIZE);
            Assertions.assertThrows(IOException.class, () -> {
                constrained.append(chars, 0, SEGMENT_SIZE);
                constrained.contentsAsString();
            });
        }
    }

    @Test
    public void appendString() throws Exception {
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] chars = new char[SEGMENT_SIZE];
            Arrays.fill(chars, 'A');
            constrained.append(new String(chars), 0, SEGMENT_SIZE);
            Assertions.assertThrows(IOException.class, () -> {
                constrained.append(new String(chars), 0, SEGMENT_SIZE);
                constrained.contentsAsString();
            });
        }
    }

    @Test
    public void appendSingle() throws Exception {
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] chars = new char[SEGMENT_SIZE];
            Arrays.fill(chars, 'A');
            constrained.append(chars, 0, SEGMENT_SIZE);
            Assertions.assertThrows(IOException.class, () -> {
                constrained.append('x');
                constrained.contentsAsString();
            });
        }
    }

    private static IOContext makeConstrainedContext(int maxStringLen) {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxStringLength(maxStringLen)
                .build();
        return new IOContext(
                constraints,
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(),
                ContentReference.rawReference("N/A"), true);
    }
}