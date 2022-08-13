/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

/**
 * Provides parsers that parse a {@code FloatingPointLiteral} from a
 * {@link java.lang.CharSequence}, {@code char} array, or {@code byte} array
 * ({@code str});.
 * <p>
 * Leading and trailing whitespace characters in {@code str} are ignored.
 * Whitespace is removed as if by the {@link java.lang.String#trim()} method;
 * that is, characters in the range [U+0000,U+0020].
 * <p>
 * The rest of {@code str} should constitute a  {@code FloatingPointLiteral} as described
 * by the lexical syntax rules shown below:
 * <blockquote>
 * <dl>
 * <dt><i>FloatingPointLiteral:</i></dt>
 * <dd><i>[Sign]</i> {@code NaN}</dd>
 * <dd><i>[Sign]</i> {@code Infinity}</dd>
 * <dd><i>[Sign] DecimalFloatingPointLiteral</i></dd>
 * <dd><i>[Sign] HexFloatingPointLiteral</i></dd>
 * <dd><i>SignedInteger</i></dd>
 * </dl>
 *
 * <dl>
 * <dt><i>HexFloatingPointLiteral</i>:
 * <dd><i>HexSignificand BinaryExponent [FloatTypeSuffix]</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexSignificand:</i>
 * <dd><i>HexNumeral</i>
 * <dd><i>HexNumeral</i> {@code .}
 * <dd>{@code 0x} <i>[HexDigits]</i> {@code .} <i>HexDigits</i>
 * <dd>{@code 0X} <i>[HexDigits]</i> {@code .} <i>HexDigits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>BinaryExponent:</i>
 * <dd><i>BinaryExponentIndicator SignedInteger</i>
 * </dl>
 *
 * <dl>
 * <dt><i>BinaryExponentIndicator:</i>
 * <dd>{@code p}
 * <dd>{@code P}
 * </dl>
 *
 * <dl>
 * <dt><i>DecimalFloatingPointLiteral:</i>
 * <dd><i>DecSignificand [DecExponent] [FloatTypeSuffix]</i>
 * </dl>
 *
 * <dl>
 * <dt><i>DecSignificand:</i>
 * <dd><i>Digits {@code .} [Digits]</i>
 * <dd><i>{@code .} Digits</i>
 * <dd><i>Digits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>DecExponent:</i>
 * <dd><i>ExponentIndicator SignedInteger</i>
 * </dl>
 *
 * <dl>
 * <dt><i>ExponentIndicator:</i>
 * <dd><i>(one of)</i>
 * <dd><i>e E</i>
 * </dl>
 *
 * <dl>
 * <dt><i>SignedInteger:</i>
 * <dd><i>[Sign] Digits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>Sign:</i>
 * <dd><i>(one of)</i>
 * <dd><i>+ -</i>
 * </dl>
 *
 * <dl>
 * <dt><i>Digits:</i>
 * <dd><i>Digit {Digit}</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexNumeral:</i>
 * <dd>{@code 0} {@code x} <i>HexDigits</i>
 * <dd>{@code 0} {@code X} <i>HexDigits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexDigits:</i>
 * <dd><i>HexDigit {HexDigit}</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexDigit:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code 0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F}
 * </dl>
 *
 * <dl>
 * <dt><i>FloatTypeSuffix:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code f F d D}
 * </dl>
 * </blockquote>
 * <p>
 * References:
 * <dl>
 *     <dt>The Java® Language Specification, Java SE 18 Edition, Chapter 3. Lexical Structure, 3.10.2. Floating-Point Literals </dt>
 *     <dd><a href="https://docs.oracle.com/javase/specs/jls/se18/html/jls-3.html#jls-3.10.2">docs.oracle.com</a></dd>
 * </dl>
 */
package com.fasterxml.jackson.core.io.doubleparser;