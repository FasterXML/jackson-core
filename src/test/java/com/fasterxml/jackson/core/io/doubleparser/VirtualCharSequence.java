/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

public class VirtualCharSequence implements CharSequence {
    private final int length;
    private final char fill;
    private final char[] prefix;
    private final char[] suffix;
    private final char[] middle;
    private final int prefixTo;
    private final int middleFrom;
    private final int middleTo;
    private final int suffixFrom;

    public VirtualCharSequence(String prefix, int middleFrom, String middle, String suffix, char fill, int length) {
        this.length = length;
        this.prefix = prefix.toCharArray();
        this.middle = middle.toCharArray();
        this.suffix = suffix.toCharArray();
        this.prefixTo = this.prefix.length;
        this.middleFrom = middleFrom;
        this.middleTo = middleFrom + this.middle.length;
        this.suffixFrom = length - this.suffix.length;
        this.fill = fill;
    }

    public VirtualCharSequence(char fill, int length) {
        this("", 0, "", "", fill, length);
    }

    public VirtualCharSequence(String prefix, char fill, int length) {
        this(prefix, 0, "", "", fill, length);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        if (index < prefixTo) {
            return prefix[index];
        } else if (index < middleFrom) {
            return fill;
        } else if (index < middleTo) {
            return middle[index - middleFrom];
        } else if (index < suffixFrom) {
            return fill;
        } else {
            return suffix[index - suffixFrom];
        }
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new VirtualCharSequence(
                start < prefixTo ? new String(prefix, start, Math.min(end, prefix.length)) : "",
                Math.min(end, middleFrom),
                end < middleFrom ? new String(middle, Math.max(0, start - middleFrom), Math.min(end, middleTo) - middleFrom) : "",
                end <= suffixFrom ? "" : new String(suffix, 0, suffixFrom - end),
                fill,
                end - start
        );
    }

    @Override
    public String toString() {
        if (length < Integer.MAX_VALUE - 4) {
            char[] chars = new char[length];
            for (int i = 0; i < length; i++) {
                chars[i] = charAt(i);
            }
            return new String(chars);
        }
        return "VirtualCharSequence{" +
                "length=" + length +
                ", fill=" + fill +
                ", prefix=" + new String(prefix) +
                ", suffix=" + new String(suffix) +
                ", middle=" + new String(middle) +
                ", prefixTo=" + prefixTo +
                ", middleFrom=" + middleFrom +
                ", middleTo=" + middleTo +
                ", suffixFrom=" + suffixFrom +
                '}';
    }
}
