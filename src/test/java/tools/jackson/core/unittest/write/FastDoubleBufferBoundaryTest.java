package tools.jackson.core.unittest.write;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the {@link StreamWriteFeature#USE_FAST_DOUBLE_WRITER} direct-to-buffer
 * path reserves enough room for the longest possible output, at every alignment of the
 * value against the end of the output buffer.
 */
public class FastDoubleBufferBoundaryTest extends JacksonCoreTestBase
{
    // Longest double output: 17 significant digits, sign, 3-digit negative exponent
    private final static double LONGEST_DOUBLE = -2.2250738585072014E-308;
    // Longest float output: 9 significant digits, sign, 2-digit negative exponent
    private final static float LONGEST_FLOAT = -1.01897876E-22f;

    private final JsonFactory FAST_FACTORY = JsonFactory.builder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();

    private final JsonFactory FAST_AS_STRINGS_FACTORY = JsonFactory.builder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
            .build();

    @Test
    void doubleAtEveryBufferOffsetBytes() throws Exception {
        _verifyAllOffsets(FAST_FACTORY, true, LONGEST_DOUBLE, String.valueOf(LONGEST_DOUBLE));
    }

    @Test
    void doubleAtEveryBufferOffsetChars() throws Exception {
        _verifyAllOffsets(FAST_FACTORY, false, LONGEST_DOUBLE, String.valueOf(LONGEST_DOUBLE));
    }

    @Test
    void floatAtEveryBufferOffsetBytes() throws Exception {
        _verifyAllOffsets(FAST_FACTORY, true, LONGEST_FLOAT, String.valueOf(LONGEST_FLOAT));
    }

    @Test
    void floatAtEveryBufferOffsetChars() throws Exception {
        _verifyAllOffsets(FAST_FACTORY, false, LONGEST_FLOAT, String.valueOf(LONGEST_FLOAT));
    }

    @Test
    void numbersAsStringsAtEveryBufferOffsetBytes() throws Exception {
        _verifyAllOffsets(FAST_AS_STRINGS_FACTORY, true, LONGEST_DOUBLE,
                q(String.valueOf(LONGEST_DOUBLE)));
        _verifyAllOffsets(FAST_AS_STRINGS_FACTORY, true, LONGEST_FLOAT,
                q(String.valueOf(LONGEST_FLOAT)));
    }

    @Test
    void numbersAsStringsAtEveryBufferOffsetChars() throws Exception {
        _verifyAllOffsets(FAST_AS_STRINGS_FACTORY, false, LONGEST_DOUBLE,
                q(String.valueOf(LONGEST_DOUBLE)));
        _verifyAllOffsets(FAST_AS_STRINGS_FACTORY, false, LONGEST_FLOAT,
                q(String.valueOf(LONGEST_FLOAT)));
    }

    // Writes padding of increasing length before the number so that the value starts at
    // every possible distance from the end of the generator's output buffer.
    private void _verifyAllOffsets(JsonFactory f, boolean bytes, Number value, String expected)
        throws Exception
    {
        // BufferRecycler sizes: byte write-encoding buffer 8000, char concat buffer 4000
        final int bufferSize = bytes ? 8000 : 4000;
        final int padStart = bufferSize - 100;

        for (int padLen = padStart; padLen < bufferSize; ++padLen) {
            String pad = _pad(padLen);
            String json = _write(f, bytes, pad, value);
            assertEquals("[\""+pad+"\","+expected+"]", json,
                    "padding length "+padLen+" (bytes="+bytes+")");
        }
    }

    private String _write(JsonFactory f, boolean bytes, String pad, Number value)
        throws Exception
    {
        if (bytes) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), out)) {
                _writeDoc(g, pad, value);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
        StringWriter out = new StringWriter();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), out)) {
            _writeDoc(g, pad, value);
        }
        return out.toString();
    }

    private void _writeDoc(JsonGenerator g, String pad, Number value) {
        g.writeStartArray();
        g.writeString(pad);
        if (value instanceof Double) {
            g.writeNumber(value.doubleValue());
        } else {
            g.writeNumber(value.floatValue());
        }
        g.writeEndArray();
    }

    private String _pad(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append('x');
        }
        return sb.toString();
    }
}
