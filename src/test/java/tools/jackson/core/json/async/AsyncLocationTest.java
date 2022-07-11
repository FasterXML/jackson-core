package tools.jackson.core.json.async;

import tools.jackson.core.*;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.json.JsonFactory;

public class AsyncLocationTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    // for [core#531]
    public void testLocationOffsets() throws Exception
    {
        JsonParser parser = DEFAULT_F.createNonBlockingByteArrayParser(ObjectReadContext.empty());
        ByteArrayFeeder feeder = (ByteArrayFeeder) parser.nonBlockingInputFeeder();

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
}
