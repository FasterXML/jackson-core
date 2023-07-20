package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapperForByteArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;

import static com.fasterxml.jackson.core.async.AsyncTestBase.asyncForBytes;
import static org.junit.Assert.assertArrayEquals;

class ReadConstrainedTextBufferTest {
    private static final int SEGMENT_SIZE = TextBuffer.MIN_SEGMENT_LEN;

    @Test
    public void appendCharArray() throws Exception {
        TextBuffer constrained = makeConstrainedBuffer(SEGMENT_SIZE, false);
        char[] chars = new char[SEGMENT_SIZE];
        Arrays.fill(chars, 'A');
        constrained.append(chars, 0, SEGMENT_SIZE);
        Assertions.assertThrows(IOException.class, () -> constrained.append(chars, 0, SEGMENT_SIZE));
    }

    @Test
    public void appendString() throws Exception {
        TextBuffer constrained = makeConstrainedBuffer(SEGMENT_SIZE, false);
        char[] chars = new char[SEGMENT_SIZE];
        Arrays.fill(chars, 'A');
        constrained.append(new String(chars), 0, SEGMENT_SIZE);
        Assertions.assertThrows(IOException.class, () -> constrained.append(new String(chars), 0, SEGMENT_SIZE));
    }

    @Test
    public void appendSingle() throws Exception {
        TextBuffer constrained = makeConstrainedBuffer(SEGMENT_SIZE, false);
        char[] chars = new char[SEGMENT_SIZE];
        Arrays.fill(chars, 'A');
        constrained.append(chars, 0, SEGMENT_SIZE);
        Assertions.assertThrows(IOException.class, () -> constrained.append('x'));
    }

    private static TextBuffer makeConstrainedBuffer(int maxStringLen, boolean pooledBufferRecycler) {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxStringLength(maxStringLen)
                .build();
        IOContext ioContext = new IOContext(
                constraints,
                StreamWriteConstraints.defaults(),
                pooledBufferRecycler ? BufferRecyclers.getBufferRecycler() : new BufferRecycler(),
                ContentReference.rawReference("N/A"), true);
        return ioContext.constructReadConstrainedTextBuffer();
    }

    @Test
    public void appendStringUsingExistingBufferRecycler() throws Exception {
        int size = 6000;
        AsyncReaderWrapper reader = createReader(size);

        // This statement will cause the release of a large buffer in the BufferRecycler.CHAR_TEXT_BUFFER bucket
        reader.nextToken();

        reader.close();

        // Since it is running on the same thread the ThreadLocal based pooling will make this to reuse the same BufferRecycler
        TextBuffer constrained = makeConstrainedBuffer(SEGMENT_SIZE, true);

        char[] chars = new char[SEGMENT_SIZE];
        Arrays.fill(chars, 'A');
        constrained.append(new String(chars), 0, SEGMENT_SIZE);

        // This should throw an exception but doesn't since the constraint is calculated on the size of the formerly released buffer
        Assertions.assertThrows(IOException.class, () -> constrained.append(new String(chars), 0, SEGMENT_SIZE));
    }

    private AsyncReaderWrapper createReader(int size) throws IOException {
        byte[] binary = _generateData(size);
        ByteArrayOutputStream bo = new ByteArrayOutputStream(size +10);

        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator g = jsonFactory.createGenerator(bo);
        g.writeBinary(binary);
        g.close();
        byte[] byteArray = bo.toByteArray();

        return asyncForBytes(jsonFactory, Integer.MAX_VALUE, byteArray, 1);
    }

    private byte[] _generateData(int size) {
        byte[] result = new byte[size];
        for (int i = 0; i < size; ++i) {
            result[i] = (byte) (i % 255);
        }
        return result;
    }
}