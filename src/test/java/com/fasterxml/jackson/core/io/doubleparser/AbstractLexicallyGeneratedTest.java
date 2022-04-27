
/*
 * @(#)LexicallyGeneratedTest.java
 * Copyright © 2021. Werner Randelshofer, Switzerland. MIT License.
 */

package com.fasterxml.jackson.core.io.doubleparser;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * The purpose of this test factory is to discover new cases, where
 * {@link FastDoubleParser#parseDouble(CharSequence)} does not
 * produce the same result like {@link Double#parseDouble(String)}.
 * <p>
 * Unfortunately, the space of input values is huge, it includes
 * all character sequences from lengths in the range
 * [0,{@value Integer#MAX_VALUE}].
 * <p>
 * Each run of this method produces new test cases with
 * input generated by applying the syntax rules for a double
 * value. The generation uses a random number generator.
 */
abstract class AbstractLexicallyGeneratedTest {
    /**
     * Seed for random number generator.
     * Specify a literal number to obtain repeatable tests.
     * Specify System.nanoTime to explore the input space.
     * (Make sure to take a note of the seed value if
     * tests failed.)
     */
    public static final long SEED = 0;//System.nanoTime();

    @TestFactory
    Stream<DynamicTest> dynamicTestsRandomStringFrom10SyntaxRuleWithoutWhitespace() {
        Random rng = new Random(SEED);
        LexicalGenerator gen = new LexicalGenerator(false, false);
        return IntStream.range(1, 10_000).mapToObj(i -> {
                    String str = gen.produceRandomInputStringFromLexicalRuleWithoutWhitespace(10, rng);
                    return dynamicTest(i + ": " + str,
                            () -> testAgainstDoubleParseDouble(str));
                }
        );
    }

    @TestFactory
    Stream<DynamicTest> dynamicTestsRandomStringFrom1SyntaxRuleWithoutWhitespace() {
        Random rng = new Random(SEED);
        LexicalGenerator gen = new LexicalGenerator(false, false);
        return IntStream.range(1, 10_000).mapToObj(i -> {
                    String str = gen.produceRandomInputStringFromLexicalRuleWithoutWhitespace(1, rng);
                    return dynamicTest(i + ": " + str,
                            () -> testAgainstDoubleParseDouble(str));
                }
        );
    }

    @TestFactory
    Stream<DynamicTest> dynamicTestsRandomStringFrom40SyntaxRuleWithoutWhitespace() {
        Random rng = new Random(SEED);
        LexicalGenerator gen = new LexicalGenerator(false, false);
        return IntStream.range(1, 10_000).mapToObj(i -> {
                    String str = gen.produceRandomInputStringFromLexicalRuleWithoutWhitespace(40, rng);
                    return dynamicTest(i + ": " + str,
                            () -> testAgainstDoubleParseDouble(str));
                }
        );
    }

    @TestFactory
    Stream<DynamicTest> dynamicTestsRandomStringsOfIncreasingLengthWithWhitespace() {
        Random rng = new Random(SEED);
        LexicalGenerator gen = new LexicalGenerator(false, false);
        return IntStream.range(1, 100).mapToObj(i -> {
                    String str = gen.produceRandomInputStringFromLexicalRuleWithWhitespace(i, rng);
                    return dynamicTest(i + ": " + str,
                            () -> testAgainstDoubleParseDouble(str));
                }
        );
    }

    /**
     * Given an input String {@code str},
     * tests if {@link FastDoubleParser#parseDouble(CharSequence)}
     * produces the same result like {@link Double#parseDouble(String)}.
     *
     * @param str the given input string
     */
    private void testAgainstDoubleParseDouble(String str) {
        double expected = Double.parseDouble(str);
        double actual = parse(str);
        assertEquals(expected, actual, "str=" + str);
        assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(actual),
                "longBits of " + expected);
    }

    protected abstract double parse(String str);


}
