package com.fasterxml.jackson.core.io;

import java.io.*;

import com.fasterxml.jackson.core.JsonEncoding;

public class MergedStreamTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimple() throws Exception
    {
        IOContext ctxt = testIOContext();
        // bit complicated; must use recyclable buffer...
        byte[] first = ctxt.allocReadIOBuffer();
        System.arraycopy("ABCDE".getBytes("UTF-8"), 0, first, 99, 5);
        byte[] second = "FGHIJ".getBytes("UTF-8");

        assertNull(ctxt.contentReference().getRawContent());
        assertFalse(ctxt.isResourceManaged());
        ctxt.setEncoding(JsonEncoding.UTF8);
        MergedStream ms = new MergedStream(ctxt, new ByteArrayInputStream(second),
                                           first, 99, 99+5);
        ctxt.close();

        // Ok, first, should have 5 bytes from first buffer:
        assertEquals(5, ms.available());
        // not supported when there's buffered stuff...
        assertFalse(ms.markSupported());
        // so this won't work, but shouldn't throw exception
        ms.mark(1);
        assertEquals((byte) 'A', ms.read());
        assertEquals(3, ms.skip(3));
        byte[] buffer = new byte[5];
        // Ok, now, code is allowed to return anywhere between 1 and 3,
        // but we now it will return 1...
        assertEquals(1, ms.read(buffer, 1, 3));
        assertEquals((byte) 'E', buffer[1]);
        // So let's read bit more
        assertEquals(3, ms.read(buffer, 0, 3));
        assertEquals((byte) 'F', buffer[0]);
        assertEquals((byte) 'G', buffer[1]);
        assertEquals((byte) 'H', buffer[2]);
        assertEquals(2, ms.available());
        // And then skip the reset
        assertEquals(2, ms.skip(200));

        ms.close();
    }
}
