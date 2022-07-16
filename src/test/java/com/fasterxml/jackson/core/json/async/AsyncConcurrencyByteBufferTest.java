package com.fasterxml.jackson.core.json.async;

import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import java.io.IOException;

public class AsyncConcurrencyByteBufferTest extends AsyncConcurrencyTest {

    @Override
    protected AsyncReaderWrapper createParser() throws IOException {
        return asyncForByteBuffer(JSON_F, 100, JSON_DOC, 0);
    }
}
