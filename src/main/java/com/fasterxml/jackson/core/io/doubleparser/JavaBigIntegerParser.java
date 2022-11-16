package com.fasterxml.jackson.core.io.doubleparser;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * Provides static method for parsing a {@link BigInteger} from a
 * {@link CharSequence}.
 * <p>
 * <b>Syntax</b>
 * <p>
 * Formal specification of the grammar:
 * <blockquote>
 * <dl>
 * <dt><i>BigIntegerLiteral:</i></dt>
 * <dd><i>[Sign] Digits</i></dd>
 * <dd><i>[Sign]</i> {@code 0x} <i>[HexDigits]</i>
 * <dd><i>[Sign]</i> {@code 0X} <i>[HexDigits]</i>
 * </dl>
 * <dl>
 * <dt><i>Digits:</i>
 * <dd><i>Digit {Digit}</i>
 * </dl>
 * <dl>
 * <dt><i>HexDigits:</i>
 * <dd><i>HexDigit {HexDigit}</i>
 * </dl>
 * <dl>
 * <dt><i>Digit:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code 0 1 2 3 4 5 6 7 8 9}
 * </dl>
 * <dl>
 * <dt><i>HexDigit:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code 0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F}
 * </dl>
 * <p>
 */
public class JavaBigIntegerParser {
    public static BigInteger parseBigIntegerOrNull(String str) {
        return parseBigIntegerOrNull(str.getBytes(StandardCharsets.ISO_8859_1));
    }

    public static BigInteger parseBigIntegerOrNull(byte[] str) {
        return parseBigIntegerOrNull(str, 0, str.length);
    }

    public static BigInteger parseBigIntegerOrNull(byte[] str, int offset, int length) {
        try {
            return new JavaBigIntegerFromByteArray().parseBigIntegerLiteral(str, offset, length);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
