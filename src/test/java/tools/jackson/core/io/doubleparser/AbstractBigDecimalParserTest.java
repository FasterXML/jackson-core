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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static tools.jackson.core.io.doubleparser.Strings.repeat;

public abstract class AbstractBigDecimalParserTest {
    protected List<BigDecimalTestData> createDataForBadStrings() {
        return Arrays.asList(
                new BigDecimalTestData("NaN"),
                new BigDecimalTestData("+NaN"),
                new BigDecimalTestData("-NaN"),
                new BigDecimalTestData("NaNf"),
                new BigDecimalTestData("+NaNd"),
                new BigDecimalTestData("-NaNF"),
                new BigDecimalTestData("+-NaND"),
                new BigDecimalTestData("NaNInfinity"),
                new BigDecimalTestData("nan"),
                new BigDecimalTestData("Infinity"),
                new BigDecimalTestData("+Infinity"),
                new BigDecimalTestData("-Infinity"),
                new BigDecimalTestData("Infinit"),
                new BigDecimalTestData("+Infinityf"),
                new BigDecimalTestData("-InfinityF"),
                new BigDecimalTestData("+Infinityd"),
                new BigDecimalTestData("+-InfinityD"),
                new BigDecimalTestData("+InfinityNaN"),
                new BigDecimalTestData("infinity"),
                new BigDecimalTestData("empty", ""),
                new BigDecimalTestData("+"),
                new BigDecimalTestData("-"),
                new BigDecimalTestData("+e"),
                new BigDecimalTestData("-e"),
                new BigDecimalTestData("+e123"),
                new BigDecimalTestData("-e456"),
                new BigDecimalTestData("78 e9"),
                new BigDecimalTestData("-01 e23"),
                new BigDecimalTestData("- 1"),
                new BigDecimalTestData("-0 .5"),
                new BigDecimalTestData("-0. 5"),
                new BigDecimalTestData("-0.5 e"),
                new BigDecimalTestData("-0.5e 3"),
                new BigDecimalTestData("45\ne6"),
                new BigDecimalTestData("d"),
                new BigDecimalTestData(".f"),
                new BigDecimalTestData("7_8e90"),
                new BigDecimalTestData("12e3_4"),
                new BigDecimalTestData("00x5.6p7"),
                new BigDecimalTestData("89p0"),
                new BigDecimalTestData("cafebabe.1p2"),
                new BigDecimalTestData("0x123pa"),
                new BigDecimalTestData("0x1.2e7"),
                new BigDecimalTestData("0xp89"),
                new BigDecimalTestData("FloatTypeSuffix", "1d"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2d"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2e-3d"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2E-3d"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2e-3d"),

                new BigDecimalTestData(" 1.2e3"),
                new BigDecimalTestData("1.2e3 "),
                new BigDecimalTestData("  1.2e3"),
                new BigDecimalTestData("  -1.2e3"),
                new BigDecimalTestData("1.2e3  "),
                new BigDecimalTestData("   1.2e3   "),

                new BigDecimalTestData("FloatTypeSuffix", "1D"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2D"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2e-3D"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2E-3D"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2e-3D"),
                new BigDecimalTestData("FloatTypeSuffix", "1f"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2f"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2e-3f"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2E-3f"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2e-3f"),
                new BigDecimalTestData("FloatTypeSuffix", "1F"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2F"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2e-3F"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2E-3F"),
                new BigDecimalTestData("FloatTypeSuffix", "1.2e-3F")

        );
    }

    protected List<BigDecimalTestData> createDataForLegalDecStrings() {
        return Arrays.asList(
                new BigDecimalTestData("0", new BigDecimal("0")),
                new BigDecimalTestData("00", new BigDecimal("0")),
                new BigDecimalTestData("007", new BigDecimal("7")),
                new BigDecimalTestData("1", new BigDecimal("1")),
                new BigDecimalTestData("1.2", new BigDecimal("1.2")),
                new BigDecimalTestData("12.3", new BigDecimal("12.3")),
                new BigDecimalTestData("1.2e3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("1.2E3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("1.2e3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("+1", new BigDecimal("1")),
                new BigDecimalTestData("+1.2", new BigDecimal("1.2")),
                new BigDecimalTestData("+1.2e3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("+1.2E3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("+1.2e3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("-1", new BigDecimal("-1")),
                new BigDecimalTestData("-1.2", new BigDecimal("-1.2")),
                new BigDecimalTestData("-1.2e3", new BigDecimal("-1.2e3")),
                new BigDecimalTestData("-1.2E3", new BigDecimal("-1.2e3")),
                new BigDecimalTestData("-1.2e3", new BigDecimal("-1.2e3")),
                new BigDecimalTestData("1", new BigDecimal("1")),
                new BigDecimalTestData("1.2", new BigDecimal("1.2")),
                new BigDecimalTestData("1.2e-3", new BigDecimal("1.2e-3")),
                new BigDecimalTestData("1.2E-3", new BigDecimal("1.2e-3")),
                new BigDecimalTestData("1.2e-3", new BigDecimal("1.2e-3")),

                new BigDecimalTestData("1", new BigDecimal("1")),
                new BigDecimalTestData("1.2", new BigDecimal("1.2")),
                new BigDecimalTestData("1.2e+3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("1.2E+3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("1.2e+3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("-1.2e+3", new BigDecimal("-1.2e3")),
                new BigDecimalTestData("-1.2E-3", new BigDecimal("-1.2e-3")),
                new BigDecimalTestData("+1.2E+3", new BigDecimal("1.2e3")),
                new BigDecimalTestData("1234567890", new BigDecimal("1234567890")),
                new BigDecimalTestData("000000000", new BigDecimal("000000000")),
                new BigDecimalTestData("0000.0000", new BigDecimal("0000.0000")),

                new BigDecimalTestData("8." + (repeat("9", 19)) + "e68", new BigDecimal("8." + (repeat("9", 19)) + "e68")),
                new BigDecimalTestData("103203303403503603703803903.122232425262728292", new BigDecimal("103203303403503603703803903.122232425262728292")),
                new BigDecimalTestData("122232425262728292.103203303403503603703803903", new BigDecimal("122232425262728292.103203303403503603703803903")),
                new BigDecimalTestData("-103203303403503603703803903.122232425262728292e6789", new BigDecimal("-103203303403503603703803903.122232425262728292e6789")),
                new BigDecimalTestData("122232425262728292.103203303403503603703803903e-6789", new BigDecimal("122232425262728292.103203303403503603703803903e-6789")),
                new BigDecimalTestData("-122232425262728292.103203303403503603703803903e-6789", new BigDecimal("-122232425262728292.103203303403503603703803903e-6789"))
        );
    }

    protected List<BigDecimalTestData> createDataForLegalCroppedStrings() {
        return Arrays.asList(
                new BigDecimalTestData("x1y", 1, 1, 1),
                new BigDecimalTestData("xx-0x1p2yyy", -0x1p2, 2, 6)
        );
    }

    protected List<BigDecimalTestData> createDataForBigDecimalLimits() {
        return Arrays.asList(
                new BigDecimalTestData("BigDecimal Min Scale",
                        BIG_DECIMAL_MIN_SCALE.toString(), BIG_DECIMAL_MIN_SCALE),
                new BigDecimalTestData("BigDecimal Max Scale",
                        BIG_DECIMAL_MAX_SCALE.toString(), BIG_DECIMAL_MAX_SCALE)

        );
    }

    protected List<BigDecimalTestData> createDataForVeryLongStrings() {
        return Arrays.asList(
                /*
                // new BigDecimalTestData("BigDecimal Max Big Integer",
                //         MAX_BIG_INTEGER.toString(), new BigDecimal(MAX_BIG_INTEGER,0)),
                // new BigDecimalTestData("BigDecimal Min Big Integer",
                //         MAX_BIG_INTEGER.negate().toString(), new BigDecimal(MAX_BIG_INTEGER.negate(),0)),

                new BigDecimalTestData("'9876543210' ** 10", "9876543210".repeat(10)
                        , () -> new BigDecimal("9876543210".repeat(10))),
                new BigDecimalTestData("'9' ** 1_000", "9".repeat(1_000),
                        () -> new BigDecimal("1e1000").subtract(BigDecimal.ONE)),
                new BigDecimalTestData("'9' ** 1_292_782_620, BigInteger would overflow supported range", "9".repeat(1_292_782_620)),
                new BigDecimalTestData("'9' ** 536_870_920, BigDecimal would overflow supported range", "9".repeat(536_870_920)),
                //new BigDecimalTestData("'9' ** 536_870_919", "9".repeat(536_870_919),
                //        ()->null)//()->new BigDecimal("1e536870919").subtract(BigDecimal.ONE))
                new BigDecimalTestData("DIGIT ** 100_000_000", "9".repeat(100_000_000),
                        () -> new BigDecimal("1e100000000").subtract(BigDecimal.ONE)),
                new BigDecimalTestData("DIGIT ** 536_870_919", "9".repeat(536_870_919),
                        () -> new BigDecimal("1e536870919").subtract(BigDecimal.ONE))
                        
                 */
        );
    }

    protected List<BigDecimalTestData> createTestDataForInputClassesInMethodParseBigDecimalString() {
        return Arrays.asList(
                new BigDecimalTestData("parseBigDecimalString(): illegal empty string", ""),
                new BigDecimalTestData("parseBigDecimalString(): illegal sign", "§"),
                new BigDecimalTestData("parseBigDecimalString(): illegal only negative sign", "-"),
                new BigDecimalTestData("parseBigDecimalString(): illegal only positive sign", "+"),
                new BigDecimalTestData("parseBigDecimalString(): illegal only point", "."),
                new BigDecimalTestData("parseBigDecimalString(): integer significand", "1", true),
                new BigDecimalTestData("parseBigDecimalString(): fractional significand", "0.1", true),
                new BigDecimalTestData("parseBigDecimalString(): point before significand", ".1", true),
                new BigDecimalTestData("parseBigDecimalString(): point after significand", "1.", true),
                new BigDecimalTestData("parseBigDecimalString(): point before significand, 40 digits", ".1234567890123456789012345678901234567890", true),
                new BigDecimalTestData("parseBigDecimalString(): point after significand, 40 digits", "1234567890123456789012345678901234567890.", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 18 digits in integer part", "123456789012345678", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 18 digits in fraction part", ".123456789012345678", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 18 digits in integer and fraction part together", "1234567890.12345678", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 19 digits in integer part", "1234567890123456789", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 19 digits in fraction part", ".1234567890123456789", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 19 digits in integer and fraction part together", "1234567890.123456789", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 40 digits in integer part", "1234567890123456789012345678901234567890", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 40 digits in fraction part", ".1234567890123456789012345678901234567890", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 40 digits in integer and fraction part together", "1234567890.123456789012345678901234567890", true),
                new BigDecimalTestData("parseBigDecimalString(): illegal digit in significand with 18 digits in integer part", "123456789012345u78"),
                new BigDecimalTestData("parseBigDecimalString(): illegal digit in significand with 18 digits in fraction part", ".1234567890123u5678"),
                new BigDecimalTestData("parseBigDecimalString(): illegal digit in significand with 18 digits in integer and fraction part together", "123456789u.12345678"),
                new BigDecimalTestData("parseBigDecimalString(): illegal digit in significand with 19 digits in integer part", "12345678901234567u9"),
                new BigDecimalTestData("parseBigDecimalString(): illegal digit in significand with 19 digits in fraction part", ".12345678901234567u9"),
                new BigDecimalTestData("parseBigDecimalString(): illegal digit in significand with 19 digits in integer and fraction part together", "1234567890.12345u789"),
                new BigDecimalTestData("parseBigDecimalString(): illegal digit in significand with 40 digits in integer part", "1234567890123456789012345678901234567u9"),
                new BigDecimalTestData("parseBigDecimalString(): illegal digit in significand with 40 digits in fraction part", ".1234567890123456789012345678901234567u9"),
                new BigDecimalTestData("parseBigDecimalString(): illegal digit in significand with 40 digits in integer and fraction part together", "123456789012345678901234567890.12345u789"),
                new BigDecimalTestData("parseBigDecimalString(): illegal only exponent indicator e", "e"),
                new BigDecimalTestData("parseBigDecimalString(): illegal only exponent indicator E", "E"),
                new BigDecimalTestData("parseBigDecimalString(): illegal exponent without number", "1e"),
                new BigDecimalTestData("parseBigDecimalString(): illegal exponent without number +", "1e+"),
                new BigDecimalTestData("parseBigDecimalString(): illegal exponent without number -", "1e-"),
                new BigDecimalTestData("parseBigDecimalString(): illegal exponent without number §", "1e§"),
                new BigDecimalTestData("parseBigDecimalString(): illegal duplicate point", "1.2.3e4"),
                new BigDecimalTestData("parseBigDecimalString(): illegal duplicate sign", "--1.2e4"),
                new BigDecimalTestData("parseBigDecimalString(): illegal duplicate sign after point", "-1.-2e5"),
                new BigDecimalTestData("parseBigDecimalString(): illegal duplicate sign inside significand", "-1-2e5"),
                new BigDecimalTestData("parseBigDecimalString(): illegal duplicate sign inside exponent", "-12e5-6"),
                new BigDecimalTestData("parseBigDecimalString(): illegal duplicate  exponent", "-12e5e6"),
                new BigDecimalTestData("parseBigDecimalString(): significand with 18 digits in integer part and exponent", "123456789012345678e-887799", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 18 digits in fraction part and exponent", "-.123456789012345678e887799", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 18 digits in integer and fraction part together and exponent", "1234567890.12345678e-887799", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 19 digits in integer part and exponent", "-1234567890123456789e887799", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 19 digits in fraction part and exponent", "-.1234567890123456789e-887799", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 19 digits in integer and fraction part together and exponent", "-1234567890.123456789e887799", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 40 digits in integer part and exponent", "-1234567890123456789012345678901234567890e887799", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 40 digits in fraction part and exponent", "-.1234567890123456789012345678901234567890e-887799", true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 40 digits in integer and fraction part together and exponent", "-123456789012345678901234567890.1234567890e887799", true),
                new BigDecimalTestData("parseBigDecimalString(): significand too many digits (BigInteger would overflow supported range)", new VirtualCharSequence('1', 1_292_782_621 + 1)),

                new BigDecimalTestData("parseBigDecimalString(): significand with 127 integer digits (below recursion threshold)", new VirtualCharSequence('7', 127), true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 128 integer digits (above recursion threshold)", new VirtualCharSequence('7', 128), true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 127 fraction digits (below recursion threshold)", new VirtualCharSequence(".", '7', 128), true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 128 fraction digits (above recursion threshold)", new VirtualCharSequence(".", '7', 129), true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 1023 integer digits (below parallel threshold)", new VirtualCharSequence('7', 1023), true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 1024 integer digits (above parallel threshold)", new VirtualCharSequence('7', 1024), true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 2048 integer digits (twice above parallel threshold)", new VirtualCharSequence('7', 2048), true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 1023 fraction digits (below parallel threshold)", new VirtualCharSequence(".", '7', 1024), true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 1024 fraction digits (above parallel threshold)", new VirtualCharSequence(".", '7', 1025), true),
                new BigDecimalTestData("parseBigDecimalString(): significand with 2048 integer digits, 4096 fraction digits (above parallel threshold)", new VirtualCharSequence("", 2048, ".", "", '7', 2048 + 4096 + 1), true)
        );
    }


    private final static BigInteger MAX_BIG_INTEGER;

    static {
        byte[] bytes = new byte[1 << 26];
        Arrays.fill(bytes, (byte) -1);
        bytes[0] = (byte) 0x7f;
        MAX_BIG_INTEGER = new BigInteger(bytes);
    }

    private final static BigDecimal BIG_DECIMAL_MIN_SCALE = new BigDecimal(BigInteger.ONE, Integer.MIN_VALUE + 1);
    private final static BigDecimal BIG_DECIMAL_MAX_SCALE = new BigDecimal(BigInteger.ONE, Integer.MAX_VALUE);

    List<BigDecimalTestData> createRegularTestData() {
        List<BigDecimalTestData> list = new ArrayList<>();
        list.addAll(createDataForBigDecimalLimits());
        list.addAll(createDataForBadStrings());
        list.addAll(createDataForLegalDecStrings());
        list.addAll(createDataForLegalCroppedStrings());
        return list;
    }


}
