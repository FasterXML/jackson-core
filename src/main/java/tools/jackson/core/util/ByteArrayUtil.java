package tools.jackson.core.util;

/**
 * Byte-shifting fallback for reading and writing multi-byte primitives into and
 * out of byte arrays, for Little- and Big-Endian cases.
 *
 * @since 3.3
 */
public final class ByteArrayUtil
{
    private ByteArrayUtil() { }

    /*
    /**********************************************************************
    /* Reading
    /**********************************************************************
     */

    /**
     * Reads 4 bytes starting at given offset as a little-endian {@code int}.
     * Caller MUST have verified that {@code offset+4} is within bounds of
     * given array.
     */
    public static int getIntLE(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF)
                | ((buffer[offset+1] & 0xFF) << 8)
                | ((buffer[offset+2] & 0xFF) << 16)
                | ((buffer[offset+3] & 0xFF) << 24);
    }

    /**
     * Reads 4 bytes starting at given offset as a big-endian {@code int}.
     * Caller MUST have verified that {@code offset+4} is within bounds of
     * given array.
     */
    public static int getIntBE(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xFF) << 24)
                | ((buffer[offset+1] & 0xFF) << 16)
                | ((buffer[offset+2] & 0xFF) << 8)
                | (buffer[offset+3] & 0xFF);
    }

    /**
     * Reads 8 bytes starting at given offset as a little-endian {@code long}.
     * Caller MUST have verified that {@code offset+8} is within bounds of
     * given array.
     */
    public static long getLongLE(byte[] buffer, int offset) {
        // the two 32-bit halves combine to exactly a little-endian 8-byte read
        final int i1 = getIntLE(buffer, offset);
        final int i2 = getIntLE(buffer, offset+4);
        return (((long) i1) & 0xFFFFFFFFL) | (((long) i2) << 32);
    }

    /**
     * Reads 8 bytes starting at given offset as a big-endian {@code long}.
     * Caller MUST have verified that {@code offset+8} is within bounds of
     * given array.
     */
    public static long getLongBE(byte[] buffer, int offset) {
        // the two 32-bit halves combine to exactly a big-endian 8-byte read
        final int i1 = getIntBE(buffer, offset);
        final int i2 = getIntBE(buffer, offset+4);
        return (((long) i1) << 32) | (((long) i2) & 0xFFFFFFFFL);
    }

    /*
    /**********************************************************************
    /* Writing
    /**********************************************************************
     */

    /**
     * Writes given {@code int} as 4 little-endian bytes at given offset; caller
     * MUST have verified that {@code offset+4} is within bounds of given array.
     */
    public static void setIntLE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) value;
        buffer[offset+1] = (byte) (value >> 8);
        buffer[offset+2] = (byte) (value >> 16);
        buffer[offset+3] = (byte) (value >> 24);
    }

    /**
     * Writes given {@code int} as 4 big-endian bytes at given offset; caller
     * MUST have verified that {@code offset+4} is within bounds of given array.
     */
    public static void setIntBE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value >> 24);
        buffer[offset+1] = (byte) (value >> 16);
        buffer[offset+2] = (byte) (value >> 8);
        buffer[offset+3] = (byte) value;
    }

    /**
     * Writes given {@code long} as 8 little-endian bytes at given offset; caller
     * MUST have verified that {@code offset+8} is within bounds of given array.
     */
    public static void setLongLE(byte[] buffer, int offset, long value) {
        setIntLE(buffer, offset, (int) value);
        setIntLE(buffer, offset+4, (int) (value >> 32));
    }

    /**
     * Writes given {@code long} as 8 big-endian bytes at given offset; caller
     * MUST have verified that {@code offset+8} is within bounds of given array.
     */
    public static void setLongBE(byte[] buffer, int offset, long value) {
        setIntBE(buffer, offset, (int) (value >> 32));
        setIntBE(buffer, offset+4, (int) value);
    }
}
