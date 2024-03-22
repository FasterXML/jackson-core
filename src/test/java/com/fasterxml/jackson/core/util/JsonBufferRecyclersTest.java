package com.fasterxml.jackson.core.util;

import java.io.StringWriter;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Basic testing for [core#1064] wrt usage by `JsonParser` / `JsonGenerator`
// (wrt simple reads/writes working without issues)
public class JsonBufferRecyclersTest extends BaseTest
{
    // // Parsers with RecyclerPools:

    @Test
    public void testParserWithThreadLocalPool() throws Exception {
        _testParser(JsonRecyclerPools.threadLocalPool());
    }

    @Test
    public void testParserWithNopLocalPool() throws Exception {
        _testParser(JsonRecyclerPools.nonRecyclingPool());
    }

    @Test
    public void testParserWithDequeuPool() throws Exception {
        _testParser(JsonRecyclerPools.newConcurrentDequePool());
        _testParser(JsonRecyclerPools.sharedConcurrentDequePool());
    }

    @Test
    public void testParserWithLockFreePool() throws Exception {
        _testParser(JsonRecyclerPools.newLockFreePool());
        _testParser(JsonRecyclerPools.sharedLockFreePool());
    }

    @Test
    public void testParserWithBoundedPool() throws Exception {
        _testParser(JsonRecyclerPools.newBoundedPool(5));
        _testParser(JsonRecyclerPools.sharedBoundedPool());
    }

    private void _testParser(RecyclerPool<BufferRecycler> pool) throws Exception
    {
        JsonFactory jsonF = JsonFactory.builder()
                .recyclerPool(pool)
                .build();

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
    }

    // // Generators with RecyclerPools:

    @Test
    public void testGeneratorWithThreadLocalPool() throws Exception {
        _testGenerator(JsonRecyclerPools.threadLocalPool());
    }

    @Test
    public void testGeneratorWithNopLocalPool() throws Exception {
        _testGenerator(JsonRecyclerPools.nonRecyclingPool());
    }

    @Test
    public void testGeneratorWithDequeuPool() throws Exception {
        _testGenerator(JsonRecyclerPools.newConcurrentDequePool());
        _testGenerator(JsonRecyclerPools.sharedConcurrentDequePool());
    }

    @Test
    public void testGeneratorWithLockFreePool() throws Exception {
        _testGenerator(JsonRecyclerPools.newLockFreePool());
        _testGenerator(JsonRecyclerPools.sharedLockFreePool());
    }

    @Test
    public void testGeneratorWithBoundedPool() throws Exception {
        _testGenerator(JsonRecyclerPools.newBoundedPool(5));
        _testGenerator(JsonRecyclerPools.sharedBoundedPool());
    }
    
    private void _testGenerator(RecyclerPool<BufferRecycler> pool) throws Exception
    {
        JsonFactory jsonF = JsonFactory.builder()
                .recyclerPool(pool)
                .build();

        StringWriter w = new StringWriter();
        JsonGenerator g = jsonF.createGenerator(w);

        g.writeStartObject();
        g.writeNumberField("a", -42);
        g.writeStringField("b", "barfoo");
        g.writeEndObject();

        g.close();

        assertEquals(a2q("{'a':-42,'b':'barfoo'}"), w.toString());
    }

    // // Read-and-Write: Parser and Generator, overlapping usage

    @Test
    public void testCopyWithThreadLocalPool() throws Exception {
        _testCopy(JsonRecyclerPools.threadLocalPool());
    }

    @Test
    public void testCopyWithNopLocalPool() throws Exception {
        _testCopy(JsonRecyclerPools.nonRecyclingPool());
    }

    @Test
    public void testCopyWithDequeuPool() throws Exception {
        _testCopy(JsonRecyclerPools.newConcurrentDequePool());
        _testCopy(JsonRecyclerPools.sharedConcurrentDequePool());
    }

    @Test
    public void testCopyWithLockFreePool() throws Exception {
        _testCopy(JsonRecyclerPools.newLockFreePool());
        _testCopy(JsonRecyclerPools.sharedLockFreePool());
    }

    @Test
    public void testCopyWithBoundedPool() throws Exception {
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
