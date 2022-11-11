/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import java.util.Objects;

public final class TestData {
    private final String title;
    private final String input;
    private final int charOffset;
    private final int charLength;
    private final int byteOffset;
    private final int byteLength;
    private final double expectedDoubleValue;
    private final float expectedFloatValue;
    private final boolean valid;

    public TestData(String title,
                    String input,
                    int charOffset, int charLength,
                    int byteOffset, int byteLength,
                    double expectedDoubleValue,
                    float expectedFloatValue,
                    boolean valid) {
        this.title = title;
        this.input = input;
        this.charOffset = charOffset;
        this.charLength = charLength;
        this.byteOffset = byteOffset;
        this.byteLength = byteLength;
        this.expectedDoubleValue = expectedDoubleValue;
        this.expectedFloatValue = expectedFloatValue;
        this.valid = valid;
    }

    public TestData(String input, double expectedDoubleValue, float expectedFloatValue) {
        this(input, input, 0, input.length(), 0, input.length(),
                expectedDoubleValue,
                expectedFloatValue, true);
    }

    public TestData(String input, double expectedDoubleValue, float expectedFloatValue, int offset, int length) {
        this(input, input, offset, length, offset, length,
                expectedDoubleValue,
                expectedFloatValue, true);
    }

    public TestData(String title, String input, double expectedDoubleValue, float expectedFloatValue) {
        this(title.contains(input) ? title : title + " " + input, input, 0, input.length(), 0, input.length(),
                expectedDoubleValue,
                expectedFloatValue, true);
    }

    public TestData(String input) {
        this(input, input);
    }

    public TestData(String title, String input) {
        this(title.contains(input) ? title : title + " " + input, input, 0, input.length(), 0, input.length(),
                Double.NaN,
                Float.NaN, false);
    }

    public String title() {
        return title;
    }

    public String input() {
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

    public double expectedDoubleValue() {
        return expectedDoubleValue;
    }

    public float expectedFloatValue() {
        return expectedFloatValue;
    }

    public boolean valid() {
        return valid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        TestData that = (TestData) obj;
        return Objects.equals(this.title, that.title) &&
                Objects.equals(this.input, that.input) &&
                this.charOffset == that.charOffset &&
                this.charLength == that.charLength &&
                this.byteOffset == that.byteOffset &&
                this.byteLength == that.byteLength &&
                Double.doubleToLongBits(this.expectedDoubleValue) == Double.doubleToLongBits(that.expectedDoubleValue) &&
                Float.floatToIntBits(this.expectedFloatValue) == Float.floatToIntBits(that.expectedFloatValue) &&
                this.valid == that.valid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, input, charOffset, charLength, byteOffset, byteLength, expectedDoubleValue, expectedFloatValue, valid);
    }

    @Override
    public String toString() {
        return "TestData[" +
                "title=" + title + ", " +
                "input=" + input + ", " +
                "charOffset=" + charOffset + ", " +
                "charLength=" + charLength + ", " +
                "byteOffset=" + byteOffset + ", " +
                "byteLength=" + byteLength + ", " +
                "expectedDoubleValue=" + expectedDoubleValue + ", " +
                "expectedFloatValue=" + expectedFloatValue + ", " +
                "valid=" + valid + ']';
    }

}
