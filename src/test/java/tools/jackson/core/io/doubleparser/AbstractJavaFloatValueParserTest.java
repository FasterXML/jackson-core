/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package tools.jackson.core.io.doubleparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static tools.jackson.core.io.doubleparser.Strings.repeat;

public abstract class AbstractJavaFloatValueParserTest extends AbstractFloatValueParserTest {
    protected List<FloatTestData> createFloatTestDataForNan() {
        return Arrays.asList(
                new FloatTestData("NaN", Double.NaN, Float.NaN),
                new FloatTestData("+NaN", Double.NaN, Float.NaN),
                new FloatTestData("-NaN", Double.NaN, Float.NaN),
                new FloatTestData("NaNf"),
                new FloatTestData("+NaNd"),
                new FloatTestData("-NaNF"),
                new FloatTestData("+-NaND"),
                new FloatTestData("NaNInfinity"),
                new FloatTestData("nan")
        );
    }

    protected List<FloatTestData> createFloatTestDataForInfinity() {
        return Arrays.asList(
                new FloatTestData("Infinity", Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                new FloatTestData("+Infinity", Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                new FloatTestData("-Infinity", Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY),
                new FloatTestData("Infinit"),
                new FloatTestData("+Infinityf"),
                new FloatTestData("-InfinityF"),
                new FloatTestData("+Infinityd"),
                new FloatTestData("+-InfinityD"),
                new FloatTestData("+InfinityNaN"),
                new FloatTestData("infinity")
        );
    }

    protected List<FloatTestData> createDataForBadStrings() {
        return Arrays.asList(
                new FloatTestData("empty", ""),
                new FloatTestData("+"),
                new FloatTestData("-"),
                new FloatTestData("+e"),
                new FloatTestData("-e"),
                new FloatTestData("+e123"),
                new FloatTestData("-e456"),
                new FloatTestData("78 e9"),
                new FloatTestData("-01 e23"),
                new FloatTestData("- 1"),
                new FloatTestData("-0 .5"),
                new FloatTestData("-0. 5"),
                new FloatTestData("-0.5 e"),
                new FloatTestData("-0.5e 3"),
                new FloatTestData("45\ne6"),
                new FloatTestData("d"),
                new FloatTestData(".f"),
                new FloatTestData("7_8e90"),
                new FloatTestData("12e3_4"),
                new FloatTestData("00x5.6p7"),
                new FloatTestData("89p0"),
                new FloatTestData("cafebabe.1p2"),
                new FloatTestData("0x123pa"),
                new FloatTestData("0x1.2e7"),
                new FloatTestData("0xp89")
        );
    }

    protected List<FloatTestData> createDataForLegalDecStrings() {
        return Arrays.asList(
                new FloatTestData("0", 0, 0f),
                new FloatTestData("00", 0, 0f),
                new FloatTestData("007", 7, 7f),
                new FloatTestData("1", 1, 1f),
                new FloatTestData("1.2", 1.2, 1.2f),
                new FloatTestData("1.2e3", 1.2e3, 1.2e3f),
                new FloatTestData("1.2E3", 1.2e3, 1.2e3f),
                new FloatTestData("1.2e3", 1.2e3, 1.2e3f),
                new FloatTestData("+1", 1, 1f),
                new FloatTestData("+1.2", 1.2, 1.2f),
                new FloatTestData("+1.2e3", 1.2e3, 1.2e3f),
                new FloatTestData("+1.2E3", 1.2e3, 1.2e3f),
                new FloatTestData("+1.2e3", 1.2e3, 1.2e3f),
                new FloatTestData("-1", -1, -1f),
                new FloatTestData("-1.2", -1.2, -1.2f),
                new FloatTestData("-1.2e3", -1.2e3, -1.2e3f),
                new FloatTestData("-1.2E3", -1.2e3, -1.2e3f),
                new FloatTestData("-1.2e3", -1.2e3, -1.2e3f),
                new FloatTestData("1", 1, 1f),
                new FloatTestData("1.2", 1.2, 1.2f),
                new FloatTestData("1.2e-3", 1.2e-3, 1.2e-3f),
                new FloatTestData("1.2E-3", 1.2e-3, 1.2e-3f),
                new FloatTestData("1.2e-3", 1.2e-3, 1.2e-3f),

                new FloatTestData("FloatTypeSuffix", "1d", 1, 1f),
                new FloatTestData("FloatTypeSuffix", "1.2d", 1.2, 1.2f),
                new FloatTestData("FloatTypeSuffix", "1.2e-3d", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1.2E-3d", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1.2e-3d", 1.2e-3, 1.2e-3f),

                new FloatTestData("FloatTypeSuffix", "1D", 1, 1f),
                new FloatTestData("FloatTypeSuffix", "1.2D", 1.2, 1.2f),
                new FloatTestData("FloatTypeSuffix", "1.2e-3D", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1.2E-3D", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1.2e-3D", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1f", 1, 1f),
                new FloatTestData("FloatTypeSuffix", "1.2f", 1.2, 1.2f),
                new FloatTestData("FloatTypeSuffix", "1.2e-3f", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1.2E-3f", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1.2e-3f", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1F", 1, 1f),
                new FloatTestData("FloatTypeSuffix", "1.2F", 1.2, 1.2f),
                new FloatTestData("FloatTypeSuffix", "1.2e-3F", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1.2E-3F", 1.2e-3, 1.2e-3f),
                new FloatTestData("FloatTypeSuffix", "1.2e-3F", 1.2e-3, 1.2e-3f),

                new FloatTestData("1", 1, 1f),
                new FloatTestData("1.2", 1.2, 1.2f),
                new FloatTestData("1.2e+3", 1.2e3, 1.2e3f),
                new FloatTestData("1.2E+3", 1.2e3, 1.2e3f),
                new FloatTestData("1.2e+3", 1.2e3, 1.2e3f),
                new FloatTestData("-1.2e+3", -1.2e3, -1.2e3f),
                new FloatTestData("-1.2E-3", -1.2e-3, -1.2e-3f),
                new FloatTestData("+1.2E+3", 1.2e3, 1.2e3f),
                new FloatTestData(" 1.2e3", 1.2e3, 1.2e3f),
                new FloatTestData("1.2e3 ", 1.2e3, 1.2e3f),
                new FloatTestData("  1.2e3", 1.2e3, 1.2e3f),
                new FloatTestData("  -1.2e3", -1.2e3, -1.2e3f),
                new FloatTestData("1.2e3  ", 1.2e3, 1.2e3f),
                new FloatTestData("   1.2e3   ", 1.2e3, 1.2e3f),
                new FloatTestData("1234567890", 1234567890d, 1234567890f),
                new FloatTestData("000000000", 0d, 0f),
                new FloatTestData("0000.0000", 0d, 0f)
        );
    }

    List<FloatTestData> createDataForLegalHexStrings() {
        return Arrays.asList(
                new FloatTestData("0xap2", 0xap2, 0xap2f),

                new FloatTestData("FloatTypeSuffix", "0xap2d", 0xap2, 0xap2f),
                new FloatTestData("FloatTypeSuffix", "0xap2D", 0xap2, 0xap2f),
                new FloatTestData("FloatTypeSuffix", "0xap2f", 0xap2, 0xap2f),
                new FloatTestData("FloatTypeSuffix", "0xap2F", 0xap2, 0xap2f),

                new FloatTestData(" 0xap2", 0xap2, 0xap2f),
                new FloatTestData(" 0xap2  ", 0xap2, 0xap2f),
                new FloatTestData("   0xap2   ", 0xap2, 0xap2f),

                new FloatTestData("0x0.1234ab78p0", 0x0.1234ab78p0, 0x0.1234ab78p0f),
                new FloatTestData("-0x0.1234AB78p+7", -0x0.1234AB78p7, -0x0.1234ab78p7f),
                new FloatTestData("0x1.0p8", 256d, 256f),
                new FloatTestData("0x1.234567890abcdefP123", 0x1.234567890abcdefp123, 0x1.234567890abcdefp123f),
                new FloatTestData("+0x1234567890.abcdefp-45", 0x1234567890.abcdefp-45d, 0x1234567890.abcdefp-45f),
                new FloatTestData("0x1234567890.abcdef12p-45", 0x1234567890.abcdef12p-45, 0x1234567890.abcdef12p-45f)

        );
    }

    protected List<FloatTestData> createFloatTestDataForInputClassesInMethodParseFloatValue() {
        return Arrays.asList(
                new FloatTestData("parseFloatValue(): charOffset too small", "3.14", -1, 4, -1, 4, 3d, 3f, false),
                new FloatTestData("parseFloatValue(): charOffset too big", "3.14", 8, 4, 8, 4, 3d, 3f, false),
                new FloatTestData("parseFloatValue(): charLength too small", "3.14", 0, -4, 0, -4, 3d, 3f, false),
                new FloatTestData("parseFloatValue(): charLength too big", "3.14", 0, 8, 0, 8, 3d, 3f, false),
                new FloatTestData("parseFloatValue(): Significand with leading whitespace", "   3", 0, 4, 0, 4, 3d, 3f, true),
                new FloatTestData("parseFloatValue(): Significand with trailing whitespace", "3   ", 0, 4, 0, 4, 3d, 3f, true),
                new FloatTestData("parseFloatValue(): Empty String", "", 0, 0, 0, 0, 0d, 0f, false),
                new FloatTestData("parseFloatValue(): Blank String", "   ", 0, 3, 0, 3, 0d, 0f, false),
                new FloatTestData("parseFloatValue(): Very long non-blank String", repeat("a", 66), 0, 66, 0, 66, 0d, 0f, false),
                new FloatTestData("parseFloatValue(): Plus Sign", "+", 0, 1, 0, 1, 0d, 0f, false),
                new FloatTestData("parseFloatValue(): Negative Sign", "-", 0, 1, 0, 1, 0d, 0f, false),
                new FloatTestData("parseFloatValue(): Infinity", "Infinity", 0, 8, 0, 8, Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, true),
                new FloatTestData("parseFloatValue(): NaN", "NaN", 0, 3, 0, 3, Double.NaN, Float.NaN, true),
                new FloatTestData("parseInfinity(): Infinit (missing last char)", "Infinit", 0, 7, 0, 7, 0d, 0f, false),
                new FloatTestData("parseInfinity(): InfinitY (bad last char)", "InfinitY", 0, 8, 0, 8, 0d, 0f, false),
                new FloatTestData("parseNaN(): Na (missing last char)", "Na", 0, 2, 0, 2, 0d, 0f, false),
                new FloatTestData("parseNaN(): Nan (bad last char)", "Nan", 0, 3, 0, 3, 0d, 0f, false),
                new FloatTestData("parseFloatValue(): Leading zero", "03", 0, 2, 0, 2, 3d, 3f, true),
                new FloatTestData("parseFloatValue(): Leading zeroes", "003", 0, 3, 0, 3, 3d, 3f, true),
                new FloatTestData("parseFloatValue(): Leading zero x", "0x3", 0, 3, 0, 3, 0d, 0f, false),
                new FloatTestData("parseFloatValue(): Leading zero X", "0X3", 0, 3, 0, 3, 0d, 0f, false),

                new FloatTestData("parseDecFloatLiteral(): Decimal point only", ".", 0, 1, 0, 1, 0d, 0f, false),
                new FloatTestData("parseDecFloatLiteral(): With decimal point", "3.", 0, 2, 0, 2, 3d, 3f, true),
                new FloatTestData("parseDecFloatLiteral(): Without decimal point", "3", 0, 1, 0, 1, 3d, 3f, true),
                new FloatTestData("parseDecFloatLiteral(): 7 digits after decimal point", "3.1234567", 0, 9, 0, 9, 3.1234567, 3.1234567f, true),
                new FloatTestData("parseDecFloatLiteral(): 8 digits after decimal point", "3.12345678", 0, 10, 0, 10, 3.12345678, 3.12345678f, true),
                new FloatTestData("parseDecFloatLiteral(): 9 digits after decimal point", "3.123456789", 0, 11, 0, 11, 3.123456789, 3.123456789f, true),
                new FloatTestData("parseDecFloatLiteral(): 1 digit + 7 chars after decimal point", "3.1abcdefg", 0, 10, 0, 10, 0d, 0f, false),
                new FloatTestData("parseDecFloatLiteral(): With 'e' at end", "3e", 0, 2, 0, 2, 0d, 0f, false),
                new FloatTestData("parseDecFloatLiteral(): With 'E' at end", "3E", 0, 2, 0, 2, 0d, 0f, false),
                new FloatTestData("parseDecFloatLiteral(): With 'e' + whitespace at end", "3e   ", 0, 5, 0, 5, 0d, 0f, false),
                new FloatTestData("parseDecFloatLiteral(): With 'E' + whitespace  at end", "3E   ", 0, 5, 0, 5, 0d, 0f, false),
                new FloatTestData("parseDecFloatLiteral(): With 'e+' at end", "3e+", 0, 3, 0, 3, 0d, 0f, false),
                new FloatTestData("parseDecFloatLiteral(): With 'E-' at end", "3E-", 0, 3, 0, 3, 0d, 0f, false),
                new FloatTestData("parseDecFloatLiteral(): With 'e+9' at end", "3e+9", 0, 4, 0, 4, 3e+9, 3e+9f, true),
                new FloatTestData("parseDecFloatLiteral(): With 20 significand digits", "12345678901234567890", 0, 20, 0, 20, 12345678901234567890d, 12345678901234567890f, true),
                new FloatTestData("parseDecFloatLiteral(): With 20 significand digits + non-ascii char", "12345678901234567890￡", 0, 21, 0, 21, 0d, 0f, false),
                new FloatTestData("parseDecFloatLiteral(): With 20 significand digits with decimal point", "1234567890.1234567890", 0, 21, 0, 21, 1234567890.1234567890, 1234567890.1234567890f, true),
                new FloatTestData("parseDecFloatLiteral(): With illegal FloatTypeSuffix 'z': 1.2e3z", "1.2e3z", 0, 6, 0, 6, 1.2e3, 1.2e3f, false),
                new FloatTestData("parseDecFloatLiteral(): With FloatTypeSuffix 'd': 1.2e3d", "1.2e3d", 0, 6, 0, 6, 1.2e3, 1.2e3f, true),
                new FloatTestData("parseDecFloatLiteral(): With FloatTypeSuffix 'd' + whitespace: 1.2e3d ", "1.2e3d ", 0, 7, 0, 7, 1.2e3, 1.2e3f, true),
                new FloatTestData("parseDecFloatLiteral(): With FloatTypeSuffix 'D': 1.2D", "1.2D", 0, 4, 0, 4, 1.2, 1.2f, true),
                new FloatTestData("parseDecFloatLiteral(): With FloatTypeSuffix 'f': 1f", "1f", 0, 2, 0, 2, 1d, 1f, true),
                new FloatTestData("parseDecFloatLiteral(): With FloatTypeSuffix 'F': -1.2e-3F", "-1.2e-3F", 0, 8, 0, 8, -1.2e-3, -1.2e-3f, true),
                new FloatTestData("parseDecFloatLiteral(): No digits+whitespace+'z'", ". z", 0, 2, 0, 2, 0d, 0f, false),

                new FloatTestData("parseHexFloatLiteral(): With decimal point", "0x3.", 0, 4, 0, 4, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): No digits with decimal point", "0x.", 0, 3, 0, 3, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): Without decimal point", "0X3", 0, 3, 0, 3, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): 7 digits after decimal point", "0x3.1234567", 0, 11, 0, 11, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): 8 digits after decimal point", "0X3.12345678", 0, 12, 0, 12, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): 9 digits after decimal point", "0x3.123456789", 0, 13, 0, 13, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): 1 digit + 7 chars after decimal point", "0X3.1abcdefg", 0, 12, 0, 12, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): With 'p' at end", "0X3p", 0, 4, 0, 4, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): With 'P' at end", "0x3P", 0, 4, 0, 4, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): With 'p' + whitespace at end", "0X3p   ", 0, 7, 0, 7, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): With 'P' + whitespace  at end", "0x3P   ", 0, 7, 0, 7, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): With 'p+' at end", "0X3p+", 0, 5, 0, 5, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): With 'P-' at end", "0x3P-", 0, 5, 0, 5, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): With 'p+9' at end", "0X3p+9", 0, 6, 0, 6, 0X3p+9, 0X3p+9f, true),
                new FloatTestData("parseHexFloatLiteral(): With 20 significand digits", "0x12345678901234567890p0", 0, 24, 0, 24, 0x12345678901234567890p0, 0x12345678901234567890p0f, true),
                new FloatTestData("parseHexFloatLiteral(): With 20 significand digits + non-ascii char", "0x12345678901234567890￡p0", 0, 25, 0, 25, 0d, 0f, false),
                new FloatTestData("parseHexFloatLiteral(): With 20 significand digits with decimal point", "0x1234567890.1234567890P0", 0, 25, 0, 25, 0x1234567890.1234567890P0, 0x1234567890.1234567890P0f, true),
                new FloatTestData("parseHexFloatLiteral(): With illegal FloatTypeSuffix 'z': 0x1.2p3z", "0x1.2p3z", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, false),
                new FloatTestData("parseHexFloatLiteral(): With FloatTypeSuffix 'd': 0x1.2p3d", "0x1.2p3d", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, true),
                new FloatTestData("parseHexFloatLiteral(): With FloatTypeSuffix 'd' + whitespace: 0x1.2p3d ", "0x1.2p3d ", 0, 9, 0, 9, 0x1.2p3d, 0x1.2p3f, true),
                new FloatTestData("parseHexFloatLiteral(): With FloatTypeSuffix 'D': 0x1.2p3D", "0x1.2p3D", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, true),
                new FloatTestData("parseHexFloatLiteral(): With FloatTypeSuffix 'f': 0x1.2p3f", "0x1.2p3f", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, true),
                new FloatTestData("parseHexFloatLiteral(): With FloatTypeSuffix 'F': 0x1.2p3F", "0x1.2p3F", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, true)
        );
    }

    protected List<FloatTestData> createDataForLegalCroppedStrings() {
        return Arrays.asList(
                new FloatTestData("x1y", 1, 1f, 1, 1),
                new FloatTestData("xx-0x1p2yyy", -0x1p2, -0x1p2f, 2, 6)
        );
    }

    protected List<FloatTestData> createDataForVeryLongStrings() {
        return Arrays.asList(
                // This should be Float.POSITIVE_INFINITY instead of -1f
                new FloatTestData("9 repeated MAX_VALUE", repeat("9", Integer.MAX_VALUE - 8), Double.POSITIVE_INFINITY, -1f)
        );
    }

    List<FloatTestData> createAllTestData() {
        List<FloatTestData> list = new ArrayList<>();
        list.addAll(createFloatTestDataForInfinity());
        list.addAll(createFloatTestDataForNan());
        list.addAll(createDataForDecimalLimits());
        list.addAll(createDataForHexadecimalLimits());
        list.addAll(createDataForBadStrings());
        list.addAll(createDataForLegalDecStrings());
        list.addAll(createDataForLegalHexStrings());
        list.addAll(createDataForDecimalClingerInputClasses());
        list.addAll(createDataForHexadecimalClingerInputClasses());
        list.addAll(createDataForLegalCroppedStrings());
        list.addAll(createFloatTestDataForInputClassesInMethodParseFloatValue());
        list.addAll(createDataForSignificandDigitsInputClasses());
        //list.addAll(createDataForVeryLongStrings());
        return list;
    }

}