package tools.jackson.core.unittest.read;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.exc.UnexpectedEndOfInputException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

// [core#1752]: invalid token reporting with DataInput must neither swallow
// I/O failures nor report end-of-input as I/O failure
class DataInputInvalidToken1752Test
    extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    private final static int[] BYTE_MODES = { MODE_INPUT_STREAM, MODE_DATA_INPUT };

    @Test
    void invalidTokenPreservesReadFailure() throws Exception
    {
        final IOException failure = new IOException("test-induced read failure");
        try (JsonParser p = createParserForDataInput(JSON_F,
                new DataInputStream(_failAfter("a", failure)))) {
            JacksonIOException e = assertThrows(JacksonIOException.class, p::nextToken);
            verifyException(e, failure.getMessage());
            assertSame(failure, e.getCause());
            assertSame(p, e.processor());
        }
    }

    @Test
    void base64PreservesReadFailure() throws Exception
    {
        final IOException failure = new IOException("test-induced read failure");
        try (JsonParser p = createParserForDataInput(JSON_F,
                new DataInputStream(_failAfter("\"AQID", failure)))) {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            JacksonIOException e = assertThrows(JacksonIOException.class,
                    () -> p.getBinaryValue());
            assertSame(failure, e.getCause());
        }
    }

    @Test
    void base64TruncatedAtEOF() throws Exception
    {
        for (int mode : BYTE_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, utf8Bytes("\"AQID"))) {
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertThrows(UnexpectedEndOfInputException.class, () -> p.getBinaryValue());
            }
        }
    }

    @Test
    void readBinaryTruncatedAtEOF() throws Exception
    {
        for (int mode : BYTE_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, utf8Bytes("\"AQID"))) {
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertThrows(UnexpectedEndOfInputException.class,
                        () -> p.readBinaryValue(new ByteArrayOutputStream()));
            }
        }
    }

    @Test
    void invalidTokenAtEOF() throws Exception
    {
        _testInvalidToken("abc", "abc");
        _testInvalidToken("tru", "tru");
        _testInvalidToken("truea", "truea");
        _testInvalidToken("nullx", "nullx");
        _testInvalidToken("falsez", "falsez");
        _testInvalidToken("é", "é");
        _testInvalidToken("[ tr", "tr");
    }

    @Test
    void invalidTokenMalformedUtf8() throws Exception
    {
        for (int mode : BYTE_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, new byte[] { 'a', (byte) 0xC2, 'A' })) {
                StreamReadException e = assertThrows(StreamReadException.class, p::nextToken);
                verifyException(e, "Invalid UTF-8 middle byte 0x41");
            }
        }
    }

    @Test
    void invalidTokenTruncatedUtf8() throws Exception
    {
        // Multi-byte char following invalid token: token itself still reported
        try (JsonParser p = createParserForDataInput(JSON_F,
                new DataInputStream(new ByteArrayInputStream(new byte[] { 'a', (byte) 0xC2 })))) {
            StreamReadException e = assertThrows(StreamReadException.class, p::nextToken);
            verifyException(e, "Unrecognized token 'a'");
        }
    }

    @Test
    void truncatedUtf8AtEOF() throws Exception
    {
        _testTruncated(new byte[] { 't', 'r', 'u', 'e', (byte) 0xC3 });
        _testTruncated(new byte[] { (byte) 0xC3 });
        _testTruncated(new byte[] { '{', (byte) 0xC3 });
    }

    private void _testInvalidToken(String doc, String token) throws Exception
    {
        for (int mode : BYTE_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, utf8Bytes(doc))) {
                StreamReadException e = assertThrows(StreamReadException.class, () -> {
                    while (p.nextToken() != null) { }
                });
                verifyException(e, "Unrecognized token '"+token+"'");
            }
        }
    }

    private void _testTruncated(byte[] doc) throws Exception
    {
        for (int mode : BYTE_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, doc)) {
                UnexpectedEndOfInputException e = assertThrows(UnexpectedEndOfInputException.class, () -> {
                    while (p.nextToken() != null) { }
                });
                if (mode == MODE_DATA_INPUT) {
                    verifyException(e, "in a multi-byte UTF-8 character");
                }
            }
        }
    }

    private static InputStream _failAfter(String content, IOException failure) {
        final byte[] data = content.getBytes(StandardCharsets.UTF_8);
        return new InputStream() {
            private int _offset;

            @Override
            public int read() throws IOException {
                if (_offset < data.length) {
                    return data[_offset++] & 0xFF;
                }
                throw failure;
            }
        };
    }
}
