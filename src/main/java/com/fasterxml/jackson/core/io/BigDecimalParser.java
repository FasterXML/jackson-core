package com.fasterxml.jackson.core.io;

import java.math.BigDecimal;

// Based on a great idea of Eric Obermühlner to use a tree of smaller BigDecimals for parsing really big numbers
// with O(n^1.5) complexity instead of O(n^2) when using the constructor for a decimal representation from JDK 8/11:
// https://github.com/eobermuhlner/big-math/commit/7a5419aac8b2adba2aa700ccf00197f97b2ad89f
public final class BigDecimalParser {

    private final char[] chars;
    private final int off;
    private final int len;

    BigDecimalParser(char[] chars, int off, int len) {
        this.chars = chars;
        this.off = off;
        this.len = len;
    }

    BigDecimal parse() throws NumberFormatException {
        try {
            if (len < 500) {
                return new BigDecimal(chars, off, len);
            }

            int splitLen = len / 10;
            return parseBigDecimal(splitLen);

        } catch (NumberFormatException e) {
            String val = new String(chars, off, len);

            throw new NumberFormatException("Value \"" + val + "\" can not be represented as BigDecimal."
                    + " Reason: " + e.getMessage());
        }
    }

    private BigDecimal parseBigDecimal(int splitLen) {
        boolean numHasSign = false;
        boolean expHasSign = false;
        boolean neg = false;
        int numIdx = 0;
        int expIdx = -1;
        int dotIdx = -1;
        int scale = 0;

        for (int i = off; i < len; i++) {
            char c = chars[i];
            switch (c) {
            case '+':
                if (expIdx >= 0) {
                    if (expHasSign) {
                        throw new NumberFormatException("Multiple signs in exponent");
                    }
                    expHasSign = true;
                } else {
                    if (numHasSign) {
                        throw new NumberFormatException("Multiple signs in number");
                    }
                    numHasSign = true;
                    numIdx = i + 1;
                }
                break;
            case '-':
                if (expIdx >= 0) {
                    if (expHasSign) {
                        throw new NumberFormatException("Multiple signs in exponent");
                    }
                    expHasSign = true;
                } else {
                    if (numHasSign) {
                        throw new NumberFormatException("Multiple signs in number");
                    }
                    numHasSign = true;
                    neg = true;
                    numIdx = i + 1;
                }
                break;
            case 'e':
            case 'E':
                if (expIdx >= 0) {
                    throw new NumberFormatException("Multiple exponent markers");
                }
                expIdx = i;
                break;
            case '.':
                if (dotIdx >= 0) {
                    throw new NumberFormatException("Multiple decimal points");
                }
                dotIdx = i;
                break;
            default:
                if (dotIdx >= 0 && expIdx == -1) {
                    scale++;
                }
            }
        }

        int numEndIdx;
        int exp = 0;
        if (expIdx >= 0) {
            numEndIdx = expIdx;
            String expStr = new String(chars, expIdx + 1, len - expIdx - 1);
            exp = Integer.parseInt(expStr);
            scale = adjustScale(scale, exp);
        } else {
            numEndIdx = len;
        }

        BigDecimal res;

        if (dotIdx >= 0) {
            int leftLen = dotIdx - numIdx;
            BigDecimal left = toBigDecimalRec(numIdx, leftLen, exp, splitLen);

            int rightLen = numEndIdx - dotIdx - 1;
            BigDecimal right = toBigDecimalRec(dotIdx + 1, rightLen, exp - rightLen, splitLen);

            res = left.add(right);
        } else {
            res = toBigDecimalRec(numIdx, numEndIdx - numIdx, exp, splitLen);
        }

        if (scale != 0) {
            res = res.setScale(scale);
        }

        if (neg) {
            res = res.negate();
        }

        return res;
    }

    private int adjustScale(int scale, long exp) {
        long adjScale = scale - exp;
        if (adjScale > Integer.MAX_VALUE || adjScale < Integer.MIN_VALUE) {
            throw new NumberFormatException(
                    "Scale out of range: " + adjScale + " while adjusting scale " + scale + " to exponent " + exp);
        }

        return (int) adjScale;
    }

    private BigDecimal toBigDecimalRec(int off, int len, int scale, int splitLen) {
        if (len > splitLen) {
            int mid = len / 2;
            BigDecimal left = toBigDecimalRec(off, mid, scale + len - mid, splitLen);
            BigDecimal right = toBigDecimalRec(off + mid, len - mid, scale, splitLen);

            return left.add(right);
        }

        return len == 0 ? BigDecimal.ZERO : new BigDecimal(chars, off, len).movePointRight(scale);
    }
}
