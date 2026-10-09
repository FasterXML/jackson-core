package tools.jackson.core.unittest.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tools.jackson.core.unittest.*;
import tools.jackson.core.util.ByteArrayUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ByteArrayUtil} accessors, checked against {@link ByteBuffer}
 * as an independent oracle.
 */
public class ByteArrayUtilTest extends JacksonCoreTestBase
{
    private final static int OFFSET = 3; // deliberately unaligned

    /*
    /**********************************************************************
    /* Test methods, reading
    /**********************************************************************
     */

    @Test
    void getShortLE()
    {
        for (byte[] input : _inputs(2)) {
            final short exp = ByteBuffer.wrap(input, OFFSET, 2)
                    .order(ByteOrder.LITTLE_ENDIAN).getShort();
            assertEquals(exp, ByteArrayUtil.getShortLE(input, OFFSET),
                    "for input "+Arrays.toString(input));
        }
    }

    @Test
    void getShortBE()
    {
        for (byte[] input : _inputs(2)) {
            final short exp = ByteBuffer.wrap(input, OFFSET, 2)
                    .order(ByteOrder.BIG_ENDIAN).getShort();
            assertEquals(exp, ByteArrayUtil.getShortBE(input, OFFSET),
                    "for input "+Arrays.toString(input));
        }
    }

    @Test
    void getIntLE()
    {
        for (byte[] input : _inputs(4)) {
            final int exp = ByteBuffer.wrap(input, OFFSET, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).getInt();
            assertEquals(exp, ByteArrayUtil.getIntLE(input, OFFSET),
                    "for input "+Arrays.toString(input));
        }
    }

    @Test
    void getIntBE()
    {
        for (byte[] input : _inputs(4)) {
            final int exp = ByteBuffer.wrap(input, OFFSET, 4)
                    .order(ByteOrder.BIG_ENDIAN).getInt();
            assertEquals(exp, ByteArrayUtil.getIntBE(input, OFFSET),
                    "for input "+Arrays.toString(input));
        }
    }

    @Test
    void getLongLE()
    {
        for (byte[] input : _inputs(8)) {
            final long exp = ByteBuffer.wrap(input, OFFSET, 8)
                    .order(ByteOrder.LITTLE_ENDIAN).getLong();
            assertEquals(exp, ByteArrayUtil.getLongLE(input, OFFSET),
                    "for input "+Arrays.toString(input));
        }
    }

    @Test
    void getLongBE()
    {
        for (byte[] input : _inputs(8)) {
            final long exp = ByteBuffer.wrap(input, OFFSET, 8)
                    .order(ByteOrder.BIG_ENDIAN).getLong();
            assertEquals(exp, ByteArrayUtil.getLongBE(input, OFFSET),
                    "for input "+Arrays.toString(input));
        }
    }

    /*
    /**********************************************************************
    /* Test methods, writing
    /**********************************************************************
     */

    @Test
    void setShortLE()
    {
        for (short value : _shortValues()) {
            byte[] exp = new byte[OFFSET+2];
            ByteBuffer.wrap(exp, OFFSET, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(value);

            byte[] act = new byte[OFFSET+2];
            ByteArrayUtil.setShortLE(act, OFFSET, value);
            assertArrayEquals(exp, act, "for value "+value);
            assertEquals(value, ByteArrayUtil.getShortLE(act, OFFSET));
        }
    }

    @Test
    void setShortBE()
    {
        for (short value : _shortValues()) {
            byte[] exp = new byte[OFFSET+2];
            ByteBuffer.wrap(exp, OFFSET, 2).order(ByteOrder.BIG_ENDIAN).putShort(value);

            byte[] act = new byte[OFFSET+2];
            ByteArrayUtil.setShortBE(act, OFFSET, value);
            assertArrayEquals(exp, act, "for value "+value);
            assertEquals(value, ByteArrayUtil.getShortBE(act, OFFSET));
        }
    }

    @Test
    void setIntLE()
    {
        for (int value : _intValues()) {
            byte[] exp = new byte[OFFSET+4];
            ByteBuffer.wrap(exp, OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value);

            byte[] act = new byte[OFFSET+4];
            ByteArrayUtil.setIntLE(act, OFFSET, value);
            assertArrayEquals(exp, act, "for value "+value);
            assertEquals(value, ByteArrayUtil.getIntLE(act, OFFSET));
        }
    }

    @Test
    void setIntBE()
    {
        for (int value : _intValues()) {
            byte[] exp = new byte[OFFSET+4];
            ByteBuffer.wrap(exp, OFFSET, 4).order(ByteOrder.BIG_ENDIAN).putInt(value);

            byte[] act = new byte[OFFSET+4];
            ByteArrayUtil.setIntBE(act, OFFSET, value);
            assertArrayEquals(exp, act, "for value "+value);
            assertEquals(value, ByteArrayUtil.getIntBE(act, OFFSET));
        }
    }

    @Test
    void setLongLE()
    {
        for (long value : _longValues()) {
            byte[] exp = new byte[OFFSET+8];
            ByteBuffer.wrap(exp, OFFSET, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(value);

            byte[] act = new byte[OFFSET+8];
            ByteArrayUtil.setLongLE(act, OFFSET, value);
            assertArrayEquals(exp, act, "for value "+value);
            assertEquals(value, ByteArrayUtil.getLongLE(act, OFFSET));
        }
    }

    @Test
    void setLongBE()
    {
        for (long value : _longValues()) {
            byte[] exp = new byte[OFFSET+8];
            ByteBuffer.wrap(exp, OFFSET, 8).order(ByteOrder.BIG_ENDIAN).putLong(value);

            byte[] act = new byte[OFFSET+8];
            ByteArrayUtil.setLongBE(act, OFFSET, value);
            assertArrayEquals(exp, act, "for value "+value);
            assertEquals(value, ByteArrayUtil.getLongBE(act, OFFSET));
        }
    }

    /*
    /**********************************************************************
    /* Test methods, endianness relations
    /**********************************************************************
     */

    @Test
    void endiannessIsReversed()
    {
        for (byte[] input : _inputs(8)) {
            assertEquals(ByteArrayUtil.getShortBE(input, OFFSET),
                    Short.reverseBytes(ByteArrayUtil.getShortLE(input, OFFSET)));
            assertEquals(ByteArrayUtil.getIntBE(input, OFFSET),
                    Integer.reverseBytes(ByteArrayUtil.getIntLE(input, OFFSET)));
            assertEquals(ByteArrayUtil.getLongBE(input, OFFSET),
                    Long.reverseBytes(ByteArrayUtil.getLongLE(input, OFFSET)));
        }
    }

    @Test
    void writesTouchNoOtherBytes()
    {
        // Writes must stay strictly within [offset, offset+size)
        byte[] b = new byte[OFFSET+8+OFFSET];
        Arrays.fill(b, (byte) 0x5A);
        ByteArrayUtil.setLongBE(b, OFFSET, -1L);
        for (int i = 0; i < OFFSET; ++i) {
            assertEquals((byte) 0x5A, b[i], "at index "+i);
            assertEquals((byte) 0x5A, b[OFFSET+8+i], "at index "+(OFFSET+8+i));
        }

        Arrays.fill(b, (byte) 0x5A);
        ByteArrayUtil.setIntLE(b, OFFSET, -1);
        for (int i = 0; i < OFFSET; ++i) {
            assertEquals((byte) 0x5A, b[i], "at index "+i);
        }
        for (int i = OFFSET+4; i < b.length; ++i) {
            assertEquals((byte) 0x5A, b[i], "at index "+i);
        }
    }

    /*
    /**********************************************************************
    /* Helper methods for building inputs: sign-bit and all-bits-set cases
    /* first, then pseudo-random ones (fixed seed, for reproducibility)
    /**********************************************************************
     */

    private byte[][] _inputs(int length) {
        final int size = OFFSET + length;
        byte[][] result = new byte[3+100][];
        result[0] = new byte[size];
        result[1] = new byte[size];
        Arrays.fill(result[1], (byte) 0xFF);
        result[2] = new byte[size];
        result[2][OFFSET] = (byte) 0x80; // high bit of first byte only
        Random rnd = new Random(1234);
        for (int i = 3; i < result.length; ++i) {
            byte[] b = new byte[size];
            rnd.nextBytes(b);
            result[i] = b;
        }
        return result;
    }

    private short[] _shortValues() {
        short[] result = new short[4+100];
        result[0] = 0;
        result[1] = -1;
        result[2] = Short.MIN_VALUE;
        result[3] = Short.MAX_VALUE;
        Random rnd = new Random(3456);
        for (int i = 4; i < result.length; ++i) {
            result[i] = (short) rnd.nextInt();
        }
        return result;
    }

    private int[] _intValues() {
        int[] result = new int[4+100];
        result[0] = 0;
        result[1] = -1;
        result[2] = Integer.MIN_VALUE;
        result[3] = Integer.MAX_VALUE;
        Random rnd = new Random(5678);
        for (int i = 4; i < result.length; ++i) {
            result[i] = rnd.nextInt();
        }
        return result;
    }

    private long[] _longValues() {
        long[] result = new long[4+100];
        result[0] = 0L;
        result[1] = -1L;
        result[2] = Long.MIN_VALUE;
        result[3] = Long.MAX_VALUE;
        Random rnd = new Random(9012);
        for (int i = 4; i < result.length; ++i) {
            result[i] = rnd.nextLong();
        }
        return result;
    }
}
