package tools.jackson.core.io.xjb;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * VarHandle-based little-endian byte array access.
 * Kept separate from {@link XJBWriter} so that a runtime without {@code VarHandle}
 * (Android API &lt; 33) only fails to initialize this class; {@link XJBWriter} probes it
 * once via {@link #selfTest()} and otherwise uses {@link tools.jackson.core.util.ByteArrayUtil}.
 */
final class XJBVarHandleAccess {

    private static final VarHandle INT_LE =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle SHORT_LE =
            MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG_LE =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private XJBVarHandleAccess() {
    }

    /**
     * Round-trips a value through the handles; returns {@code true} if they work.
     * Any failure (missing {@code VarHandle}, unsupported access mode) propagates
     * as an exception to the caller.
     */
    static boolean selfTest() {
        final byte[] buf = new byte[8];
        setLong(buf, 0, 0x0807060504030201L);
        setInt(buf, 0, 0x0D0C0B0A);
        setShort(buf, 4, (short) 0x0F0E);
        return (getLong(buf, 0) == 0x08070F0E0D0C0B0AL);
    }

    static void setInt(byte[] buf, int pos, int v) {
        INT_LE.set(buf, pos, v);
    }

    static void setShort(byte[] buf, int pos, short v) {
        SHORT_LE.set(buf, pos, v);
    }

    static void setLong(byte[] buf, int pos, long v) {
        LONG_LE.set(buf, pos, v);
    }

    static long getLong(byte[] buf, int pos) {
        return (long) LONG_LE.get(buf, pos);
    }
}
