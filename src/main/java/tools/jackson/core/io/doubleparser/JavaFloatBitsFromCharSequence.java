/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package tools.jackson.core.io.doubleparser;

/**
 * Parses a {@code float} from a {@link CharSequence}.
 */
final class JavaFloatBitsFromCharSequence extends AbstractJavaFloatingPointBitsFromCharSequence {


    /**
     * Creates a new instance.
     */
    public JavaFloatBitsFromCharSequence() {

    }

    @Override
    long nan() {
        return Float.floatToRawIntBits(Float.NaN);
    }

    @Override
    long negativeInfinity() {
        return Float.floatToRawIntBits(Float.NEGATIVE_INFINITY);
    }

    @Override
    long positiveInfinity() {
        return Float.floatToRawIntBits(Float.POSITIVE_INFINITY);
    }

    @Override
    long valueOfFloatLiteral(CharSequence str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        float d = FastFloatMath.decFloatLiteralToFloat(isNegative, significand, exponent, isSignificandTruncated, exponentOfTruncatedSignificand);
        return Float.floatToRawIntBits(Float.isNaN(d) ? Float.parseFloat(str.subSequence(startIndex, endIndex).toString()) : d);
    }

    @Override
    long valueOfHexLiteral(
            CharSequence str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        float d = FastFloatMath.hexFloatLiteralToFloat(isNegative, significand, exponent, isSignificandTruncated, exponentOfTruncatedSignificand);
        return Float.floatToRawIntBits(Float.isNaN(d) ? Float.parseFloat(str.subSequence(startIndex, endIndex).toString()) : d);
    }

}