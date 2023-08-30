package com.fasterxml.jackson.core.write;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.BufferRecyclerPool;

import java.io.IOException;
import java.io.OutputStream;

public class BufferRecyclerPoolTest extends BaseTest
{
    public void testNoOp() {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.NonRecyclingPool.shared());
    }

    public void testThreadLocal() {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.ThreadLocalPool.shared());
    }

    public void testLockFree() {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.LockFreePool.nonShared());
    }

    public void testConcurrentDequeue() {
        checkBufferRecyclerPoolImpl(BufferRecyclerPool.ConcurrentDequePool.nonShared());
    }

    private void checkBufferRecyclerPoolImpl(BufferRecyclerPool pool) {
        JsonFactory jsonFactory = new JsonFactory().setBufferRecyclerPool(pool);
        assertEquals(6, write("test", jsonFactory));
    }

    protected final int write(Object value, JsonFactory jsonFactory) {
        NopOutputStream out = new NopOutputStream();
        try (JsonGenerator gen = jsonFactory.createGenerator(out)) {
            gen.writeObject(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.size();
    }

    public class NopOutputStream extends OutputStream {
        protected int size = 0;

        public NopOutputStream() { }

        @Override
        public void write(int b) throws IOException { ++size; }

        @Override
        public void write(byte[] b) throws IOException { size += b.length; }

        @Override
        public void write(byte[] b, int offset, int len) throws IOException { size += len; }

        public NopOutputStream reset() {
            size = 0;
            return this;
        }
        public int size() { return size; }
    }
}
