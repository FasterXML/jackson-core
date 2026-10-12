package tools.jackson.core.unittest.json.async;

import java.io.*;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.async.ByteBufferFeeder;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

class AsyncParserConfigTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    @Test
    void factoryDefaults() throws IOException
    {
        assertTrue(DEFAULT_F.canParseAsync());
    }

    @Test
    void asyncParserDefaults() throws IOException
    {
        byte[] data = _jsonDoc("[true,false]");
        AsyncReaderWrapper r = asyncForBytes(DEFAULT_F, 100, data, 0);
        JsonParser p = r.parser();

        assertTrue(p.canParseAsync());
        assertNull(p.streamReadInputSource());
        assertEquals(-1, p.releaseBuffered(new StringWriter()));
        assertEquals(0, p.releaseBuffered(new ByteArrayOutputStream()));

        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertEquals(11, p.releaseBuffered(new ByteArrayOutputStream()));

        p.close();
    }

    // [core#1646]: must only release unconsumed bytes
    @Test
    void asyncByteBufferReleaseBuffered() throws IOException
    {
        _testByteBufferReleaseBuffered(0);
        // and with non-zero offset, to verify absolute-index handling:
        _testByteBufferReleaseBuffered(3);
    }

    private void _testByteBufferReleaseBuffered(int offset) throws IOException
    {
        byte[] data = _jsonDoc("[true,false]");
        byte[] input = new byte[offset + data.length + offset];
        System.arraycopy(data, 0, input, offset, data.length);
        ByteBuffer bb = ByteBuffer.wrap(input, offset, data.length);

        JsonParser p = DEFAULT_F.createNonBlockingByteBufferParser(ObjectReadContext.empty());
        ((ByteBufferFeeder) p.nonBlockingInputFeeder()).feedInput(bb);

        assertToken(JsonToken.START_ARRAY, p.nextToken());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] expected = utf8Bytes("true,false]");
        assertEquals(expected.length, p.releaseBuffered(out));
        assertArrayEquals(expected, out.toByteArray());

        // and buffer caller fed must be left as-is
        assertEquals(offset, bb.position());
        assertEquals(offset + data.length, bb.limit());

        p.close();
    }
}
