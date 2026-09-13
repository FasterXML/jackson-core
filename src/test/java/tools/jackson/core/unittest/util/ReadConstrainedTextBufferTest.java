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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void appendCharArrayToSharedFailsBeforeAllocation() throws Exception {
        TrackingBufferRecycler recycler = new TrackingBufferRecycler();
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE, recycler)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] shared = new char[SEGMENT_SIZE];
            char[] extra = new char[1];
            Arrays.fill(shared, 'A');
            Arrays.fill(extra, 'B');

            constrained.resetWithShared(shared, 0, shared.length);

            assertThrows(StreamConstraintsException.class,
                    () -> constrained.append(extra, 0, extra.length));
            assertEquals(0, recycler.charTextBufferAllocations);
        }
    }

    @Test
    void appendStringToSharedFailsBeforeAllocation() throws Exception {
        TrackingBufferRecycler recycler = new TrackingBufferRecycler();
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE, recycler)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] shared = new char[SEGMENT_SIZE];
            Arrays.fill(shared, 'A');

            constrained.resetWithShared(shared, 0, shared.length);

            assertThrows(StreamConstraintsException.class,
                    () -> constrained.append("B", 0, 1));
            assertEquals(0, recycler.charTextBufferAllocations);
        }
    }

    @Test
    void appendSingleCharToSharedFailsBeforeAllocation() throws Exception {
        TrackingBufferRecycler recycler = new TrackingBufferRecycler();
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE, recycler)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] shared = new char[SEGMENT_SIZE];
            Arrays.fill(shared, 'A');

            constrained.resetWithShared(shared, 0, shared.length);

            assertThrows(StreamConstraintsException.class,
                    () -> constrained.append('B'));
            assertEquals(0, recycler.charTextBufferAllocations);
        }
    }

    @Test
    void appendSingleCharToSharedAtLimitSucceeds() throws Exception {
        try (IOContext ioContext = makeConstrainedContext(SEGMENT_SIZE)) {
            TextBuffer constrained = ioContext.constructReadConstrainedTextBuffer();
            char[] shared = new char[SEGMENT_SIZE - 1];
            Arrays.fill(shared, 'A');

            constrained.resetWithShared(shared, 0, shared.length);
            constrained.append('B');

            assertEquals(SEGMENT_SIZE, constrained.size());
            assertEquals(SEGMENT_SIZE, constrained.contentsAsString().length());
        }
    }

    private static IOContext makeConstrainedContext(int maxStringLen) {
        return makeConstrainedContext(maxStringLen, new BufferRecycler());
    }

    private static IOContext makeConstrainedContext(int maxStringLen, BufferRecycler recycler) {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxStringLength(maxStringLen)
                .build();
        return new IOContext(
                constraints,
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                recycler,
                ContentReference.rawReference("N/A"), true,
                JsonEncoding.UTF8);
    }

    private static class TrackingBufferRecycler extends BufferRecycler {
        int charTextBufferAllocations;

        @Override
        protected char[] calloc(int size) {
            ++charTextBufferAllocations;
            return super.calloc(size);
        }
    }
}
