package tools.jackson.core.io.xjb;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * VarHandle-based little-endian byte array access for Java 9+.
 * Separated from {@link XJBWriter} so animal-sniffer can exclude it
 * while still verifying Android SDK compatibility of the main class.
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
