/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package tools.jackson.core.io.doubleparser;

public class FastIntegerMath {
    /**
     * Don't let anyone instantiate this class.
     */
    private FastIntegerMath() {

    }

    /**
     * Returns {@code a * 10}.
     * <p>
     * We compute {@code (a + a * 4) * 2}, which is {@code (a + (a << 2)) << 1}.
     * <p>
     * Expected assembly code on x64:
     * <pre>
     * lea     rax, [rdi+rdi*4]
     * add     rax, rax
     * </pre>
     * Expected assembly code on aarch64:
     * <pre>
     * add     x0, x0, x0, lsl 2
     * lsl     x0, x0, 1
     * </pre>
     */
    public static long mul10L(long a) {
        return (a + (a << 2)) << 1;
    }

    /**
     * Returns {@code a * 10}.
     * <p>
     * We compute {@code (a + a * 4) * 2}, which is {@code (a + (a << 2)) << 1}.
     * <p>
     * Expected assembly code on x64:
     * <pre>
     * lea     eax, [rdi+rdi*4]
     * add     eax, eax
     * </pre>
     * Expected assembly code on aarch64:
     * <pre>
     * add     w0, w0, w0, lsl 2
     * lsl     w0, w0, 1
     * </pre>
     */
    public static int mul10(int a) {
        return (a + (a << 2)) << 1;
    }

    /**
     * Computes {@code uint128 product = (uint64)x * (uint64)y}.
     * <p>
     * References:
     * <dl>
     *     <dt>Getting the high part of 64 bit integer multiplication</dt>
     *     <dd><a href="https://stackoverflow.com/questions/28868367/getting-the-high-part-of-64-bit-integer-multiplication">
     *         stackoverflow</a></dd>
     * </dl>
     *
     * @param x uint64 factor x
     * @param y uint64 factor y
     * @return uint128 product of x and y
     */
    static UInt128 fullMultiplication(long x, long y) {//before Java 18
        long x0 = x & 0xffffffffL, x1 = x >>> 32;
        long y0 = y & 0xffffffffL, y1 = y >>> 32;
        long p11 = x1 * y1, p01 = x0 * y1;
        long p10 = x1 * y0, p00 = x0 * y0;

        // 64-bit product + two 32-bit values
        long middle = p10 + (p00 >>> 32) + (p01 & 0xffffffffL);
        return new UInt128(
                // 64-bit product + two 32-bit values
                p11 + (middle >>> 32) + (p01 >>> 32),
                // Add LOW PART and lower half of MIDDLE PART
                (middle << 32) | (p00 & 0xffffffffL));
    }
    static class UInt128 {
        final long high, low;

        private UInt128(long high, long low) {
            this.high = high;
            this.low = low;
        }
    }
}
