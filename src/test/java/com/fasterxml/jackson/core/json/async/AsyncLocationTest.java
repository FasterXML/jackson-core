package com.fasterxml.jackson.core.json.async;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.async.ByteBufferFeeder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncLocationTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    // for [core#531]
    @Test
    void locationOffsets() throws Exception
    {
        JsonParser parser = DEFAULT_F.createNonBlockingByteArrayParser();
        ByteArrayFeeder feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();

        byte[] input = utf8Bytes("[[[");

        feeder.feedInput(input, 2, 3);
        assertEquals(JsonToken.START_ARRAY, parser.nextToken());
        assertEquals(1, parser.currentLocation().getByteOffset());
        assertEquals(1, parser.currentTokenLocation().getByteOffset());
        assertEquals(1, parser.currentLocation().getLineNr());
        assertEquals(1, parser.currentTokenLocation().getLineNr());
        assertEquals(2, parser.currentLocation().getColumnNr());
        assertEquals(1, parser.currentTokenLocation().getColumnNr());

        feeder.feedInput(input, 0, 1);
        assertEquals(JsonToken.START_ARRAY, parser.nextToken());
        assertEquals(2, parser.currentLocation().getByteOffset());
        assertEquals(2, parser.currentTokenLocation().getByteOffset());
        assertEquals(1, parser.currentLocation().getLineNr());
        assertEquals(1, parser.currentTokenLocation().getLineNr());
        assertEquals(3, parser.currentLocation().getColumnNr());
        assertEquals(2, parser.currentTokenLocation().getColumnNr());
        parser.close();
    }

    @Test
    void locationOffsetsByteBuffer() throws Exception
    {
        JsonParser parser = DEFAULT_F.createNonBlockingByteBufferParser();
        ByteBufferFeeder feeder = (ByteBufferFeeder) parser.getNonBlockingInputFeeder();
        String input = "[[[";

        feeder.feedInput(ByteBuffer.wrap(utf8Bytes(input), 2, 1));
        assertEquals(JsonToken.START_ARRAY, parser.nextToken());
        assertEquals(1, parser.currentLocation().getByteOffset());
        assertEquals(1, parser.currentTokenLocation().getByteOffset());
        assertEquals(1, parser.currentLocation().getLineNr());
        assertEquals(1, parser.currentTokenLocation().getLineNr());
        assertEquals(2, parser.currentLocation().getColumnNr());
        assertEquals(1, parser.currentTokenLocation().getColumnNr());

        feeder.feedInput(ByteBuffer.wrap(utf8Bytes(input), 0, 1));
        assertEquals(JsonToken.START_ARRAY, parser.nextToken());
        assertEquals(2, parser.currentLocation().getByteOffset());
        assertEquals(2, parser.currentTokenLocation().getByteOffset());
        assertEquals(1, parser.currentLocation().getLineNr());
        assertEquals(1, parser.currentTokenLocation().getLineNr());
        assertEquals(3, parser.currentLocation().getColumnNr());
        assertEquals(2, parser.currentTokenLocation().getColumnNr());
        parser.close();
    }
}
