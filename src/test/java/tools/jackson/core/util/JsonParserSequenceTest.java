package tools.jackson.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


import tools.jackson.core.BaseTest;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.ReaderBasedJsonParser;
import tools.jackson.core.json.UTF8StreamJsonParser;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;
import tools.jackson.core.sym.CharsToNameCanonicalizer;

/**
 * Unit tests for class {@link JsonParserSequence}.
 *
 * @see JsonParserSequence
 */
@SuppressWarnings("resource")
public class JsonParserSequenceTest extends BaseTest
{
    public void testClose() throws IOException {
        IOContext ioContext = ioContextForTests(this);
        ReaderBasedJsonParser readerBasedJsonParser = new ReaderBasedJsonParser(
                ObjectReadContext.empty(),
                ioContext,
                2, 0, null, CharsToNameCanonicalizer.createRoot());
        JsonParserSequence jsonParserSequence = JsonParserSequence.createFlattened(true, readerBasedJsonParser, readerBasedJsonParser);

        assertFalse(jsonParserSequence.isClosed());

        jsonParserSequence.close();

        assertTrue(jsonParserSequence.isClosed());
        assertNull(jsonParserSequence.nextToken());
    }

    public void testSkipChildren() throws IOException {
        JsonParser[] jsonParserArray = new JsonParser[3];
        IOContext ioContext = ioContextForTests(this);
        byte[] byteArray = new byte[8];
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray, 0, (byte) 58);
        UTF8StreamJsonParser uTF8StreamJsonParser = new UTF8StreamJsonParser(ObjectReadContext.empty(),
                ioContext,
                0, 0, byteArrayInputStream, ByteQuadsCanonicalizer.createRoot(),
                byteArray, -1, (byte) 9, 0, true);
        JsonParserDelegate jsonParserDelegate = new JsonParserDelegate(jsonParserArray[0]);
        JsonParserSequence jsonParserSequence = JsonParserSequence.createFlattened(true, uTF8StreamJsonParser, jsonParserDelegate);
        JsonParserSequence jsonParserSequenceTwo = (JsonParserSequence) jsonParserSequence.skipChildren();

        assertEquals(2, jsonParserSequenceTwo.containedParsersCount());
    }
}