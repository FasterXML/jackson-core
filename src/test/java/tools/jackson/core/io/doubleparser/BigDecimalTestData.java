/*
 * @(#)TestData.java
 * Copyright © 2022. Werner Randelshofer, Switzerland. MIT License.
 */

package tools.jackson.core.io.doubleparser;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Supplier;

public final class BigDecimalTestData {
    private final String title;
    private final CharSequence input;
    private final int charOffset;
    private final int charLength;
    private final int byteOffset;
    private final int byteLength;
    private final Supplier<BigDecimal> expectedValue;

    public BigDecimalTestData(String title,
                              CharSequence input,
                              int charOffset, int charLength,
                              int byteOffset, int byteLength,
                              Supplier<BigDecimal> expectedValue
    ) {
        this.title = title;
        this.input = input;
        this.charOffset = charOffset;
        this.charLength = charLength;
        this.byteOffset = byteOffset;
        this.byteLength = byteLength;
        this.expectedValue = expectedValue;
    }

    public BigDecimalTestData(CharSequence input, double expectedDoubleValue) {
        this(input.toString(), input, 0, input.length(), 0, input.length(),
                () -> BigDecimal.valueOf(expectedDoubleValue)
        );
    }

    public BigDecimalTestData(CharSequence input, BigDecimal expectedValue) {
        this(input.toString(), input, 0, input.length(), 0, input.length(),
                () -> expectedValue
        );
    }

    public BigDecimalTestData(CharSequence input, double expectedDoubleValue, int offset, int length) {
        this(input.toString(), input, offset, length, offset, length,
                () -> BigDecimal.valueOf(expectedDoubleValue)
        );
    }

    public BigDecimalTestData(String title, CharSequence input, double expectedDoubleValue) {
        this(title.contains(input) ? title : title + " " + input, input, 0, input.length(), 0, input.length(),
                () -> BigDecimal.valueOf(expectedDoubleValue)
        );
    }

    public BigDecimalTestData(String title, CharSequence input, BigDecimal expectedValue) {
        this(title.contains(input) || title.length() + (long) input.length() > 100
                        ? title
                        : title + " " + input,
                input, 0, input.length(), 0, input.length(),
                () -> expectedValue
        );
    }

    public BigDecimalTestData(String title, CharSequence input, boolean valid) {
        this(title.contains(input) || title.length() + (long) input.length() > 100
                        ? title
                        : title + " " + input,
                input, 0, input.length(), 0, input.length(),
                () -> valid ? new BigDecimal(input.toString()) : null
        );
    }

    public BigDecimalTestData(String title, CharSequence input, Supplier<BigDecimal> expectedValue) {
        this(title.contains(input) || title.length() + (long) input.length() > 100
                        ? title
                        : title + " " + input,
                input, 0, input.length(), 0, input.length(),
                expectedValue
        );
    }

    public BigDecimalTestData(CharSequence input) {
        this(input.toString(), input);
    }

    public BigDecimalTestData(String title, CharSequence input) {
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

    public Supplier<BigDecimal> expectedValue() {
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
        BigDecimalTestData that = (BigDecimalTestData) obj;
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
        return "BigDecimalTestData[" +
                "title=" + title + ", " +
                "input=" + input + ", " +
                "charOffset=" + charOffset + ", " +
                "charLength=" + charLength + ", " +
                "byteOffset=" + byteOffset + ", " +
                "byteLength=" + byteLength + ", " +
                "expectedValue=" + expectedValue + ']';
    }

}
