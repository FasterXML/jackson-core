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
 * Tests class {@link FastDoubleParser}
 */
public class FastFloatParserTest extends AbstractFastXParserTest {
    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleCharSequence() {
        return createAllTestData().stream()
                .filter(t -> t.charLength() == t.input().length()
                        && t.charOffset() == 0)
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> FastFloatParser.parseFloat(u.input()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleCharSequenceIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> FastFloatParser.parseFloat(u.input(), u.charOffset(), u.charLength()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleCharArray() {
        return createAllTestData().stream()
                .filter(t -> t.charLength() == t.input().length()
                        && t.charOffset() == 0)
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> FastFloatParser.parseFloat(u.input().toCharArray()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleCharArrayIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> FastFloatParser.parseFloat(u.input().toCharArray(), u.charOffset(), u.charLength()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleBitsCharSequenceIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> testBits(t, u -> FastFloatParser.parseFloatBits(u.input(), u.charOffset(), u.charLength()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleBitsCharArrayIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> testBits(t, u -> FastFloatParser.parseFloatBits(u.input().toCharArray(), u.charOffset(), u.charLength()))));
    }

    private void test(TestData d, ToFloatFunction<TestData> f) {
        byte[] bytes = d.input().getBytes(StandardCharsets.UTF_8);
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

    private void testBits(TestData d, ToLongFunction<TestData> f) {
        byte[] bytes = d.input().getBytes(StandardCharsets.UTF_8);
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
