package com.fasterxml.jackson.core.util;

import java.lang.reflect.Array;

/**
 * ArraysCompat implements {@link java.util.Arrays} methods which are not available in Android 2.2 (FroYo).
 */
public class ArraysCompat {
    /**
     * Copies {@code newLength} elements from {@code original} into a new array.
     * If {@code newLength} is greater than {@code original.length}, the result is padded
     * with the value {@code '\\u0000'}.
     *
     * @param original the original array
     * @param newLength the length of the new array
     * @return the new array
     * @throws NegativeArraySizeException if {@code newLength < 0}
     * @throws NullPointerException if {@code original == null}
     */
    public static char[] copyOf(char[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    /**
     * Copies {@code newLength} elements from {@code original} into a new array.
     * If {@code newLength} is greater than {@code original.length}, the result is padded
     * with the value {@code 0}.
     *
     * @param original the original array
     * @param newLength the length of the new array
     * @return the new array
     * @throws NegativeArraySizeException if {@code newLength < 0}
     * @throws NullPointerException if {@code original == null}
     */
    public static int[] copyOf(int[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    /**
     * Copies {@code newLength} elements from {@code original} into a new array.
     * If {@code newLength} is greater than {@code original.length}, the result is padded
     * with the value {@code null}.
     *
     * @param original the original array
     * @param newLength the length of the new array
     * @return the new array
     * @throws NegativeArraySizeException if {@code newLength < 0}
     * @throws NullPointerException if {@code original == null}
     */
    public static <T> T[] copyOf(T[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    /**
     * Copies elements from {@code original} into a new array, from indexes start (inclusive) to
     * end (exclusive). The original order of elements is preserved.
     * If {@code end} is greater than {@code original.length}, the result is padded
     * with the value {@code '\\u0000'}.
     *
     * @param original the original array
     * @param start the start index, inclusive
     * @param end the end index, exclusive
     * @return the new array
     * @throws ArrayIndexOutOfBoundsException if {@code start < 0 || start > original.length}
     * @throws IllegalArgumentException if {@code start > end}
     * @throws NullPointerException if {@code original == null}
     */
    public static char[] copyOfRange(char[] original, int start, int end) {
        int originalLength = original.length;
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        char[] result = new char[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    /**
     * Copies elements from {@code original} into a new array, from indexes start (inclusive) to
     * end (exclusive). The original order of elements is preserved.
     * If {@code end} is greater than {@code original.length}, the result is padded
     * with the value {@code 0}.
     *
     * @param original the original array
     * @param start the start index, inclusive
     * @param end the end index, exclusive
     * @return the new array
     * @throws ArrayIndexOutOfBoundsException if {@code start < 0 || start > original.length}
     * @throws IllegalArgumentException if {@code start > end}
     * @throws NullPointerException if {@code original == null}
     */
    public static int[] copyOfRange(int[] original, int start, int end) {
        int originalLength = original.length;
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        int[] result = new int[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    /**
     * Copies elements from {@code original} into a new array, from indexes start (inclusive) to
     * end (exclusive). The original order of elements is preserved.
     * If {@code end} is greater than {@code original.length}, the result is padded
     * with the value {@code null}.
     *
     * @param original the original array
     * @param start the start index, inclusive
     * @param end the end index, exclusive
     * @return the new array
     * @throws ArrayIndexOutOfBoundsException if {@code start < 0 || start > original.length}
     * @throws IllegalArgumentException if {@code start > end}
     * @throws NullPointerException if {@code original == null}
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] copyOfRange(T[] original, int start, int end) {
        int originalLength = original.length;
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        T[] result = (T[]) Array.newInstance(original.getClass().getComponentType(), resultLength);
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
}
