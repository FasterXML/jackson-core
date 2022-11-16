/*
 * @(#)TestData.java
 * Copyright © 2022. Werner Randelshofer, Switzerland. MIT License.
 */

package com.fasterxml.jackson.core.io.doubleparser;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Supplier;

public final class BigIntegerTestData {
    private final String title;
    private final CharSequence input;
    private final int charOffset;
    private final int charLength;
    private final int byteOffset;
    private final int byteLength;
    private final Supplier<BigInteger> expectedValue;

    public BigIntegerTestData(String title,
                              CharSequence input,
                              int charOffset, int charLength,
                              int byteOffset, int byteLength,
                              Supplier<BigInteger> expectedValue
    ) {
        this.title = title;
        this.input = input;
        this.charOffset = charOffset;
        this.charLength = charLength;
        this.byteOffset = byteOffset;
        this.byteLength = byteLength;
        this.expectedValue = expectedValue;
    }

    public BigIntegerTestData(CharSequence input, long expectedLongValue) {
        this(input.toString(), input, 0, input.length(), 0, input.length(),
                () -> BigInteger.valueOf(expectedLongValue)
        );
    }

    public BigIntegerTestData(CharSequence input, BigInteger expectedValue) {
        this(input.toString(), input, 0, input.length(), 0, input.length(),
                () -> expectedValue
        );
    }

    public BigIntegerTestData(CharSequence input, long expectedLongValue, int offset, int length) {
        this(input.toString(), input, offset, length, offset, length,
                () -> BigInteger.valueOf(expectedLongValue)
        );
    }

    public BigIntegerTestData(String title, CharSequence input, long expectedLongValue) {
        this(title.contains(input) ? title : title + " " + input, input, 0, input.length(), 0, input.length(),
                () -> BigInteger.valueOf(expectedLongValue)
        );
    }

    public BigIntegerTestData(String title, CharSequence input, BigInteger expectedValue) {
        this(title.contains(input) || title.length() + (long) input.length() > 100
                        ? title
                        : title + " " + input,
                input, 0, input.length(), 0, input.length(),
                () -> expectedValue
        );
    }

    public BigIntegerTestData(String title, CharSequence input, Supplier<BigInteger> expectedValue) {
        this(title.contains(input) || title.length() + (long) input.length() > 100
                        ? title
                        : title + " " + input,
                input, 0, input.length(), 0, input.length(),
                expectedValue
        );
    }

    public BigIntegerTestData(CharSequence input) {
        this(input.toString(), input);
    }

    public BigIntegerTestData(String title, CharSequence input) {
        this(title.contains(input) || title.length() + (long) input.length() > 100
                        ? title
                        : title + " " + input,
                input, 0, input.length(), 0, input.length(),
                () -> null
        );
    }

    public String title() {
        return title;
    }

    public CharSequence input() {
        return input;
    }

    public int charOffset() {
        return charOffset;
    }

    public int charLength() {
        return charLength;
    }

    public int byteOffset() {
        return byteOffset;
    }

    public int byteLength() {
        return byteLength;
    }

    public Supplier<BigInteger> expectedValue() {
        return expectedValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        BigIntegerTestData that = (BigIntegerTestData) obj;
        return Objects.equals(this.title, that.title) &&
                Objects.equals(this.input, that.input) &&
                this.charOffset == that.charOffset &&
                this.charLength == that.charLength &&
                this.byteOffset == that.byteOffset &&
                this.byteLength == that.byteLength &&
                Objects.equals(this.expectedValue, that.expectedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, input, charOffset, charLength, byteOffset, byteLength, expectedValue);
    }

    @Override
    public String toString() {
        return "BigIntegerTestData[" +
                "title=" + title + ", " +
                "input=" + input + ", " +
                "charOffset=" + charOffset + ", " +
                "charLength=" + charLength + ", " +
                "byteOffset=" + byteOffset + ", " +
                "byteLength=" + byteLength + ", " +
                "expectedValue=" + expectedValue + ']';
    }

}
