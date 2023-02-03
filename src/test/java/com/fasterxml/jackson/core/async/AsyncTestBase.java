package com.fasterxml.jackson.core.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapperForByteArray;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapperForByteBuffer;

public abstract class AsyncTestBase extends BaseTest
{
    final static String SPACES = "                ";

    protected final static char UNICODE_2BYTES = (char) 167; // law symbol
    protected final static char UNICODE_3BYTES = (char) 0x4567;

    protected final static String UNICODE_SEGMENT = "["+UNICODE_2BYTES+"/"+UNICODE_3BYTES+"]";

    public static AsyncReaderWrapper asyncForBytes(JsonFactory f,
            int bytesPerRead,
            byte[] bytes, int padding) throws IOException
    {
        return new AsyncReaderWrapperForByteArray(f.createNonBlockingByteArrayParser(),
                bytesPerRead, bytes, padding);
    }

    public static AsyncReaderWrapper asyncForByteBuffer(JsonFactory f,
                                                        int bytesPerRead,
                                                        byte[] bytes, int padding) throws IOException
    {
        return new AsyncReaderWrapperForByteBuffer(f.createNonBlockingByteBufferParser(),
                bytesPerRead, bytes, padding);
    }

    protected static String spaces(int count)
    {
        return SPACES.substring(0, Math.min(SPACES.length(), count));
    }

    protected final JsonToken verifyStart(AsyncReaderWrapper reader) throws Exception
    {
        assertToken(JsonToken.NOT_AVAILABLE, reader.currentToken());
        return reader.nextToken();
    }

    protected final byte[] _jsonDoc(String doc) throws IOException {
        return doc.getBytes("UTF-8");
    }
}
