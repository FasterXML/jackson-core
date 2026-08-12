package tools.jackson.core.unittest.io.xjb;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the fallback (non-VarHandle) byte-level operations in
 * {@code tools.jackson.core.io.xjb.XJBWriter}.
 * These are the implementations used on Android SDK and older JDKs without VarHandle.
 */
public class XJBFallbackByteOpsTest
{
    private static Method SET_INT;
    private static Method SET_SHORT;
    private static Method SET_LONG;
    private static Method GET_LONG;

    @BeforeAll
    static void findMethods() throws Exception {
        Class<?> writer = Class.forName("tools.jackson.core.io.xjb.XJBWriter");
        SET_INT = writer.getDeclaredMethod("setIntFallback", byte[].class, int.class, int.class);
        SET_INT.setAccessible(true);
        SET_SHORT = writer.getDeclaredMethod("setShortFallback", byte[].class, int.class, short.class);
        SET_SHORT.setAccessible(true);
        SET_LONG = writer.getDeclaredMethod("setLongFallback", byte[].class, int.class, long.class);
        SET_LONG.setAccessible(true);
        GET_LONG = writer.getDeclaredMethod("getLongFallback", byte[].class, int.class);
        GET_LONG.setAccessible(true);
    }

    private static void setInt(byte[] buf, int pos, int v) throws Exception {
        SET_INT.invoke(null, buf, pos, v);
    }

    private static void setShort(byte[] buf, int pos, short v) throws Exception {
        SET_SHORT.invoke(null, buf, pos, v);
    }

    private static void setLong(byte[] buf, int pos, long v) throws Exception {
        SET_LONG.invoke(null, buf, pos, v);
    }

    private static long getLong(byte[] buf, int pos) throws Exception {
        return (long) GET_LONG.invoke(null, buf, pos);
    }

    @Test
    void testSetIntFallback() throws Exception
    {
        byte[] buf = new byte[8];

        // Zero
        setInt(buf, 0, 0);
        assertEquals(0, buf[0]);
        assertEquals(0, buf[1]);
        assertEquals(0, buf[2]);
        assertEquals(0, buf[3]);

        // Little-endian: 0x04030201
        setInt(buf, 0, 0x04030201);
        assertEquals(0x01, buf[0] & 0xFF);
        assertEquals(0x02, buf[1] & 0xFF);
        assertEquals(0x03, buf[2] & 0xFF);
        assertEquals(0x04, buf[3] & 0xFF);

        // At offset
        setInt(buf, 2, 0x08070605);
        assertEquals(0x05, buf[2] & 0xFF);
        assertEquals(0x06, buf[3] & 0xFF);
        assertEquals(0x07, buf[4] & 0xFF);
        assertEquals(0x08, buf[5] & 0xFF);

        // Negative value (all bits set = -1)
        setInt(buf, 0, -1);
        for (int i = 0; i < 4; i++) {
            assertEquals(0xFF, buf[i] & 0xFF);
        }

        // 0x302E30 — the "0.0" constant used by writeFloat
        setInt(buf, 0, 0x302E30);
        assertEquals('0', buf[0]);
        assertEquals('.', buf[1]);
        assertEquals('0', buf[2]);
    }

    @Test
    void testSetShortFallback() throws Exception
    {
        byte[] buf = new byte[4];

        // Zero
        setShort(buf, 0, (short) 0);
        assertEquals(0, buf[0]);
        assertEquals(0, buf[1]);

        // Little-endian: 0x0201
        setShort(buf, 0, (short) 0x0201);
        assertEquals(0x01, buf[0] & 0xFF);
        assertEquals(0x02, buf[1] & 0xFF);

        // At offset
        setShort(buf, 2, (short) 0x0403);
        assertEquals(0x03, buf[2] & 0xFF);
        assertEquals(0x04, buf[3] & 0xFF);

        // Negative (0xFFFF = -1)
        setShort(buf, 0, (short) -1);
        assertEquals(0xFF, buf[0] & 0xFF);
        assertEquals(0xFF, buf[1] & 0xFF);

        // 0x302E little-endian: low byte first
        setShort(buf, 0, (short) 0x302E);
        assertEquals('.', buf[0]); // 0x2E
        assertEquals('0', buf[1]); // 0x30
    }

    @Test
    void testSetLongFallback() throws Exception
    {
        byte[] buf = new byte[16];

        // Zero
        setLong(buf, 0, 0L);
        for (int i = 0; i < 8; i++) {
            assertEquals(0, buf[i]);
        }

        // Little-endian: 0x0807060504030201
        setLong(buf, 0, 0x0807060504030201L);
        for (int i = 0; i < 8; i++) {
            assertEquals(i + 1, buf[i] & 0xFF);
        }

        // At offset
        setLong(buf, 4, 0x100F0E0D0C0B0A09L);
        for (int i = 0; i < 8; i++) {
            assertEquals(i + 9, buf[4 + i] & 0xFF);
        }

        // All bits set (-1L)
        setLong(buf, 0, -1L);
        for (int i = 0; i < 8; i++) {
            assertEquals(0xFF, buf[i] & 0xFF);
        }

        // High byte only
        setLong(buf, 0, 0xFF00000000000000L);
        assertEquals(0x00, buf[0] & 0xFF);
        assertEquals(0xFF, buf[7] & 0xFF);
    }

    @Test
    void testGetLongFallback() throws Exception
    {
        byte[] buf = new byte[16];

        // Zero
        assertEquals(0L, getLong(buf, 0));

        // Little-endian: 0x0807060504030201
        for (int i = 0; i < 8; i++) {
            buf[i] = (byte) (i + 1);
        }
        assertEquals(0x0807060504030201L, getLong(buf, 0));

        // At offset
        for (int i = 0; i < 8; i++) {
            buf[4 + i] = (byte) (i + 9);
        }
        assertEquals(0x100F0E0D0C0B0A09L, getLong(buf, 4));

        // All bits set
        for (int i = 0; i < 8; i++) {
            buf[i] = (byte) 0xFF;
        }
        assertEquals(-1L, getLong(buf, 0));

        // Single byte in each position
        for (int pos = 0; pos < 8; pos++) {
            for (int i = 0; i < 8; i++) buf[i] = 0;
            buf[pos] = (byte) 0xAB;
            long expected = (0xABL) << (pos * 8);
            assertEquals(expected, getLong(buf, 0),
                    "byte at position " + pos);
        }
    }

    @Test
    void testSetGetRoundTrip() throws Exception
    {
        byte[] buf = new byte[16];

        // setLong then getLong
        long[] testValues = {
            0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE,
            0x0102030405060708L, 0xDEADBEEFCAFEBABEL
        };
        for (long v : testValues) {
            setLong(buf, 0, v);
            assertEquals(v, getLong(buf, 0),
                    "round-trip for 0x" + Long.toHexString(v));
        }

        // setInt then getLong (upper 4 bytes should be zero)
        int[] intValues = { 0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 0x04030201 };
        for (int v : intValues) {
            java.util.Arrays.fill(buf, (byte) 0);
            setInt(buf, 0, v);
            long expected = v & 0xFFFFFFFFL;
            assertEquals(expected, getLong(buf, 0),
                    "int-to-long round-trip for 0x" + Integer.toHexString(v));
        }
    }
}
