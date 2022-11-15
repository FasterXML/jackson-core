/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.nio.charset.StandardCharsets;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Tests class {@link JavaDoubleParser}
 */
public class JavaFloatParserTest extends AbstractJavaFloatValueParserTest {
    @TestFactory
    public Stream<DynamicNode> dynamicTestsParseDoubleCharSequence() {
        return createAllTestData().stream()
                .filter(t -> t.charLength() == t.input().length()
                        && t.charOffset() == 0)
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> JavaFloatParser.parseFloat(u.input()))));
    }

    @TestFactory
    public Stream<DynamicNode> dynamicTestsParseDoubleCharSequenceIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> JavaFloatParser.parseFloat(u.input(), u.charOffset(), u.charLength()))));
    }

    @TestFactory
    public Stream<DynamicNode> dynamicTestsParseDoubleCharArray() {
        return createAllTestData().stream()
                .filter(t -> t.charLength() == t.input().length()
                        && t.charOffset() == 0)
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> JavaFloatParser.parseFloat(u.input().toString().toCharArray()))));
    }

    @TestFactory
    public Stream<DynamicNode> dynamicTestsParseDoubleCharArrayIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> JavaFloatParser.parseFloat(u.input().toString().toCharArray(), u.charOffset(), u.charLength()))));
    }

    @TestFactory
    public Stream<DynamicNode> dynamicTestsParseDoubleBitsCharSequenceIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> testBits(t, u -> JavaFloatParser.parseFloatBits(u.input(), u.charOffset(), u.charLength()))));
    }

    @TestFactory
    public Stream<DynamicNode> dynamicTestsParseDoubleBitsCharArrayIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> testBits(t, u -> JavaFloatParser.parseFloatBits(u.input().toString().toCharArray(), u.charOffset(), u.charLength()))));
    }

    private void test(FloatTestData d, ToFloatFunction<FloatTestData> f) {
        byte[] bytes = d.input().toString().getBytes(StandardCharsets.UTF_8);
        if (!d.valid()) {
            try {
                assertEquals(-1L, f.applyAsFloat(d));
            } catch (Exception e) {
                //success
            }
        } else {
            double actual = f.applyAsFloat(d);
            assertEquals(d.expectedFloatValue(), actual);
        }
    }

    private void testBits(FloatTestData d, ToLongFunction<FloatTestData> f) {
        if (!d.valid()) {
            assertEquals(-1L, f.applyAsLong(d));
        } else {
            assertEquals(d.expectedFloatValue(), Float.intBitsToFloat((int) f.applyAsLong(d)));
        }
    }

    @FunctionalInterface
    public interface ToFloatFunction<T> {

        float applyAsFloat(T value);
    }
}
