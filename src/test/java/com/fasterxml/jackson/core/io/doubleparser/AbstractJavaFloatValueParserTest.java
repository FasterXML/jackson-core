/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractJavaFloatValueParserTest extends AbstractFloatValueParserTest {
    protected List<TestData> createTestDataForNan() {
        return Arrays.asList(
                new TestData("NaN", Double.NaN, Float.NaN),
                new TestData("+NaN", Double.NaN, Float.NaN),
                new TestData("-NaN", Double.NaN, Float.NaN),
                new TestData("NaNf"),
                new TestData("+NaNd"),
                new TestData("-NaNF"),
                new TestData("+-NaND"),
                new TestData("NaNInfinity"),
                new TestData("nan")
        );
    }

    protected List<TestData> createTestDataForInfinity() {
        return Arrays.asList(
                new TestData("Infinity", Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                new TestData("+Infinity", Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                new TestData("-Infinity", Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY),
                new TestData("Infinit"),
                new TestData("+Infinityf"),
                new TestData("-InfinityF"),
                new TestData("+Infinityd"),
                new TestData("+-InfinityD"),
                new TestData("+InfinityNaN"),
                new TestData("infinity")
        );
    }

    protected List<TestData> createDataForBadStrings() {
        return Arrays.asList(
                new TestData("empty", ""),
                new TestData("+"),
                new TestData("-"),
                new TestData("+e"),
                new TestData("-e"),
                new TestData("+e123"),
                new TestData("-e456"),
                new TestData("78 e9"),
                new TestData("-01 e23"),
                new TestData("- 1"),
                new TestData("-0 .5"),
                new TestData("-0. 5"),
                new TestData("-0.5 e"),
                new TestData("-0.5e 3"),
                new TestData("45\ne6"),
                new TestData("d"),
                new TestData(".f"),
                new TestData("7_8e90"),
                new TestData("12e3_4"),
                new TestData("00x5.6p7"),
                new TestData("89p0"),
                new TestData("cafebabe.1p2"),
                new TestData("0x123pa"),
                new TestData("0x1.2e7"),
                new TestData("0xp89")
        );
    }

    protected List<TestData> createDataForLegalDecStrings() {
        return Arrays.asList(
                new TestData("0", 0, 0f),
                new TestData("00", 0, 0f),
                new TestData("1", 1, 1f),
                new TestData("1.2", 1.2, 1.2f),
                new TestData("1.2e3", 1.2e3, 1.2e3f),
                new TestData("1.2E3", 1.2e3, 1.2e3f),
                new TestData("1.2e3", 1.2e3, 1.2e3f),
                new TestData("+1", 1, 1f),
                new TestData("+1.2", 1.2, 1.2f),
                new TestData("+1.2e3", 1.2e3, 1.2e3f),
                new TestData("+1.2E3", 1.2e3, 1.2e3f),
                new TestData("+1.2e3", 1.2e3, 1.2e3f),
                new TestData("-1", -1, -1f),
                new TestData("-1.2", -1.2, -1.2f),
                new TestData("-1.2e3", -1.2e3, -1.2e3f),
                new TestData("-1.2E3", -1.2e3, -1.2e3f),
                new TestData("-1.2e3", -1.2e3, -1.2e3f),
                new TestData("1", 1, 1f),
                new TestData("1.2", 1.2, 1.2f),
                new TestData("1.2e-3", 1.2e-3, 1.2e-3f),
                new TestData("1.2E-3", 1.2e-3, 1.2e-3f),
                new TestData("1.2e-3", 1.2e-3, 1.2e-3f),

                new TestData("FloatTypeSuffix", "1d", 1, 1f),
                new TestData("FloatTypeSuffix", "1.2d", 1.2, 1.2f),
                new TestData("FloatTypeSuffix", "1.2e-3d", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1.2E-3d", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1.2e-3d", 1.2e-3, 1.2e-3f),

                new TestData("FloatTypeSuffix", "1D", 1, 1f),
                new TestData("FloatTypeSuffix", "1.2D", 1.2, 1.2f),
                new TestData("FloatTypeSuffix", "1.2e-3D", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1.2E-3D", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1.2e-3D", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1f", 1, 1f),
                new TestData("FloatTypeSuffix", "1.2f", 1.2, 1.2f),
                new TestData("FloatTypeSuffix", "1.2e-3f", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1.2E-3f", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1.2e-3f", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1F", 1, 1f),
                new TestData("FloatTypeSuffix", "1.2F", 1.2, 1.2f),
                new TestData("FloatTypeSuffix", "1.2e-3F", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1.2E-3F", 1.2e-3, 1.2e-3f),
                new TestData("FloatTypeSuffix", "1.2e-3F", 1.2e-3, 1.2e-3f),

                new TestData("1", 1, 1f),
                new TestData("1.2", 1.2, 1.2f),
                new TestData("1.2e+3", 1.2e3, 1.2e3f),
                new TestData("1.2E+3", 1.2e3, 1.2e3f),
                new TestData("1.2e+3", 1.2e3, 1.2e3f),
                new TestData("-1.2e+3", -1.2e3, -1.2e3f),
                new TestData("-1.2E-3", -1.2e-3, -1.2e-3f),
                new TestData("+1.2E+3", 1.2e3, 1.2e3f),
                new TestData(" 1.2e3", 1.2e3, 1.2e3f),
                new TestData("1.2e3 ", 1.2e3, 1.2e3f),
                new TestData("  1.2e3", 1.2e3, 1.2e3f),
                new TestData("  -1.2e3", -1.2e3, -1.2e3f),
                new TestData("1.2e3  ", 1.2e3, 1.2e3f),
                new TestData("   1.2e3   ", 1.2e3, 1.2e3f),
                new TestData("1234567890", 1234567890d, 1234567890f),
                new TestData("000000000", 0d, 0f),
                new TestData("0000.0000", 0d, 0f)
        );
    }

    List<TestData> createDataForLegalHexStrings() {
        return Arrays.asList(
                new TestData("0xap2", 0xap2, 0xap2f),

                new TestData("FloatTypeSuffix", "0xap2d", 0xap2, 0xap2f),
                new TestData("FloatTypeSuffix", "0xap2D", 0xap2, 0xap2f),
                new TestData("FloatTypeSuffix", "0xap2f", 0xap2, 0xap2f),
                new TestData("FloatTypeSuffix", "0xap2F", 0xap2, 0xap2f),

                new TestData(" 0xap2", 0xap2, 0xap2f),
                new TestData(" 0xap2  ", 0xap2, 0xap2f),
                new TestData("   0xap2   ", 0xap2, 0xap2f),

                new TestData("0x0.1234ab78p0", 0x0.1234ab78p0, 0x0.1234ab78p0f),
                new TestData("-0x0.1234AB78p+7", -0x0.1234AB78p7, -0x0.1234ab78p7f),
                new TestData("0x1.0p8", 256d, 256f),
                new TestData("0x1.234567890abcdefP123", 0x1.234567890abcdefp123, 0x1.234567890abcdefp123f),
                new TestData("+0x1234567890.abcdefp-45", 0x1234567890.abcdefp-45d, 0x1234567890.abcdefp-45f),
                new TestData("0x1234567890.abcdef12p-45", 0x1234567890.abcdef12p-45, 0x1234567890.abcdef12p-45f)

        );
    }

    protected List<TestData> createTestDataForInputClassesInMethodParseFloatValue() {
        return Arrays.asList(
                new TestData("parseFloatValue(): charOffset too small", "3.14", -1, 4, -1, 4, 3d, 3f, false),
                new TestData("parseFloatValue(): charOffset too big", "3.14", 8, 4, 8, 4, 3d, 3f, false),
                new TestData("parseFloatValue(): charLength too small", "3.14", 0, -4, 0, -4, 3d, 3f, false),
                new TestData("parseFloatValue(): charLength too big", "3.14", 0, 8, 0, 8, 3d, 3f, false),
                new TestData("parseFloatValue(): Significand with leading whitespace", "   3", 0, 4, 0, 4, 3d, 3f, true),
                new TestData("parseFloatValue(): Significand with trailing whitespace", "3   ", 0, 4, 0, 4, 3d, 3f, true),
                new TestData("parseFloatValue(): Empty String", "", 0, 0, 0, 0, 0d, 0f, false),
                new TestData("parseFloatValue(): Blank String", "   ", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseFloatValue(): Very long non-blank String", Strings.repeat("a",66), 0, 66, 0, 66, 0d, 0f, false),
                new TestData("parseFloatValue(): Plus Sign", "+", 0, 1, 0, 1, 0d, 0f, false),
                new TestData("parseFloatValue(): Negative Sign", "-", 0, 1, 0, 1, 0d, 0f, false),
                new TestData("parseFloatValue(): Infinity", "Infinity", 0, 8, 0, 8, Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, true),
                new TestData("parseFloatValue(): NaN", "NaN", 0, 3, 0, 3, Double.NaN, Float.NaN, true),
                new TestData("parseInfinity(): Infinit (missing last char)", "Infinit", 0, 7, 0, 7, 0d, 0f, false),
                new TestData("parseInfinity(): InfinitY (bad last char)", "InfinitY", 0, 8, 0, 8, 0d, 0f, false),
                new TestData("parseNaN(): Na (missing last char)", "Na", 0, 2, 0, 2, 0d, 0f, false),
                new TestData("parseNaN(): Nan (bad last char)", "Nan", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseFloatValue(): Leading zero", "03", 0, 2, 0, 2, 3d, 3f, true),
                new TestData("parseFloatValue(): Leading zeroes", "003", 0, 3, 0, 3, 3d, 3f, true),
                new TestData("parseFloatValue(): Leading zero x", "0x3", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseFloatValue(): Leading zero X", "0X3", 0, 3, 0, 3, 0d, 0f, false),

                new TestData("parseDecFloatLiteral(): Decimal point only", ".", 0, 1, 0, 1, 0d, 0f, false),
                new TestData("parseDecFloatLiteral(): With decimal point", "3.", 0, 2, 0, 2, 3d, 3f, true),
                new TestData("parseDecFloatLiteral(): Without decimal point", "3", 0, 1, 0, 1, 3d, 3f, true),
                new TestData("parseDecFloatLiteral(): 7 digits after decimal point", "3.1234567", 0, 9, 0, 9, 3.1234567, 3.1234567f, true),
                new TestData("parseDecFloatLiteral(): 8 digits after decimal point", "3.12345678", 0, 10, 0, 10, 3.12345678, 3.12345678f, true),
                new TestData("parseDecFloatLiteral(): 9 digits after decimal point", "3.123456789", 0, 11, 0, 11, 3.123456789, 3.123456789f, true),
                new TestData("parseDecFloatLiteral(): 1 digit + 7 chars after decimal point", "3.1abcdefg", 0, 10, 0, 10, 0d, 0f, false),
                new TestData("parseDecFloatLiteral(): With 'e' at end", "3e", 0, 2, 0, 2, 0d, 0f, false),
                new TestData("parseDecFloatLiteral(): With 'E' at end", "3E", 0, 2, 0, 2, 0d, 0f, false),
                new TestData("parseDecFloatLiteral(): With 'e' + whitespace at end", "3e   ", 0, 5, 0, 5, 0d, 0f, false),
                new TestData("parseDecFloatLiteral(): With 'E' + whitespace  at end", "3E   ", 0, 5, 0, 5, 0d, 0f, false),
                new TestData("parseDecFloatLiteral(): With 'e+' at end", "3e+", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseDecFloatLiteral(): With 'E-' at end", "3E-", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseDecFloatLiteral(): With 'e+9' at end", "3e+9", 0, 4, 0, 4, 3e+9, 3e+9f, true),
                new TestData("parseDecFloatLiteral(): With 20 significand digits", "12345678901234567890", 0, 20, 0, 20, 12345678901234567890d, 12345678901234567890f, true),
                new TestData("parseDecFloatLiteral(): With 20 significand digits + non-ascii char", "12345678901234567890￡", 0, 21, 0, 21, 0d, 0f, false),
                new TestData("parseDecFloatLiteral(): With 20 significand digits with decimal point", "1234567890.1234567890", 0, 21, 0, 21, 1234567890.1234567890, 1234567890.1234567890f, true),
                new TestData("parseDecFloatLiteral(): With illegal FloatTypeSuffix 'z': 1.2e3z", "1.2e3z", 0, 6, 0, 6, 1.2e3, 1.2e3f, false),
                new TestData("parseDecFloatLiteral(): With FloatTypeSuffix 'd': 1.2e3d", "1.2e3d", 0, 6, 0, 6, 1.2e3, 1.2e3f, true),
                new TestData("parseDecFloatLiteral(): With FloatTypeSuffix 'd' + whitespace: 1.2e3d ", "1.2e3d ", 0, 7, 0, 7, 1.2e3, 1.2e3f, true),
                new TestData("parseDecFloatLiteral(): With FloatTypeSuffix 'D': 1.2D", "1.2D", 0, 4, 0, 4, 1.2, 1.2f, true),
                new TestData("parseDecFloatLiteral(): With FloatTypeSuffix 'f': 1f", "1f", 0, 2, 0, 2, 1d, 1f, true),
                new TestData("parseDecFloatLiteral(): With FloatTypeSuffix 'F': -1.2e-3F", "-1.2e-3F", 0, 8, 0, 8, -1.2e-3, -1.2e-3f, true),
                new TestData("parseDecFloatLiteral(): No digits+whitespace+'z'", ". z", 0, 2, 0, 2, 0d, 0f, false),

                new TestData("parseHexFloatLiteral(): With decimal point", "0x3.", 0, 4, 0, 4, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): No digits with decimal point", "0x.", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): Without decimal point", "0X3", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): 7 digits after decimal point", "0x3.1234567", 0, 11, 0, 11, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): 8 digits after decimal point", "0X3.12345678", 0, 12, 0, 12, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): 9 digits after decimal point", "0x3.123456789", 0, 13, 0, 13, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): 1 digit + 7 chars after decimal point", "0X3.1abcdefg", 0, 12, 0, 12, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): With 'p' at end", "0X3p", 0, 4, 0, 4, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): With 'P' at end", "0x3P", 0, 4, 0, 4, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): With 'p' + whitespace at end", "0X3p   ", 0, 7, 0, 7, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): With 'P' + whitespace  at end", "0x3P   ", 0, 7, 0, 7, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): With 'p+' at end", "0X3p+", 0, 5, 0, 5, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): With 'P-' at end", "0x3P-", 0, 5, 0, 5, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): With 'p+9' at end", "0X3p+9", 0, 6, 0, 6, 0X3p+9, 0X3p+9f, true),
                new TestData("parseHexFloatLiteral(): With 20 significand digits", "0x12345678901234567890p0", 0, 24, 0, 24, 0x12345678901234567890p0, 0x12345678901234567890p0f, true),
                new TestData("parseHexFloatLiteral(): With 20 significand digits + non-ascii char", "0x12345678901234567890￡p0", 0, 25, 0, 25, 0d, 0f, false),
                new TestData("parseHexFloatLiteral(): With 20 significand digits with decimal point", "0x1234567890.1234567890P0", 0, 25, 0, 25, 0x1234567890.1234567890P0, 0x1234567890.1234567890P0f, true),
                new TestData("parseHexFloatLiteral(): With illegal FloatTypeSuffix 'z': 0x1.2p3z", "0x1.2p3z", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, false),
                new TestData("parseHexFloatLiteral(): With FloatTypeSuffix 'd': 0x1.2p3d", "0x1.2p3d", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, true),
                new TestData("parseHexFloatLiteral(): With FloatTypeSuffix 'd' + whitespace: 0x1.2p3d ", "0x1.2p3d ", 0, 9, 0, 9, 0x1.2p3d, 0x1.2p3f, true),
                new TestData("parseHexFloatLiteral(): With FloatTypeSuffix 'D': 0x1.2p3D", "0x1.2p3D", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, true),
                new TestData("parseHexFloatLiteral(): With FloatTypeSuffix 'f': 0x1.2p3f", "0x1.2p3f", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, true),
                new TestData("parseHexFloatLiteral(): With FloatTypeSuffix 'F': 0x1.2p3F", "0x1.2p3F", 0, 8, 0, 8, 0x1.2p3d, 0x1.2p3f, true)
        );
    }

    protected List<TestData> createDataForLegalCroppedStrings() {
        return Arrays.asList(
                new TestData("x1y", 1, 1f, 1, 1),
                new TestData("xx-0x1p2yyy", -0x1p2, -0x1p2f, 2, 6)
        );
    }

    List<TestData> createAllTestData() {
        List<TestData> list = new ArrayList<>();
        list.addAll(createTestDataForInfinity());
        list.addAll(createTestDataForNan());
        list.addAll(createDataForDecimalLimits());
        list.addAll(createDataForHexadecimalLimits());
        list.addAll(createDataForBadStrings());
        list.addAll(createDataForLegalDecStrings());
        list.addAll(createDataForLegalHexStrings());
        list.addAll(createDataForDecimalClingerInputClasses());
        list.addAll(createDataForHexadecimalClingerInputClasses());
        list.addAll(createDataForLegalCroppedStrings());
        list.addAll(createTestDataForInputClassesInMethodParseFloatValue());
        list.addAll(createDataForSignificandDigitsInputClasses());
        return list;
    }

}