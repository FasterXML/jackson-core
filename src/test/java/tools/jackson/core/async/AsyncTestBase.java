package tools.jackson.core.async;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testutil.AsyncReaderWrapper;
import tools.jackson.core.testutil.AsyncReaderWrapperForByteArray;
import tools.jackson.core.testutil.AsyncReaderWrapperForByteBuffer;

public abstract class AsyncTestBase extends JUnit5TestBase
{
    final static String SPACES = "                ";

    protected final static char UNICODE_2BYTES = (char) 167; // law symbol
    protected final static char UNICODE_3BYTES = (char) 0x4567;

    protected final static String UNICODE_SEGMENT = "["+UNICODE_2BYTES+"/"+UNICODE_3BYTES+"]";

    public static AsyncReaderWrapper asyncForBytes(TokenStreamFactory f,
            int bytesPerRead,
            byte[] bytes, int padding)
    {
        return new AsyncReaderWrapperForByteArray(f.createNonBlockingByteArrayParser(ObjectReadContext.empty()),
                bytesPerRead, bytes, padding);
    }

    public static AsyncReaderWrapper asyncForByteBuffer(JsonFactory f,
            int bytesPerRead,
            byte[] bytes, int padding) throws JacksonException
    {
        return new AsyncReaderWrapperForByteBuffer(f.createNonBlockingByteBufferParser(ObjectReadContext.empty()),
                bytesPerRead, bytes, padding);
    }

    protected static String spaces(int count)
    {
        return SPACES.substring(0, Math.min(SPACES.length(), count));
    }

    protected final JsonToken verifyStart(AsyncReaderWrapper reader)
    {
        assertToken(JsonToken.NOT_AVAILABLE, reader.currentToken());
        return reader.nextToken();
    }

    protected final byte[] _jsonDoc(String doc) {
        return utf8Bytes(doc);
    }
}
