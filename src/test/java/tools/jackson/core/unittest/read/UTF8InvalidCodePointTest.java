package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UTF8InvalidCodePointTest
    extends JacksonCoreTestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    private final int[] STREAM_MODES = new int[] {
            MODE_INPUT_STREAM,
            MODE_INPUT_STREAM_THROTTLED
    };

    @Test
    void acceptUtf8CodePointAtUnicodeMaximum() throws Exception
    {
        byte[] json = _quotedUtf8Sequence((byte) 0xF4, (byte) 0x8F, (byte) 0xBF, (byte) 0xBF);

        for (int mode : STREAM_MODES) {
            try (JsonParser p = createParser(FACTORY, mode, json)) {
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals("\uDBFF\uDFFF", p.getString(), "mode=" + mode);
            }
        }
    }

    @Test
    void rejectUtf8CodePointsAboveUnicodeMaximum() throws Exception
    {
        byte[][] invalidSequences = {
                { (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80 },
                { (byte) 0xF5, (byte) 0x80, (byte) 0x80, (byte) 0x80 }
        };

        for (byte[] sequence : invalidSequences) {
            byte[] json = _quotedUtf8Sequence(sequence);

            for (int mode : STREAM_MODES) {
                StreamReadException read = assertThrows(StreamReadException.class, () -> {
                    try (JsonParser p = createParser(FACTORY, mode, json)) {
                        assertToken(JsonToken.VALUE_STRING, p.nextToken());
                        p.getString();
                    }
                }, "read path, mode=" + mode);

                verifyException(read, "Invalid UTF-8");
            }
        }
    }

    private byte[] _quotedUtf8Sequence(byte... sequence)
    {
        byte[] json = new byte[sequence.length + 2];
        json[0] = '"';
        System.arraycopy(sequence, 0, json, 1, sequence.length);
        json[json.length - 1] = '"';
        return json;
    }
}
