package com.fasterxml.jackson.core.util;

import java.io.IOException;

public interface BufferRecyclerOwner extends AutoCloseable {

    BufferRecycler getBufferRecycler();

    @Override
    default void close() throws IOException {
        BufferRecycler br = getBufferRecycler();
        if (br != null) {
            br.releaseToPool();
        }
    }
}
