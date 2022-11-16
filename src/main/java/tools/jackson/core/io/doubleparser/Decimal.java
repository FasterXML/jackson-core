/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package tools.jackson.core.io.doubleparser;

import java.util.Objects;

class Decimal {
    final static short[] number_of_digits_decimal_left_shift_table = {
            0x0000, 0x0800, 0x0801, 0x0803, 0x1006, 0x1009, 0x100D, 0x1812, 0x1817,
            0x181D, 0x2024, 0x202B, 0x2033, 0x203C, 0x2846, 0x2850, 0x285B, 0x3067,
            0x3073, 0x3080, 0x388E, 0x389C, 0x38AB, 0x38BB, 0x40CC, 0x40DD, 0x40EF,
            0x4902, 0x4915, 0x4929, 0x513E, 0x5153, 0x5169, 0x5180, 0x5998, 0x59B0,
            0x59C9, 0x61E3, 0x61FD, 0x6218, 0x6A34, 0x6A50, 0x6A6D, 0x6A8B, 0x72AA,
            0x72C9, 0x72E9, 0x7B0A, 0x7B2B, 0x7B4D, (short) 0x8370, (short) 0x8393, (short) 0x83B7, (short) 0x83DC,
            (short) 0x8C02, (short) 0x8C28, (short) 0x8C4F, (short) 0x9477, (short) 0x949F, (short) 0x94C8, (short) 0x9CF2, 0x051C, 0x051C,
            0x051C, 0x051C,
    };
    private final static int MAX_DIGITS = 768;
    private static final long UINT64_MAX = -1L;
    private static final int INFINITE_POWER = 0x7ff;
    private static final int MAX_SHIFT = 60;
    private static final int NUM_POWERS = 19;
    private static final int[] POWERS = {
            0, 3, 6, 9, 13, 16, 19, 23, 26, 29, //
            33, 36, 39, 43, 46, 49, 53, 56, 59,     //
    };
    private final static int DECIMAL_POINT_RANGE = 2047;
    private final static int MINIMUM_EXPONENT = -1023;
    private final static int MANTISSA_EXPLICIT_BITS = 52;
    private final static byte[]
            NUMBER_OF_DIGITS_DECIMAL_LEFT_SHIFT_TABLE_POWERS_OF_5 = {
            5, 2, 5, 1, 2, 5, 6, 2, 5, 3, 1, 2, 5, 1, 5, 6, 2, 5, 7, 8, 1, 2, 5, 3,
            9, 0, 6, 2, 5, 1, 9, 5, 3, 1, 2, 5, 9, 7, 6, 5, 6, 2, 5, 4, 8, 8, 2, 8,
            1, 2, 5, 2, 4, 4, 1, 4, 0, 6, 2, 5, 1, 2, 2, 0, 7, 0, 3, 1, 2, 5, 6, 1,
            0, 3, 5, 1, 5, 6, 2, 5, 3, 0, 5, 1, 7, 5, 7, 8, 1, 2, 5, 1, 5, 2, 5, 8,
            7, 8, 9, 0, 6, 2, 5, 7, 6, 2, 9, 3, 9, 4, 5, 3, 1, 2, 5, 3, 8, 1, 4, 6,
            9, 7, 2, 6, 5, 6, 2, 5, 1, 9, 0, 7, 3, 4, 8, 6, 3, 2, 8, 1, 2, 5, 9, 5,
            3, 6, 7, 4, 3, 1, 6, 4, 0, 6, 2, 5, 4, 7, 6, 8, 3, 7, 1, 5, 8, 2, 0, 3,
            1, 2, 5, 2, 3, 8, 4, 1, 8, 5, 7, 9, 1, 0, 1, 5, 6, 2, 5, 1, 1, 9, 2, 0,
            9, 2, 8, 9, 5, 5, 0, 7, 8, 1, 2, 5, 5, 9, 6, 0, 4, 6, 4, 4, 7, 7, 5, 3,
            9, 0, 6, 2, 5, 2, 9, 8, 0, 2, 3, 2, 2, 3, 8, 7, 6, 9, 5, 3, 1, 2, 5, 1,
            4, 9, 0, 1, 1, 6, 1, 1, 9, 3, 8, 4, 7, 6, 5, 6, 2, 5, 7, 4, 5, 0, 5, 8,
            0, 5, 9, 6, 9, 2, 3, 8, 2, 8, 1, 2, 5, 3, 7, 2, 5, 2, 9, 0, 2, 9, 8, 4,
            6, 1, 9, 1, 4, 0, 6, 2, 5, 1, 8, 6, 2, 6, 4, 5, 1, 4, 9, 2, 3, 0, 9, 5,
            7, 0, 3, 1, 2, 5, 9, 3, 1, 3, 2, 2, 5, 7, 4, 6, 1, 5, 4, 7, 8, 5, 1, 5,
            6, 2, 5, 4, 6, 5, 6, 6, 1, 2, 8, 7, 3, 0, 7, 7, 3, 9, 2, 5, 7, 8, 1, 2,
            5, 2, 3, 2, 8, 3, 0, 6, 4, 3, 6, 5, 3, 8, 6, 9, 6, 2, 8, 9, 0, 6, 2, 5,
            1, 1, 6, 4, 1, 5, 3, 2, 1, 8, 2, 6, 9, 3, 4, 8, 1, 4, 4, 5, 3, 1, 2, 5,
            5, 8, 2, 0, 7, 6, 6, 0, 9, 1, 3, 4, 6, 7, 4, 0, 7, 2, 2, 6, 5, 6, 2, 5,
            2, 9, 1, 0, 3, 8, 3, 0, 4, 5, 6, 7, 3, 3, 7, 0, 3, 6, 1, 3, 2, 8, 1, 2,
            5, 1, 4, 5, 5, 1, 9, 1, 5, 2, 2, 8, 3, 6, 6, 8, 5, 1, 8, 0, 6, 6, 4, 0,
            6, 2, 5, 7, 2, 7, 5, 9, 5, 7, 6, 1, 4, 1, 8, 3, 4, 2, 5, 9, 0, 3, 3, 2,
            0, 3, 1, 2, 5, 3, 6, 3, 7, 9, 7, 8, 8, 0, 7, 0, 9, 1, 7, 1, 2, 9, 5, 1,
            6, 6, 0, 1, 5, 6, 2, 5, 1, 8, 1, 8, 9, 8, 9, 4, 0, 3, 5, 4, 5, 8, 5, 6,
            4, 7, 5, 8, 3, 0, 0, 7, 8, 1, 2, 5, 9, 0, 9, 4, 9, 4, 7, 0, 1, 7, 7, 2,
            9, 2, 8, 2, 3, 7, 9, 1, 5, 0, 3, 9, 0, 6, 2, 5, 4, 5, 4, 7, 4, 7, 3, 5,
            0, 8, 8, 6, 4, 6, 4, 1, 1, 8, 9, 5, 7, 5, 1, 9, 5, 3, 1, 2, 5, 2, 2, 7,
            3, 7, 3, 6, 7, 5, 4, 4, 3, 2, 3, 2, 0, 5, 9, 4, 7, 8, 7, 5, 9, 7, 6, 5,
            6, 2, 5, 1, 1, 3, 6, 8, 6, 8, 3, 7, 7, 2, 1, 6, 1, 6, 0, 2, 9, 7, 3, 9,
            3, 7, 9, 8, 8, 2, 8, 1, 2, 5, 5, 6, 8, 4, 3, 4, 1, 8, 8, 6, 0, 8, 0, 8,
            0, 1, 4, 8, 6, 9, 6, 8, 9, 9, 4, 1, 4, 0, 6, 2, 5, 2, 8, 4, 2, 1, 7, 0,
            9, 4, 3, 0, 4, 0, 4, 0, 0, 7, 4, 3, 4, 8, 4, 4, 9, 7, 0, 7, 0, 3, 1, 2,
            5, 1, 4, 2, 1, 0, 8, 5, 4, 7, 1, 5, 2, 0, 2, 0, 0, 3, 7, 1, 7, 4, 2, 2,
            4, 8, 5, 3, 5, 1, 5, 6, 2, 5, 7, 1, 0, 5, 4, 2, 7, 3, 5, 7, 6, 0, 1, 0,
            0, 1, 8, 5, 8, 7, 1, 1, 2, 4, 2, 6, 7, 5, 7, 8, 1, 2, 5, 3, 5, 5, 2, 7,
            1, 3, 6, 7, 8, 8, 0, 0, 5, 0, 0, 9, 2, 9, 3, 5, 5, 6, 2, 1, 3, 3, 7, 8,
            9, 0, 6, 2, 5, 1, 7, 7, 6, 3, 5, 6, 8, 3, 9, 4, 0, 0, 2, 5, 0, 4, 6, 4,
            6, 7, 7, 8, 1, 0, 6, 6, 8, 9, 4, 5, 3, 1, 2, 5, 8, 8, 8, 1, 7, 8, 4, 1,
            9, 7, 0, 0, 1, 2, 5, 2, 3, 2, 3, 3, 8, 9, 0, 5, 3, 3, 4, 4, 7, 2, 6, 5,
            6, 2, 5, 4, 4, 4, 0, 8, 9, 2, 0, 9, 8, 5, 0, 0, 6, 2, 6, 1, 6, 1, 6, 9,
            4, 5, 2, 6, 6, 7, 2, 3, 6, 3, 2, 8, 1, 2, 5, 2, 2, 2, 0, 4, 4, 6, 0, 4,
            9, 2, 5, 0, 3, 1, 3, 0, 8, 0, 8, 4, 7, 2, 6, 3, 3, 3, 6, 1, 8, 1, 6, 4,
            0, 6, 2, 5, 1, 1, 1, 0, 2, 2, 3, 0, 2, 4, 6, 2, 5, 1, 5, 6, 5, 4, 0, 4,
            2, 3, 6, 3, 1, 6, 6, 8, 0, 9, 0, 8, 2, 0, 3, 1, 2, 5, 5, 5, 5, 1, 1, 1,
            5, 1, 2, 3, 1, 2, 5, 7, 8, 2, 7, 0, 2, 1, 1, 8, 1, 5, 8, 3, 4, 0, 4, 5,
            4, 1, 0, 1, 5, 6, 2, 5, 2, 7, 7, 5, 5, 5, 7, 5, 6, 1, 5, 6, 2, 8, 9, 1,
            3, 5, 1, 0, 5, 9, 0, 7, 9, 1, 7, 0, 2, 2, 7, 0, 5, 0, 7, 8, 1, 2, 5, 1,
            3, 8, 7, 7, 7, 8, 7, 8, 0, 7, 8, 1, 4, 4, 5, 6, 7, 5, 5, 2, 9, 5, 3, 9,
            5, 8, 5, 1, 1, 3, 5, 2, 5, 3, 9, 0, 6, 2, 5, 6, 9, 3, 8, 8, 9, 3, 9, 0,
            3, 9, 0, 7, 2, 2, 8, 3, 7, 7, 6, 4, 7, 6, 9, 7, 9, 2, 5, 5, 6, 7, 6, 2,
            6, 9, 5, 3, 1, 2, 5, 3, 4, 6, 9, 4, 4, 6, 9, 5, 1, 9, 5, 3, 6, 1, 4, 1,
            8, 8, 8, 2, 3, 8, 4, 8, 9, 6, 2, 7, 8, 3, 8, 1, 3, 4, 7, 6, 5, 6, 2, 5,
            1, 7, 3, 4, 7, 2, 3, 4, 7, 5, 9, 7, 6, 8, 0, 7, 0, 9, 4, 4, 1, 1, 9, 2,
            4, 4, 8, 1, 3, 9, 1, 9, 0, 6, 7, 3, 8, 2, 8, 1, 2, 5, 8, 6, 7, 3, 6, 1,
            7, 3, 7, 9, 8, 8, 4, 0, 3, 5, 4, 7, 2, 0, 5, 9, 6, 2, 2, 4, 0, 6, 9, 5,
            9, 5, 3, 3, 6, 9, 1, 4, 0, 6, 2, 5,
    };
    int num_digits;
    int decimal_point;
    boolean negative;
    boolean truncated;
    /**
     * digits is treated as an unsigned byte array.
     */
    byte[] digits = new byte[MAX_DIGITS];

    static AdjustedMantissa compute_float(Decimal d) {
        AdjustedMantissa answer = new AdjustedMantissa();
        if (d.num_digits == 0) {
            //should be zero
            answer.power2 = 0;
            answer.mantissa = 0;
            return answer;
        }
        // At this point, going further, we can assume that d.num_digits > 0.
        //
        // We want to guard against excessive decimal point values because
        // they can result in long run times. Indeed, we do
        // shifts by at most 60 bits. We have that log(10**400)/log(2**60) ~= 22
        // which is fine, but log(10**299995)/log(2**60) ~= 16609 which is not
        // fine (runs for a long time).
        //
        if (d.decimal_point < -324) {
            // We have something smaller than 1e-324 which is always zero
            // in binary64 and binary32.
            // It should be zero.
            answer.power2 = 0;
            answer.mantissa = 0;
            return answer;
        } else if (d.decimal_point >= 310) {
            // We have something at least as large as 0.1e310 which is
            // always infinite.
            answer.power2 = INFINITE_POWER;
            answer.mantissa = 0;
            return answer;
        }
        int exp2 = 0;
        while (d.decimal_point > 0) {
            int n = Math.abs(d.decimal_point);
            int shift = (n < NUM_POWERS) ? POWERS[n] : MAX_SHIFT;
            decimal_right_shift(d, shift);
            if (d.decimal_point < -DECIMAL_POINT_RANGE) {
                // should be zero
                answer.power2 = 0;
                answer.mantissa = 0;
                return answer;
            }
            exp2 += (shift);
        }
        // We shift left toward [1/2 ... 1].
        while (d.decimal_point <= 0) {
            int shift;
            if (d.decimal_point == 0) {
                if (d.digits[0] >= 5) {
                    break;
                }
                shift = (d.digits[0] < 2) ? 2 : 1;
            } else {
                int n = Math.abs(-d.decimal_point);
                shift = (n < NUM_POWERS) ? POWERS[n] : MAX_SHIFT;
            }
            decimal_left_shift(d, shift);
            if (d.decimal_point > DECIMAL_POINT_RANGE) {
                // we want to get infinity:
                answer.power2 = INFINITE_POWER;
                answer.mantissa = 0;
                return answer;
            }
            exp2 -= (shift);
        }
        // We are now in the range [1/2 ... 1] but the binary format uses [1 ... 2].
        exp2--;
        while ((MINIMUM_EXPONENT + 1) > exp2) {
            int n = Math.abs((MINIMUM_EXPONENT + 1) - exp2);
            if (n > MAX_SHIFT) {
                n = MAX_SHIFT;
            }
            decimal_right_shift(d, n);
            exp2 += (n);
        }
        if ((exp2 - MINIMUM_EXPONENT) >= INFINITE_POWER) {
            answer.power2 = INFINITE_POWER;
            answer.mantissa = 0;
            return answer;
        }

        int mantissa_size_in_bits = MANTISSA_EXPLICIT_BITS + 1;
        decimal_left_shift(d, mantissa_size_in_bits);

        long mantissa = round(d);//ulong
        // It is possible that we have an overflow, in which case we need
        // to shift back.
        if (Long.compareUnsigned(mantissa, (1L << mantissa_size_in_bits)) >= 0) {
            decimal_right_shift(d, 1);
            exp2 += 1;
            mantissa = round(d);
            if ((exp2 - MINIMUM_EXPONENT) >= INFINITE_POWER) {
                answer.power2 = INFINITE_POWER;
                answer.mantissa = 0;
                return answer;
            }
        }
        answer.power2 = exp2 - MINIMUM_EXPONENT;
        if (Long.compareUnsigned(mantissa, ((1L) << MANTISSA_EXPLICIT_BITS)) < 0) {
            answer.power2--;
        }
        answer.mantissa = mantissa & ((1L << MANTISSA_EXPLICIT_BITS) - 1);
        return answer;
    }

    /**
     * computes h * 2^shift
     */
    static void decimal_left_shift(Decimal h, int shift) {
        if (h.num_digits == 0) {
            return;
        }
        int num_new_digits = number_of_digits_decimal_left_shift(h, shift);
        int read_index = (h.num_digits - 1);
        int write_index = h.num_digits - 1 + num_new_digits;
        long n = 0;//ulong

        while (read_index >= 0) {
            n += ((long) h.digits[read_index]) << shift;
            long quotient = Long.divideUnsigned(n, 10);//ulong
            long remainder = n - (10 * quotient);//ulong
            if (write_index < MAX_DIGITS) {
                h.digits[write_index] = (byte) (remainder);
            } else if (remainder > 0) {
                h.truncated = true;
            }
            n = quotient;
            write_index--;
            read_index--;
        }
        while (n != 0) {
            long quotient = Long.divideUnsigned(n, 10);//ulong
            long remainder = n - (10 * quotient);//ulong
            if (write_index < MAX_DIGITS) {
                h.digits[write_index] = (byte) (remainder);
            } else if (remainder > 0) {
                h.truncated = true;
            }
            n = quotient;
            write_index--;
        }
        assert write_index == -1 : write_index + " must be -1 after we produced " + num_new_digits + " new digits";
        h.num_digits += num_new_digits;
        if (h.num_digits > MAX_DIGITS) {
            h.num_digits = MAX_DIGITS;
        }
        h.decimal_point += (num_new_digits);
        trim(h);
    }

    /**
     * computes h * 2^-shift.
     */
    static void decimal_right_shift(Decimal h, int shift) {
        int read_index = 0;
        int write_index = 0;

        long n = 0;//ulong

        while ((n >>> shift) == 0) {
            if (read_index < h.num_digits) {
                n = (10 * n) + h.digits[read_index++];
            } else if (n == 0) {
                return;
            } else {
                while ((n >>> shift) == 0) {
                    n = 10 * n;
                    read_index++;
                }
                break;
            }
        }
        h.decimal_point -= (read_index - 1);
        if (h.decimal_point < -DECIMAL_POINT_RANGE) { // it is zero
            h.num_digits = 0;
            h.decimal_point = 0;
            h.negative = false;
            h.truncated = false;
            return;
        }
        long mask = (1L << shift) - 1;
        while (read_index < h.num_digits) {
            byte new_digit = (byte) (n >>> shift);
            n = (10 * (n & mask)) + h.digits[read_index++];
            h.digits[write_index++] = new_digit;
        }
        while (n != 0) {
            byte new_digit = (byte) (n >>> shift);
            n = 10 * (n & mask);
            if (write_index < MAX_DIGITS) {
                h.digits[write_index++] = new_digit;
            } else if (new_digit > 0) {
                h.truncated = true;
            }
        }
        h.num_digits = write_index;
        trim(h);
    }

    static int number_of_digits_decimal_left_shift(Decimal h, int shift) {
        shift &= 63;
        int x_a = 0xffff & number_of_digits_decimal_left_shift_table[shift];//uint
        int x_b = 0xffff & number_of_digits_decimal_left_shift_table[shift + 1];//uint
        int num_new_digits = x_a >>> 11;//uint
        int pow5_a = 0x7FF & x_a;//uint8
        int pow5_b = 0x7FF & x_b;//uint8
        int i = 0;//uint
        int n = pow5_b - pow5_a;//uint
        for (; i < n; i++) {
            if (i >= h.num_digits) {
                return num_new_digits - 1;
            } else if (h.digits[i] == (0xffff & NUMBER_OF_DIGITS_DECIMAL_LEFT_SHIFT_TABLE_POWERS_OF_5[pow5_a + i])) {
                continue;
            } else if (h.digits[i] < (0xffff & NUMBER_OF_DIGITS_DECIMAL_LEFT_SHIFT_TABLE_POWERS_OF_5[pow5_a + i])) {
                return num_new_digits - 1;
            } else {
                return num_new_digits;
            }
        }
        return num_new_digits;

    }

    static long round(Decimal h) {
        if ((h.num_digits == 0) || (h.decimal_point < 0)) {
            return 0;
        } else if (h.decimal_point > 18) {
            return UINT64_MAX;
        }
        // at this point, we know that h.decimal_point >= 0
        int dp = Math.abs(h.decimal_point);
        long n = 0;//uint64
        for (int i = 0; i < dp; i++) {
            n = (10 * n) + ((i < h.num_digits) ? h.digits[i] : 0);
        }
        boolean round_up = false;
        if (dp < h.num_digits) {
            round_up = h.digits[dp] >= 5; // normally, we round up
            // but we may need to round to even!
            if ((h.digits[dp] == 5) && (dp + 1 == h.num_digits)) {
                round_up = h.truncated || ((dp > 0) && ((1 & h.digits[dp - 1]) != 0));
            }
        }
        if (round_up) {
            n++;
        }
        return n;
    }

    // remove all final zeroes
    static void trim(Decimal h) {
        while ((h.num_digits > 0) && (h.digits[h.num_digits - 1] == 0)) {
            h.num_digits--;
        }
    }

    public double doubleValue() {
        AdjustedMantissa am = compute_float(this);
        return am.toDouble(this.negative, am);
    }

    static class AdjustedMantissa {
        private static final int MANTISSA_EXPLICIT_BITS = 52;
        private static final int SIGN_INDEX = 63;
        /**
         * ulong
         */
        long mantissa;
        /**
         * a negative value indicates an invalid result.
         */
        int power2;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AdjustedMantissa that = (AdjustedMantissa) o;
            return mantissa == that.mantissa && power2 == that.power2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mantissa, power2);
        }

        double toDouble(boolean negative, AdjustedMantissa am) {
            long word = am.mantissa;//ulong
            word |= ((long) am.power2) << MANTISSA_EXPLICIT_BITS;
            word = negative
                    ? word | (1L << SIGN_INDEX) : word;
            return Double.longBitsToDouble(word);
        }
    }
}

