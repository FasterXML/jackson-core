/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import java.nio.charset.StandardCharsets;

/**
 * Parses a {@code double} from a {@code byte} array.
 */
final class JsonDoubleBitsFromByteArray extends AbstractJsonFloatingPointBitsFromByteArray {

    /**
     * Creates a new instance.
     */
    public JsonDoubleBitsFromByteArray() {

    }

    @Override
    long valueOfFloatLiteral(byte[] str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        double d = FastDoubleMath.tryDecFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d) ? Double.parseDouble(new String(str, startIndex, endIndex - startIndex, StandardCharsets.ISO_8859_1)) : d);
    }
}