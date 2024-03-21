package com.fasterxml.jackson.core.json.async;

import java.io.*;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    @Test
    public void testFactoryDefaults() throws IOException
    {
        assertTrue(DEFAULT_F.canParseAsync());
    }

    @Test
    public void testAsyncParerDefaults() throws IOException
    {
        byte[] data = _jsonDoc("[true,false]");
        AsyncReaderWrapper r = asyncForBytes(DEFAULT_F, 100, data, 0);
        JsonParser p = r.parser();

        assertTrue(p.canParseAsync());
        assertNull(p.getCodec());
        assertNull(p.getInputSource());
        assertEquals(-1, p.releaseBuffered(new StringWriter()));
        assertEquals(0, p.releaseBuffered(new ByteArrayOutputStream()));

        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertEquals(11, p.releaseBuffered(new ByteArrayOutputStream()));

        p.close();
    }
}
