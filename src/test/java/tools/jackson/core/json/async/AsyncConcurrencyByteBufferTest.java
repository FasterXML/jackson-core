package tools.jackson.core.json.async;

import java.io.IOException;

import tools.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncConcurrencyByteBufferTest extends AsyncConcurrencyTest {

    @Override
    protected AsyncReaderWrapper createParser() throws IOException {
        return asyncForByteBuffer(JSON_F, 100, JSON_DOC, 0);
    }
}
