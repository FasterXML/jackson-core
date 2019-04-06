package com.fasterxml.jackson.core.json.async;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;

public class AsyncLocationTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    // for [core#531]
    public void testLocationOffsets() throws Exception
    {
        JsonParser parser = DEFAULT_F.createNonBlockingByteArrayParser();
        ByteArrayFeeder feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();

        byte[] input = utf8Bytes("[[[");

        feeder.feedInput(input, 2, 3);
        assertEquals(JsonToken.START_ARRAY, parser.nextToken());
        assertEquals(1, parser.getCurrentLocation().getByteOffset());
        assertEquals(1, parser.getTokenLocation().getByteOffset());
        assertEquals(1, parser.getCurrentLocation().getLineNr());
        assertEquals(1, parser.getTokenLocation().getLineNr());
        assertEquals(2, parser.getCurrentLocation().getColumnNr());
        assertEquals(1, parser.getTokenLocation().getColumnNr());

        feeder.feedInput(input, 0, 1);
        assertEquals(JsonToken.START_ARRAY, parser.nextToken());
        assertEquals(2, parser.getCurrentLocation().getByteOffset());
        assertEquals(2, parser.getTokenLocation().getByteOffset());
        assertEquals(1, parser.getCurrentLocation().getLineNr());
        assertEquals(1, parser.getTokenLocation().getLineNr());
        assertEquals(3, parser.getCurrentLocation().getColumnNr());
        assertEquals(2, parser.getTokenLocation().getColumnNr());
        parser.close();
    }
}
