package tools.jackson.core.unittest.util;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.TextBuffer;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadConstrainedTextBufferTest {
    private static final int SEGMENT_SIZE = TextBuffer.MIN_SEGMENT_LEN;

    @Test
    void appendCharArray() throws Exception {
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] chars = new char[SEGMENT_SIZE];
            Arrays.fill(chars, 'A');
            constrained.append(chars, 0, SEGMENT_SIZE);
            assertThrows(StreamConstraintsException.class, () -> {
                constrained.append(chars, 0, SEGMENT_SIZE);
                constrained.contentsAsString();
            });
        }
    }

    @Test
    void appendString() throws Exception {
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] chars = new char[SEGMENT_SIZE];
            Arrays.fill(chars, 'A');
            constrained.append(new String(chars), 0, SEGMENT_SIZE);
            assertThrows(StreamConstraintsException.class, () -> {
                constrained.append(new String(chars), 0, SEGMENT_SIZE);
                constrained.contentsAsString();
            });
        }
    }

    @Test
    void appendSingle() throws Exception {
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] chars = new char[SEGMENT_SIZE];
            Arrays.fill(chars, 'A');
            constrained.append(chars, 0, SEGMENT_SIZE);
            assertThrows(StreamConstraintsException.class, () -> {
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
                ContentReference.rawReference("N/A"), true,
                JsonEncoding.UTF8);
    }
}
