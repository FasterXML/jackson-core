package com.fasterxml.jackson.core.util;

import org.junit.Assert;

public class ByteArrayBuilderTest extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimple() throws Exception
    {
        ByteArrayBuilder b = new ByteArrayBuilder(null, 20);
        Assert.assertArrayEquals(new byte[0], b.toByteArray());

        b.write((byte) 0);
        b.append(1);

        byte[] foo = new byte[98];
        for (int i = 0; i < foo.length; ++i) {
            foo[i] = (byte) (2 + i);
        }
        b.write(foo);

        byte[] result = b.toByteArray();
        assertEquals(100, result.length);
        for (int i = 0; i < 100; ++i) {
            assertEquals(i, (int) result[i]);
        }
        b.release();
        b.close();
    }

    public void testAppendFourBytesWithPositive() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler);

        assertEquals(0, byteArrayBuilder.size());

        byteArrayBuilder.appendFourBytes(2);

        assertEquals(4, byteArrayBuilder.size());
        assertEquals(0, byteArrayBuilder.toByteArray()[0]);
        assertEquals(0, byteArrayBuilder.toByteArray()[1]);
        assertEquals(0, byteArrayBuilder.toByteArray()[2]);
        assertEquals(2, byteArrayBuilder.toByteArray()[3]);
        byteArrayBuilder.close();
    }

    public void testAppendTwoBytesWithZero() {
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(0);

        assertEquals(0, byteArrayBuilder.size());

        byteArrayBuilder.appendTwoBytes(0);

        assertEquals(2, byteArrayBuilder.size());
        assertEquals(0, byteArrayBuilder.toByteArray()[0]);
        byteArrayBuilder.close();
    }

    public void testFinishCurrentSegment() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler, 2);
        byteArrayBuilder.appendThreeBytes(2);

        assertEquals(3, byteArrayBuilder.getCurrentSegmentLength());

        /*byte[] byteArray =*/ byteArrayBuilder.finishCurrentSegment();

        assertEquals(0, byteArrayBuilder.getCurrentSegmentLength());
        byteArrayBuilder.close();
    }
}