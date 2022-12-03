package tools.jackson.core.io;

import ch.randelshofer.fastdoubleparser.JavaBigIntegerParser;

import java.math.BigInteger;

import static tools.jackson.core.io.BigDecimalParser.MAX_CHARS_TO_REPORT;

/**
 * Helper class used to implement more optimized parsing of {@link BigInteger} for REALLY
 * big values (over 500 characters).
 */
public final class BigIntegerParser
{
    private BigIntegerParser() {}

    public static BigInteger parseWithFastParser(final String valueStr) {
        try {
            return JavaBigIntegerParser.parseBigInteger(valueStr);
        } catch (NumberFormatException nfe) {
            final String reportNum = valueStr.length() <= MAX_CHARS_TO_REPORT ?
                    valueStr : valueStr.substring(0, MAX_CHARS_TO_REPORT) + " [truncated]";
            throw new NumberFormatException("Value \"" + reportNum
                    + "\" can not be represented as `java.math.BigDecimal`, reason: " + nfe.getMessage());
        }
    }
}
