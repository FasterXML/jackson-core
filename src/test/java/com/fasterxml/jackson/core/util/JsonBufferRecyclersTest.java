package com.fasterxml.jackson.core.util;

import java.io.StringWriter;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Basic testing for [core#1064] wrt usage by `JsonParser` / `JsonGenerator`
// (wrt simple reads/writes working without issues)
class JsonBufferRecyclersTest extends JUnit5TestBase
{
    // // Parsers with RecyclerPools:

    @Test
    void parserWithThreadLocalPool() throws Exception {
        _testParser(JsonRecyclerPools.threadLocalPool(), -1, -1);
    }

    @Test
    void parserWithNopLocalPool() throws Exception {
        _testParser(JsonRecyclerPools.nonRecyclingPool(), 0, 0);
    }

    @Test
    void parserWithDequeuPool() throws Exception {
        _testParser(JsonRecyclerPools.newConcurrentDequePool(), 0, 1);
        _testParser(JsonRecyclerPools.sharedConcurrentDequePool(), null, null);
    }

    @Test
    @Deprecated
    void parserWithLockFreePool() throws Exception {
        _testParser(JsonRecyclerPools.newLockFreePool(), 0, 1);
        _testParser(JsonRecyclerPools.sharedLockFreePool(), null, null);
    }

    @Test
    void parserWithBoundedPool() throws Exception {
        _testParser(JsonRecyclerPools.newBoundedPool(5), 0, 1);
        _testParser(JsonRecyclerPools.sharedBoundedPool(), null, null);
    }

    private void _testParser(RecyclerPool<BufferRecycler> pool,
            Integer expSizeBefore, Integer expSizeAfter) throws Exception
    {
        JsonFactory jsonF = JsonFactory.builder()
                .recyclerPool(pool)
                .build();
        if (expSizeBefore != null) {
            assertEquals(expSizeBefore, pool.pooledCount());
        }

        JsonParser p = jsonF.createParser(a2q("{'a':123,'b':'foobar'}"));

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(123, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.currentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("foobar", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        
        p.close();

        if (expSizeAfter != null) {
            assertEquals(expSizeAfter, pool.pooledCount());
        }
    }

    // // Generators with RecyclerPools:

    @Test
    void generatorWithThreadLocalPool() throws Exception {
        _testGenerator(JsonRecyclerPools.threadLocalPool(), -1, -1);
    }

    @Test
    void generatorWithNopLocalPool() throws Exception {
        _testGenerator(JsonRecyclerPools.nonRecyclingPool(), 0, 0);
    }

    @Test
    void generatorWithDequeuPool() throws Exception {
        _testGenerator(JsonRecyclerPools.newConcurrentDequePool(), 0, 1);
        _testGenerator(JsonRecyclerPools.sharedConcurrentDequePool(), null, null);
    }

    @Test
    @Deprecated
    void generatorWithLockFreePool() throws Exception {
        _testGenerator(JsonRecyclerPools.newLockFreePool(), 0, 1);
        _testGenerator(JsonRecyclerPools.sharedLockFreePool(), null, null);
    }

    @Test
    void generatorWithBoundedPool() throws Exception {
        _testGenerator(JsonRecyclerPools.newBoundedPool(5), 0, 1);
        _testGenerator(JsonRecyclerPools.sharedBoundedPool(), null, null);
    }

    private void _testGenerator(RecyclerPool<BufferRecycler> pool,
            Integer expSizeBefore, Integer expSizeAfter) throws Exception
    {
        JsonFactory jsonF = JsonFactory.builder()
                .recyclerPool(pool)
                .build();

        if (expSizeBefore != null) {
            assertEquals(expSizeBefore, pool.pooledCount());
        }

        StringWriter w = new StringWriter();
        JsonGenerator g = jsonF.createGenerator(w);

        g.writeStartObject();
        g.writeNumberField("a", -42);
        g.writeStringField("b", "barfoo");
        g.writeEndObject();

        g.close();

        if (expSizeAfter != null) {
            assertEquals(expSizeAfter, pool.pooledCount());
        }

        assertEquals(a2q("{'a':-42,'b':'barfoo'}"), w.toString());
    }

    // // Read-and-Write: Parser and Generator, overlapping usage

    @Test
    void copyWithThreadLocalPool() throws Exception {
        _testCopy(JsonRecyclerPools.threadLocalPool());
    }

    @Test
    void copyWithNopLocalPool() throws Exception {
        _testCopy(JsonRecyclerPools.nonRecyclingPool());
    }

    @Test
    void copyWithDequeuPool() throws Exception {
        _testCopy(JsonRecyclerPools.newConcurrentDequePool());
        _testCopy(JsonRecyclerPools.sharedConcurrentDequePool());
    }

    @Test
    @Deprecated
    void copyWithLockFreePool() throws Exception {
        _testCopy(JsonRecyclerPools.newLockFreePool());
        _testCopy(JsonRecyclerPools.sharedLockFreePool());
    }

    @Test
    void copyWithBoundedPool() throws Exception {
        _testCopy(JsonRecyclerPools.newBoundedPool(5));
        _testCopy(JsonRecyclerPools.sharedBoundedPool());
    }

    private void _testCopy(RecyclerPool<BufferRecycler> pool) throws Exception
    {
        JsonFactory jsonF = JsonFactory.builder()
                .recyclerPool(pool)
                .build();

        final String DOC = a2q("{'a':123,'b':'foobar'}");
        JsonParser p = jsonF.createParser(DOC);
        StringWriter w = new StringWriter();
        JsonGenerator g = jsonF.createGenerator(w);

        while (p.nextToken() != null) {
            g.copyCurrentEvent(p);
        }

        p.close();
        g.close();

        assertEquals(DOC, w.toString());
    }
}
