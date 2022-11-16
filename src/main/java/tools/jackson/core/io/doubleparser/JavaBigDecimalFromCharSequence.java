/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package tools.jackson.core.io.doubleparser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.RecursiveTask;


/**
 * Parses a {@code double} from a {@code byte} array.
 */
final class JavaBigDecimalFromCharSequence {
    /**
     * Threshold on the number of digits for selecting the
     * recursive algorithm instead of the quadratic algorithm.
     * <p>
     * Set this to {@link Integer#MAX_VALUE} if you only want to use the
     * quadratic algorithm.
     * <p>
     * Set this to {@code 0} if you only want to use the recursive algorithm.
     * <p>
     * Rationale for choosing a specific threshold value:
     * The quadratic algorithm has a smaller constant overhead than the
     * recursive algorithm. We speculate that we break even somewhere at twice
     * the threshold value.
     */
    public static final int RECURSION_THRESHOLD = 128;
    /**
     * Threshold on the number of digits for selecting the multi-threaded
     * algorithm instead of the single-thread algorithm.
     * <p>
     * Set this to {@link Integer#MAX_VALUE} if you only want to use
     * the single-threaded algorithm.
     * <p>
     * Set this to {@code 0} if you only want to use the multi-threaded
     * algorithm.
     * <p>
     * Rationale for choosing a specific threshold value:
     * We speculate that we need to perform at least 10,000 CPU cycles
     * before it is worth using multiple threads.
     */
    public static final int PARALLEL_THRESHOLD = 1024;
    /**
     * Threshold on the number of input characters for selecting the
     * algorithm optimised for few digits in the significand vs. the algorithm for many
     * digits in the significand.
     * <p>
     * Set this to {@link Integer#MAX_VALUE} if you only want to use
     * the algorithm optimised for few digits in the significand.
     * <p>
     * Set this to {@code 0} if you only want to use the algorithm for
     * long inputs.
     * <p>
     * Rationale for choosing a specific threshold value:
     * We speculate that we only need to use the algorithm for large inputs
     * if there is zero chance, that we can parse the input with the algorithm
     * for small inputs.
     * <pre>
     * optional significant sign = 1
     * 18 significant digits = 18
     * optional decimal point in significant = 1
     * optional exponent = 1
     * optional exponent sign = 1
     * 10 exponent digits = 10
     * </pre>
     */
    public static final int MANY_DIGITS_THRESHOLD = 1 + 18 + 1 + 1 + 1 + 10;
    /**
     * See {@link JavaBigDecimalParser}.
     */
    private final static int MAX_DIGIT_COUNT = 1_292_782_621;
    private final static long MAX_EXPONENT_NUMBER = Integer.MAX_VALUE;
    private final static Map<Integer, BigInteger> SMALL_POWERS_OF_TEN = new HashMap<>();

    static {
        SMALL_POWERS_OF_TEN.put(0, BigInteger.ONE);
        SMALL_POWERS_OF_TEN.put(1, BigInteger.TEN);
        SMALL_POWERS_OF_TEN.put(2, BigInteger.valueOf(100L));
        SMALL_POWERS_OF_TEN.put(3, BigInteger.valueOf(1_000L));
        SMALL_POWERS_OF_TEN.put(4, BigInteger.valueOf(10_000L));
        SMALL_POWERS_OF_TEN.put(5, BigInteger.valueOf(100_000L));
        SMALL_POWERS_OF_TEN.put(6, BigInteger.valueOf(1_000_000L));
        SMALL_POWERS_OF_TEN.put(7, BigInteger.valueOf(10_000_000L));
        SMALL_POWERS_OF_TEN.put(8, BigInteger.valueOf(100_000_000L));
        SMALL_POWERS_OF_TEN.put(9, BigInteger.valueOf(1_000_000_000L));
        SMALL_POWERS_OF_TEN.put(10, BigInteger.valueOf(10_000_000_000L));
        SMALL_POWERS_OF_TEN.put(11, BigInteger.valueOf(100_000_000_000L));
        SMALL_POWERS_OF_TEN.put(12, BigInteger.valueOf(1_000_000_000_000L));
        SMALL_POWERS_OF_TEN.put(13, BigInteger.valueOf(10_000_000_000_000L));
        SMALL_POWERS_OF_TEN.put(14, BigInteger.valueOf(100_000_000_000_000L));
        SMALL_POWERS_OF_TEN.put(15, BigInteger.valueOf(1_000_000_000_000_000L));

    }

    private final static BigInteger TEN_POW_16 = BigInteger.valueOf(10_000_000_000_000_000L);

    /**
     * Creates a new instance.
     */
    public JavaBigDecimalFromCharSequence() {

    }

    /**
     * Parses digits in O(n^2) with a large
     * constant overhead if there are less than 19 digits.
     *
     * @param str  the input string
     * @param from the start index of the digit sequence in str (inclusive)
     * @param to   the end index of the digit sequence in str (exclusive)
     * @return a {@link BigDecimal}
     */
    static BigInteger parseDigitsQuadratic(CharSequence str, int from, int to) {
        int numDigits = to - from;
        BigSignificand bigSignificand = new BigSignificand(BigSignificand.estimateNumBits(numDigits));
        int preroll = from + (numDigits & 7);
        bigSignificand.add(FastDoubleSwar.parseUpTo7Digits(str, from, preroll));
        for (from = preroll; from < to; from += 8) {
            bigSignificand.fma(100_000_000, FastDoubleSwar.parseEightDigits(str, from));
        }

        return bigSignificand.toBigInteger();
    }

    /**
     * Parses digits using a recursive algorithm in O(n^1.5) with a large
     * constant overhead if there are less than about 100 digits.
     *
     * @param str  the input string
     * @param from the start index of the digit sequence in str (inclusive)
     * @param to   the end index of the digit sequence in str (exclusive)
     * @return a {@link BigDecimal}
     */
    private static BigInteger parseDigitsRecursive(CharSequence str, int from, int to, Map<Integer, BigInteger> powersOfTen) {
        // Base case: All sequences of 18 or fewer digits fit into a long.
        int numDigits = to - from;
        if (numDigits <= 18) {
            return parseDigitsUpTo18(str, from, to);
        }
        if (numDigits <= RECURSION_THRESHOLD) {
            return parseDigitsQuadratic(str, from, to);
        }

        // Recursion case: Sequences of more than 18 digits do not fit into a long.
        int mid = split(from, to);
        BigInteger high = parseDigitsRecursive(str, from, mid, powersOfTen);
        BigInteger low = parseDigitsRecursive(str, mid, to, powersOfTen);

        high = high.multiply(powersOfTen.get(to - mid));
        return low.add(high);
    }

    /**
     * Parses up to 18 digits in O(n), with
     * a small constant overhead.
     *
     * @param str  the input string
     * @param from the start index of the digit sequence in str (inclusive)
     * @param to   the end index of the digit sequence in str (exclusive)
     */
    private static BigInteger parseDigitsUpTo18(CharSequence str, int from, int to) {
        int numDigits = to - from;
        int preroll = from + (numDigits & 7);
        long significand = FastDoubleSwar.parseUpTo7Digits(str, from, preroll);
        for (from = preroll; from < to; from += 8) {
            significand = significand * 100_000_000L + FastDoubleSwar.parseEightDigits(str, from);
        }
        return BigInteger.valueOf(significand);
    }

    private static int split(int from, int to) {
        int mid = (from + to) >>> 1;// split in half
        mid = to - (((to - mid + 15) >> 4) << 4);// make numDigits of low a multiple of 16
        return mid;
    }

    /**
     * Computes the n-th power of ten.
     *
     * @param powersOfTen A map with pre-computed powers of ten
     * @param n           the power
     * @return the computed power of ten
     */
    private BigInteger computePowerOfTen(NavigableMap<Integer, BigInteger> powersOfTen, int n) {
        BigInteger pow;
        BigInteger p = powersOfTen.get(n);
        if (p == null) {
            p = computeTenRaisedByNFloor16Recursive(powersOfTen, n);
        }
        int remainder = n & 15;
        if (remainder == 0) {
            pow = p;
        } else {
            pow = p.multiply(SMALL_POWERS_OF_TEN.get(remainder));
        }
        return pow;
    }

    /**
     * Computes 10<sup>n&~15</sup>.
     */
    private BigInteger computeTenRaisedByNFloor16Recursive(NavigableMap<Integer, BigInteger> powersOfTen, int n) {
        n = n & ~15;
        Map.Entry<Integer, BigInteger> floorEntry = powersOfTen.floorEntry(n);
        int floorPower = floorEntry.getKey();
        BigInteger floorValue = floorEntry.getValue();
        if (floorPower == n) {
            return floorValue;
        }
        int diff = n - floorPower;
        BigInteger diffValue = powersOfTen.get(diff);
        if (diffValue == null) {
            diffValue = computeTenRaisedByNFloor16Recursive(powersOfTen, diff);
            powersOfTen.put(diff, diffValue);
        }
        return floorValue.multiply(diffValue);
    }

    /**
     * Fills the provided map with powers of 10. The map only contains powers that
     * are divisible by 16.
     * <p>
     * This algorithm uses the same split-method that the recursion algorithm
     * in method {@link #parseDigitsRecursive(CharSequence, int, int, Map)}
     * uses. This ensures that compute all powers of ten, that the
     * recursion algorithm needs.
     *
     * @param from index of the first digit character (inclusive)
     * @param to   index of the last digit character (exclusive)
     * @return a map with the powers of 10 that the recursion algorithm
     * needs.
     */
    private void fillPowersOfTenFloor16Recursive(NavigableMap<Integer, BigInteger> powersOfTen, int from, int to) {
        int numDigits = to - from;
        // base case:
        if (numDigits <= 18) {
            return;
        }
        // recursion case:
        int mid = split(from, to);
        int n = to - mid;
        if (!powersOfTen.containsKey(n)) {
            fillPowersOfTenFloor16Recursive(powersOfTen, from, mid);
            fillPowersOfTenFloor16Recursive(powersOfTen, mid, to);
            powersOfTen.put(n, computeTenRaisedByNFloor16Recursive(powersOfTen, n));
        }
        return;
    }

    private boolean isDigit(char c) {
        return (char) '0' <= c && c <= (char) '9';
    }

    /**
     * Parses a {@code BigDecimalString} as specified in {@link JavaBigDecimalParser}.
     *
     * @param str    the input string
     * @param offset start of the input data
     * @param length length of the input data
     * @return the parsed {@link BigDecimal} or null
     */
    public BigDecimal parseBigDecimalString(CharSequence str, int offset, int length) {
        if (length >= MANY_DIGITS_THRESHOLD) {
            return parseBigDecimalStringWithManyDigits(str, offset, length);
        }
        long significand = 0L;
        final int integerPartIndex;
        int decimalPointIndex = -1;
        final int exponentIndicatorIndex;

        final int endIndex = offset + length;
        int index = offset;
        char ch = index < endIndex ? str.charAt(index) : 0;
        boolean illegal = false;


        // Parse optional sign
        // -------------------
        final boolean isNegative = ch == '-';
        if (isNegative || ch == '+') {
            ch = ++index < endIndex ? str.charAt(index) : 0;
            if (ch == 0) {
                return null;
            }
        }

        // Parse significand
        integerPartIndex = index;
        for (; index < endIndex; index++) {
            ch = str.charAt(index);
            if (isDigit(ch)) {
                // This might overflow, we deal with it later.
                significand = 10 * (significand) + ch - '0';
            } else if (ch == '.') {
                illegal |= decimalPointIndex >= 0;
                decimalPointIndex = index;
                for (; index < endIndex - 4; index += 4) {
                    int digits = FastDoubleSwar.tryToParseFourDigits(str, index + 1);
                    if (digits < 0) {
                        break;
                    }
                    // This might overflow, we deal with it later.
                    significand = 10_000L * significand + digits;
                }
            } else {
                break;
            }
        }

        final int digitCount;
        final int significandEndIndex = index;
        long exponent;
        if (decimalPointIndex < 0) {
            digitCount = significandEndIndex - integerPartIndex;
            decimalPointIndex = significandEndIndex;
            exponent = 0;
        } else {
            digitCount = significandEndIndex - integerPartIndex - 1;
            exponent = decimalPointIndex - significandEndIndex + 1;
        }

        // Parse exponent number
        // ---------------------
        long expNumber = 0;
        if (ch == 'e' || ch == 'E') {
            exponentIndicatorIndex = index;
            ch = ++index < endIndex ? str.charAt(index) : 0;
            boolean isExponentNegative = ch == '-';
            if (isExponentNegative || ch == '+') {
                ch = ++index < endIndex ? str.charAt(index) : 0;
            }
            illegal |= !isDigit(ch);
            do {
                // Guard against overflow
                if (expNumber < MAX_EXPONENT_NUMBER) {
                    expNumber = 10 * (expNumber) + ch - '0';
                }
                ch = ++index < endIndex ? str.charAt(index) : 0;
            } while (isDigit(ch));
            if (isExponentNegative) {
                expNumber = -expNumber;
            }
            exponent += expNumber;
        } else {
            exponentIndicatorIndex = endIndex;
        }
        if (illegal || index < endIndex
                || digitCount == 0
                || exponent < Integer.MIN_VALUE
                || exponent > Integer.MAX_VALUE
                || digitCount > MAX_DIGIT_COUNT) {
            return null;
        }

        if (digitCount <= 18) {
            return new BigDecimal(isNegative ? -significand : significand).scaleByPowerOfTen((int) exponent);
        }
        return valueOfBigDecimalString(str, integerPartIndex, decimalPointIndex, exponentIndicatorIndex, isNegative, (int) exponent);
    }

    /**
     * Parses a big decimal string that has many digits.
     */
    private BigDecimal parseBigDecimalStringWithManyDigits(CharSequence str, int offset, int length) {
        final int integerPartIndex;
        int decimalPointIndex = -1;
        final int exponentIndicatorIndex;

        final int endIndex = offset + length;
        int index = offset;
        char ch = index < endIndex ? str.charAt(index) : 0;
        boolean illegal = false;

        // Parse optional sign
        // -------------------
        final boolean isNegative = ch == '-';
        if (isNegative || ch == '+') {
            ch = ++index < endIndex ? str.charAt(index) : 0;
            if (ch == 0) {
                return null;
            }
        }

        // Count digits of significand
        // -----------------
        // Count digits of integer part
        integerPartIndex = index;
        while (index < endIndex - 8 && FastDoubleSwar.isEightDigits(str, index)) {
            index += 8;
        }
        while (index < endIndex && isDigit(ch = str.charAt(index))) {
            index++;
        }
        if (ch == '.') {
            decimalPointIndex = index++;
            // Count digits of fraction part
            while (index < endIndex - 8 && FastDoubleSwar.isEightDigits(str, index)) {
                index += 8;
            }
            while (index < endIndex && isDigit(ch = str.charAt(index))) {
                index++;
            }
        }

        final int digitCount;
        final int significandEndIndex = index;
        long exponent;
        if (decimalPointIndex < 0) {
            digitCount = significandEndIndex - integerPartIndex;
            decimalPointIndex = significandEndIndex;
            exponent = 0;
        } else {
            digitCount = significandEndIndex - integerPartIndex - 1;
            exponent = decimalPointIndex - significandEndIndex + 1;
        }

        // Parse exponent number
        // ---------------------
        long expNumber = 0;
        if (ch == 'e' || ch == 'E') {
            exponentIndicatorIndex = index;
            ch = ++index < endIndex ? str.charAt(index) : 0;
            boolean isExponentNegative = ch == '-';
            if (isExponentNegative || ch == '+') {
                ch = ++index < endIndex ? str.charAt(index) : 0;
            }
            illegal = !isDigit(ch);
            do {
                // Guard against overflow
                if (expNumber < MAX_EXPONENT_NUMBER) {
                    expNumber = 10 * (expNumber) + ch - '0';
                }
                ch = ++index < endIndex ? str.charAt(index) : 0;
            } while (isDigit(ch));
            if (isExponentNegative) {
                expNumber = -expNumber;
            }
            exponent += expNumber;
        } else {
            exponentIndicatorIndex = endIndex;
        }
        if (illegal || index < endIndex
                || digitCount == 0
                || exponent < Integer.MIN_VALUE
                || exponent > Integer.MAX_VALUE
                || digitCount > MAX_DIGIT_COUNT) {
            return null;
        }
        return valueOfBigDecimalString(str, integerPartIndex, decimalPointIndex, exponentIndicatorIndex, isNegative, (int) exponent);
    }

    private BigInteger parseDigits(CharSequence str, int from, int to, Map<Integer, BigInteger> powersOfTen) {
        int numDigits = to - from;
        if (numDigits < PARALLEL_THRESHOLD) {
            return parseDigitsRecursive(str, from, to, powersOfTen);
        } else {
            return new ParseDigitsTask(str, from, to, powersOfTen).compute();
        }
    }

    private BigDecimal valueOfBigDecimalString(CharSequence str, int integerPartIndex, int decimalPointIndex, int exponentIndicatorIndex, boolean isNegative, int exponent) {
        int fractionDigitsCount = exponentIndicatorIndex - decimalPointIndex - 1;
        int integerDigitsCount = decimalPointIndex - integerPartIndex;

        // Create a map with powers of ten. The map is used for combining
        // sequences of digits. A navigable map is useful for filling the map,
        // however for querying, a hash map has better performance.
        // This is why we copy the map before we call the parseDigits() method.
        NavigableMap<Integer, BigInteger> powersOfTen = new TreeMap<>();
        powersOfTen.put(0, BigInteger.ONE);
        powersOfTen.put(16, TEN_POW_16);

        BigInteger integerPart;
        if (integerDigitsCount > 0) {
            if (integerDigitsCount > RECURSION_THRESHOLD) {
                fillPowersOfTenFloor16Recursive(powersOfTen, integerPartIndex, decimalPointIndex);
                integerPart = parseDigits(str, integerPartIndex, decimalPointIndex, new HashMap(powersOfTen));
            } else {
                integerPart = parseDigits(str, integerPartIndex, decimalPointIndex, null);
            }
        } else {
            integerPart = BigInteger.ZERO;
        }

        BigInteger significand;
        if (fractionDigitsCount > 0) {
            BigInteger fractionalPart;
            if (fractionDigitsCount > RECURSION_THRESHOLD) {
                fillPowersOfTenFloor16Recursive(powersOfTen, decimalPointIndex + 1, exponentIndicatorIndex);
                fractionalPart = parseDigits(str, decimalPointIndex + 1, exponentIndicatorIndex, new HashMap(powersOfTen));
            } else {
                fractionalPart = parseDigits(str, decimalPointIndex + 1, exponentIndicatorIndex, null);
            }
            if (integerPart.signum() == 0) {
                significand = fractionalPart;
            } else {
                BigInteger integerPower = computePowerOfTen(powersOfTen, exponentIndicatorIndex - decimalPointIndex - 1);
                significand = integerPart.multiply(integerPower).add(fractionalPart);
            }
        } else {
            significand = integerPart;
        }

        BigDecimal result = new BigDecimal(significand, -exponent);
        return isNegative ? result.negate() : result;
    }

    /**
     * Parses digits using a multi-threaded recursive algorithm in O(n^1.5)
     * with a large constant overhead if there are less than about 1000
     * digits.
     */
    static class ParseDigitsTask extends RecursiveTask<BigInteger> {
        private final int from, to;
        private final CharSequence str;
        private final Map<Integer, BigInteger> powersOfTen;

        ParseDigitsTask(CharSequence str, int from, int to, Map<Integer, BigInteger> powersOfTen) {
            this.from = from;
            this.to = to;
            this.str = str;
            this.powersOfTen = powersOfTen;
        }

        protected BigInteger compute() {
            int range = to - from;
            // Base case:
            if (range <= PARALLEL_THRESHOLD) {
                return parseDigitsRecursive(str, from, to, powersOfTen);
            }

            // Recursion case:
            int mid = split(from, to);
            JavaBigIntegerFromCharSequence.ParseDigitsTask high = new JavaBigIntegerFromCharSequence.ParseDigitsTask(str, from, mid, powersOfTen);
            JavaBigIntegerFromCharSequence.ParseDigitsTask low = new JavaBigIntegerFromCharSequence.ParseDigitsTask(str, mid, to, powersOfTen);
            // perform about half the work locally
            low.fork();
            BigInteger highValue = high.compute().multiply(powersOfTen.get(to - mid));
            return low.join().add(highValue);
        }
    }
}