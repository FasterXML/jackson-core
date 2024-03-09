package com.fasterxml.jackson.core.io;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.JsonRecyclerPools;

import static org.junit.jupiter.api.Assertions.*;

public class SegmentedStringWriterTest
    extends TestBase
{
    @Test
    public void testSimple() throws Exception
    {
        BufferRecycler br = new BufferRecycler();
        SegmentedStringWriter w = new SegmentedStringWriter(br);

        StringBuilder exp = new StringBuilder();

        for (int i = 0; exp.length() < 100; ++i) {
            String nr = String.valueOf(i);
            exp.append(' ').append(nr);
            w.append(' ');
            switch (i % 4) {
            case 0:
                w.append(nr);
                break;
            case 1:
                {
                    String str = "  "+nr;
                    w.append(str, 2, str.length());
                }
                break;
            case 2:
                w.write(nr.toCharArray());
                break;
            default:
                {
                    char[] ch = (" "+nr+" ").toCharArray();
                    w.write(ch, 1, nr.length());
                }
                break;
            }
        }
        // flush, close are nops but trigger just for fun
        w.flush();
        w.close();

        String act = w.getAndClear();
        assertEquals(exp.toString(), act);
    }

    // [core#1195]: Try to verify that BufferRecycler instance is indeed reused
    @Test
    public void testBufferRecyclerReuse() throws Exception
    {
        JsonFactory f = new JsonFactory();
        BufferRecycler br = new BufferRecycler()
                // need to link with some pool
                .withPool(JsonRecyclerPools.newBoundedPool(3));

        SegmentedStringWriter ssw = new SegmentedStringWriter(br);
        assertSame(br, ssw.bufferRecycler());

        JsonGenerator g = f.createGenerator(ssw);
        IOContext ioCtxt = ((GeneratorBase) g).ioContext();
        assertSame(br, ioCtxt.bufferRecycler());
        assertTrue(ioCtxt.bufferRecycler().isLinkedWithPool());

        g.writeStartArray();
        g.writeEndArray();
        g.close();

        // Generator.close() should NOT release buffer recycler
        assertTrue(br.isLinkedWithPool());

        // Nor accessing contents
        assertEquals("[]", ssw.getAndClear());
        assertTrue(br.isLinkedWithPool());

        // only explicit release does
        br.releaseToPool();
        assertFalse(br.isLinkedWithPool());
    }
}
