package tools.jackson.core.util;

import java.util.Arrays;

import tools.jackson.core.JsonEncoding;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReadConstrainedTextBufferTest {
    private static final int SEGMENT_SIZE = TextBuffer.MIN_SEGMENT_LEN;

    @Test
    public void appendCharArray() throws Exception {
        TextBuffer constrained = makeConstrainedBuffer(SEGMENT_SIZE);
        char[] chars = new char[SEGMENT_SIZE];
        Arrays.fill(chars, 'A');
        constrained.append(chars, 0, SEGMENT_SIZE);
        Assertions.assertThrows(StreamConstraintsException.class, () -> constrained.append(chars, 0, SEGMENT_SIZE));
    }

    @Test
    public void appendString() throws Exception {
        TextBuffer constrained = makeConstrainedBuffer(SEGMENT_SIZE);
        char[] chars = new char[SEGMENT_SIZE];
        Arrays.fill(chars, 'A');
        constrained.append(new String(chars), 0, SEGMENT_SIZE);
        Assertions.assertThrows(StreamConstraintsException.class, () -> constrained.append(new String(chars), 0, SEGMENT_SIZE));
    }

    @Test
    public void appendSingle() throws Exception {
        TextBuffer constrained = makeConstrainedBuffer(SEGMENT_SIZE);
        char[] chars = new char[SEGMENT_SIZE];
        Arrays.fill(chars, 'A');
        constrained.append(chars, 0, SEGMENT_SIZE);
        Assertions.assertThrows(StreamConstraintsException.class, () -> constrained.append('x'));
    }

    private static TextBuffer makeConstrainedBuffer(int maxStringLen) {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxStringLength(maxStringLen)
                .build();
        IOContext ioContext = new IOContext(
                constraints,
                new BufferRecycler(),
                ContentReference.rawReference("N/A"), true, JsonEncoding.UTF8);
        return ioContext.constructReadConstrainedTextBuffer();
    }
}