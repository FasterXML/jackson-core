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

public abstract class AbstractJsonFloatValueParserTest extends AbstractFloatValueParserTest {

    protected List<TestData> createDataForBadStrings() {
        return Arrays.asList(
                new TestData("empty", ""),
                new TestData("00"),
                new TestData("000000000"),
                new TestData("0000.0000"),
                new TestData("+"),
                new TestData("+1"),
                new TestData("+1.2"),
                new TestData("+1.2e3"),
                new TestData("+1.2E3"),
                new TestData("+1.2e3"),
                new TestData("-"),
                new TestData("+e"),
                new TestData("-e"),
                new TestData("+e123"),
                new TestData("+1.2E+3"),

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
                new TestData("0xp89"),
                new TestData("FloatTypeSuffix", "1d"),
                new TestData("FloatTypeSuffix", "1.2d"),
                new TestData("FloatTypeSuffix", "1.2e-3d"),
                new TestData("FloatTypeSuffix", "1.2E-3d"),
                new TestData("FloatTypeSuffix", "1.2e-3d"),

                new TestData("FloatTypeSuffix", "1D"),
                new TestData("FloatTypeSuffix", "1.2D"),
                new TestData("FloatTypeSuffix", "1.2e-3D"),
                new TestData("FloatTypeSuffix", "1.2E-3D"),
                new TestData("FloatTypeSuffix", "1.2e-3D"),
                new TestData("FloatTypeSuffix", "1f"),
                new TestData("FloatTypeSuffix", "1.2f"),
                new TestData("FloatTypeSuffix", "1.2e-3f"),
                new TestData("FloatTypeSuffix", "1.2E-3f"),
                new TestData("FloatTypeSuffix", "1.2e-3f"),
                new TestData("FloatTypeSuffix", "1F"),
                new TestData("FloatTypeSuffix", "1.2F"),
                new TestData("FloatTypeSuffix", "1.2e-3F"),
                new TestData("FloatTypeSuffix", "1.2E-3F"),
                new TestData("FloatTypeSuffix", "1.2e-3F"),

                new TestData("  1.2e3"),
                new TestData("  -1.2e3"),
                new TestData(" 1.2e3"),
                new TestData("1.2e3 "),
                new TestData("1.2e3  "),
                new TestData("   1.2e3   ")
        );
    }

    protected List<TestData> createDataForLegalDecStrings() {
        return Arrays.asList(
                new TestData("0", 0, 0f),
                new TestData("1", 1, 1f),
                new TestData("1.2", 1.2, 1.2f),
                new TestData("1.2e3", 1.2e3, 1.2e3f),
                new TestData("1.2E3", 1.2e3, 1.2e3f),
                new TestData("1.2e3", 1.2e3, 1.2e3f),
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

                new TestData("1", 1, 1f),
                new TestData("1.2", 1.2, 1.2f),
                new TestData("1.2e+3", 1.2e3, 1.2e3f),
                new TestData("1.2E+3", 1.2e3, 1.2e3f),
                new TestData("1.2e+3", 1.2e3, 1.2e3f),
                new TestData("-1.2e+3", -1.2e3, -1.2e3f),
                new TestData("-1.2E-3", -1.2e-3, -1.2e-3f),
                new TestData("1234567890", 1234567890d, 1234567890f)
        );
    }


    List<TestData> createDataForLegalCroppedStrings() {
        return Arrays.asList(
                new TestData("x1y", 1, 1f, 1, 1)
        );
    }

    protected List<TestData> createTestDataForInputClassesInMethodParseNumber() {
        return Arrays.asList(
                new TestData("parseNumber(): charOffset too small", "3.14", -1, 4, -1, 4, 3d, 3f, false),
                new TestData("parseNumber(): charOffset too big", "3.14", 8, 4, 8, 4, 3d, 3f, false),
                new TestData("parseNumber(): charLength too small", "3.14", 0, -4, 0, -4, 3d, 3f, false),
                new TestData("parseNumber(): charLength too big", "3.14", 0, 8, 0, 8, 3d, 3f, false),
                new TestData("parseNumber(): Significand with leading whitespace", "   3", 0, 4, 0, 4, 3d, 3f, false),
                new TestData("parseNumber(): Significand with trailing whitespace", "3   ", 0, 4, 0, 4, 3d, 3f, false),
                new TestData("parseNumber(): Empty String", "", 0, 0, 0, 0, 0d, 0f, false),
                new TestData("parseNumber(): Blank String", "   ", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseNumber(): Very long non-blank String", Strings.repeat("a",66), 0, 66, 0, 66, 0d, 0f, false),
                new TestData("parseNumber(): Plus Sign", "+", 0, 1, 0, 1, 0d, 0f, false),
                new TestData("parseNumber(): Negative Sign", "-", 0, 1, 0, 1, 0d, 0f, false),
                new TestData("parseNumber(): Infinity", "Infinity", 0, 8, 0, 8, Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, false),
                new TestData("parseNumber(): NaN", "NaN", 0, 3, 0, 3, Double.NaN, Float.NaN, false),
                new TestData("parseNumber(): Leading zero", "03", 0, 2, 0, 2, 3d, 3f, true),
                new TestData("parseNumber(): Leading zeroes", "003", 0, 3, 0, 3, 3d, 3f, false),
                new TestData("parseNumber(): Leading zero x", "0x3", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseNumber(): Leading zero X", "0X3", 0, 3, 0, 3, 0d, 0f, false),

                new TestData("parseNumber(): Decimal point only", ".", 0, 1, 0, 1, 0d, 0f, false),
                new TestData("parseNumber(): With decimal point", "3.", 0, 2, 0, 2, 3d, 3f, true),
                new TestData("parseNumber(): Without decimal point", "3", 0, 1, 0, 1, 3d, 3f, true),
                new TestData("parseNumber(): 7 digits after decimal point", "3.1234567", 0, 9, 0, 9, 3.1234567, 3.1234567f, true),
                new TestData("parseNumber(): 8 digits after decimal point", "3.12345678", 0, 10, 0, 10, 3.12345678, 3.12345678f, true),
                new TestData("parseNumber(): 9 digits after decimal point", "3.123456789", 0, 11, 0, 11, 3.123456789, 3.123456789f, true),
                new TestData("parseNumber(): 1 digit + 7 chars after decimal point", "3.1abcdefg", 0, 10, 0, 10, 0d, 0f, false),
                new TestData("parseNumber(): With 'e' at end", "3e", 0, 2, 0, 2, 0d, 0f, false),
                new TestData("parseNumber(): With 'E' at end", "3E", 0, 2, 0, 2, 0d, 0f, false),
                new TestData("parseNumber(): With 'e' + whitespace at end", "3e   ", 0, 5, 0, 5, 0d, 0f, false),
                new TestData("parseNumber(): With 'E' + whitespace  at end", "3E   ", 0, 5, 0, 5, 0d, 0f, false),
                new TestData("parseNumber(): With 'e+' at end", "3e+", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseNumber(): With 'E-' at end", "3E-", 0, 3, 0, 3, 0d, 0f, false),
                new TestData("parseNumber(): With 'e+9' at end", "3e+9", 0, 4, 0, 4, 3e+9, 3e+9f, true),
                new TestData("parseNumber(): With 20 significand digits", "12345678901234567890", 0, 20, 0, 20, 12345678901234567890d, 12345678901234567890f, true),
                new TestData("parseNumber(): With 20 significand digits + non-ascii char", "12345678901234567890￡", 0, 21, 0, 21, 0d, 0f, false),
                new TestData("parseNumber(): With 20 significand digits with decimal point", "1234567890.1234567890", 0, 21, 0, 21, 1234567890.1234567890, 1234567890.1234567890f, true),
                new TestData("parseNumber(): With illegal FloatTypeSuffix 'z': 1.2e3z", "1.2e3z", 0, 6, 0, 6, 1.2e3, 1.2e3f, false),
                new TestData("parseNumber(): With FloatTypeSuffix 'd': 1.2e3d", "1.2e3d", 0, 6, 0, 6, 1.2e3, 1.2e3f, false),
                new TestData("parseNumber(): With FloatTypeSuffix 'd' + whitespace: 1.2e3d ", "1.2e3d ", 0, 7, 0, 7, 1.2e3, 1.2e3f, false),
                new TestData("parseNumber(): With FloatTypeSuffix 'D': 1.2D", "1.2D", 0, 4, 0, 4, 1.2, 1.2f, false),
                new TestData("parseNumber(): With FloatTypeSuffix 'f': 1f", "1f", 0, 2, 0, 2, 1d, 1f, false),
                new TestData("parseNumber(): With FloatTypeSuffix 'F': -1.2e-3F", "-1.2e-3F", 0, 8, 0, 8, -1.2e-3, -1.2e-3f, false),
                new TestData("parseNumber(): No digits+whitespace+'z'", ". z", 0, 2, 0, 2, 0d, 0f, false)

        );
    }

    List<TestData> createAllTestData() {
        List<TestData> list = new ArrayList<>();
        /*
        list.addAll(createDataForDecimalLimits());
        list.addAll(createDataForBadStrings());
        list.addAll(createDataForLegalDecStrings());
        list.addAll(createDataForDecimalClingerInputClasses());
        list.addAll(createDataForLegalCroppedStrings());
         */
        list.addAll(createTestDataForInputClassesInMethodParseNumber());
        return list;
    }

}