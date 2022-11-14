/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.core.io.doubleparser.Strings.repeat;

public abstract class AbstractBigIntegerParserTest {
    protected List<BigIntegerTestData> createDataForLegalHexStrings() {
        return Arrays.asList(
                new BigIntegerTestData("0x0", BigInteger.ZERO),
                new BigIntegerTestData("0x1", BigInteger.ONE),
                new BigIntegerTestData("0xa", BigInteger.TEN),

                new BigIntegerTestData("0x00", BigInteger.ZERO),
                new BigIntegerTestData("0x01", BigInteger.ONE),
                new BigIntegerTestData("0x00000000", BigInteger.ZERO),
                new BigIntegerTestData("0x00000001", BigInteger.ONE),

                new BigIntegerTestData("0x1", new BigInteger("1", 16)),
                new BigIntegerTestData("0x12", new BigInteger("12", 16)),
                new BigIntegerTestData("0x123", new BigInteger("123", 16)),
                new BigIntegerTestData("0x1234", new BigInteger("1234", 16)),
                new BigIntegerTestData("0x12345", new BigInteger("12345", 16)),
                new BigIntegerTestData("0x123456", new BigInteger("123456", 16)),
                new BigIntegerTestData("0x1234567", new BigInteger("1234567", 16)),
                new BigIntegerTestData("0x12345678", new BigInteger("12345678", 16)),

                new BigIntegerTestData("-0x0", BigInteger.ZERO.negate()),
                new BigIntegerTestData("-0x1", BigInteger.ONE.negate()),
                new BigIntegerTestData("-0xa", BigInteger.TEN.negate()),
                new BigIntegerTestData("0xff", new BigInteger("ff", 16)),
                new BigIntegerTestData("-0xff", new BigInteger("-ff", 16)),
                new BigIntegerTestData("-0x12345678", new BigInteger("-12345678", 16))
        );
    }

    protected List<BigIntegerTestData> createDataForLegalDecStrings() {
        return Arrays.asList(
                new BigIntegerTestData("0", BigInteger.ZERO),
                new BigIntegerTestData("1", BigInteger.ONE),
                new BigIntegerTestData("10", BigInteger.TEN),

                new BigIntegerTestData("00", BigInteger.ZERO),
                new BigIntegerTestData("01", BigInteger.ONE),
                new BigIntegerTestData("00000000", BigInteger.ZERO),
                new BigIntegerTestData("00000001", BigInteger.ONE),

                new BigIntegerTestData("1", new BigInteger("1", 10)),
                new BigIntegerTestData("12", new BigInteger("12", 10)),
                new BigIntegerTestData("123", new BigInteger("123", 10)),
                new BigIntegerTestData("1234", new BigInteger("1234", 10)),
                new BigIntegerTestData("12345", new BigInteger("12345", 10)),
                new BigIntegerTestData("123456", new BigInteger("123456", 10)),
                new BigIntegerTestData("1234567", new BigInteger("1234567", 10)),
                new BigIntegerTestData("12345678", new BigInteger("12345678", 10)),

                new BigIntegerTestData("123456789012345678901234567890",
                        new BigInteger("123456789012345678901234567890", 10)),

                new BigIntegerTestData("-0", BigInteger.ZERO.negate()),
                new BigIntegerTestData("-1", BigInteger.ONE.negate()),
                new BigIntegerTestData("-10", BigInteger.TEN.negate()),
                new BigIntegerTestData("255", new BigInteger("255", 10)),
                new BigIntegerTestData("-255", new BigInteger("-255", 10)),
                new BigIntegerTestData("-12345678", new BigInteger("-12345678", 10)),

                new BigIntegerTestData(repeat("9806543217", 1_000), new BigInteger(repeat("9806543217", 1_000), 10))
        );
    }


    List<BigIntegerTestData> createRegularTestData() {
        List<BigIntegerTestData> list = new ArrayList<>();
        list.addAll(createDataForLegalDecStrings());
        list.addAll(createDataForLegalHexStrings());
        return list;
    }

}
