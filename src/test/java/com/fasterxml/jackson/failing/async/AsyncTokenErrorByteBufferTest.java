package com.fasterxml.jackson.failing.async;

import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import java.io.IOException;

public class AsyncTokenErrorByteBufferTest extends AsyncTokenErrorTest {

    @Override
    protected AsyncReaderWrapper _createParser(String doc) throws IOException {
        return asyncForByteBuffer(JSON_F, 1, _jsonDoc(doc), 1);
    }
}
