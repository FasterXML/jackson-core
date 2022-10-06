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

import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Tests class {@link FastDoubleParser}
 */
public class FastDoubleParserTest extends AbstractFastXParserTest {
    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleCharSequence() {
        return createAllTestData().stream()
                .filter(t -> t.charLength() == t.input().length()
                        && t.charOffset() == 0)
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> FastDoubleParser.parseDouble(u.input()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleCharSequenceIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> FastDoubleParser.parseDouble(u.input(), u.charOffset(), u.charLength()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleCharArray() {
        return createAllTestData().stream()
                .filter(t -> t.charLength() == t.input().length()
                        && t.charOffset() == 0)
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> FastDoubleParser.parseDouble(u.input().toCharArray()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleCharArrayIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> test(t, u -> FastDoubleParser.parseDouble(u.input().toCharArray(), u.charOffset(), u.charLength()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleBitsCharSequenceIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> testBits(t, u -> FastDoubleParser.parseDoubleBits(u.input(), u.charOffset(), u.charLength()))));
    }

    @TestFactory
    Stream<DynamicNode> dynamicTestsParseDoubleBitsCharArrayIntInt() {
        return createAllTestData().stream()
                .map(t -> dynamicTest(t.title(),
                        () -> testBits(t, u -> FastDoubleParser.parseDoubleBits(u.input().toCharArray(), u.charOffset(), u.charLength()))));
    }

    private void test(TestData d, ToDoubleFunction<TestData> f) {
        if (!d.valid()) {
            try {
                assertEquals(-1L, f.applyAsDouble(d));
            } catch (Exception e) {
                //success
            }
        } else {
            double actual = f.applyAsDouble(d);
            assertEquals(d.expectedDoubleValue(), actual);
        }
    }

    private void testBits(TestData d, ToLongFunction<TestData> f) {
        if (!d.valid()) {
            assertEquals(-1L, f.applyAsLong(d));
        } else {
            assertEquals(d.expectedDoubleValue(), Double.longBitsToDouble(f.applyAsLong(d)));
        }
    }

}
