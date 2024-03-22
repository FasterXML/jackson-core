package tools.jackson.core.json.async;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    @Test
    void factoryDefaults() throws IOException
    {
        assertTrue(DEFAULT_F.canParseAsync());
    }

    @Test
    void asyncParerDefaults() throws IOException
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
}
