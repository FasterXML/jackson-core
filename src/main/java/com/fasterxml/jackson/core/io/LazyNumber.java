package com.fasterxml.jackson.core.io;

/**
 * This implementation stores the number as text until it is needed. It can be expensive
 * to parse a number and in some cases, the number will never be retrieved.
 *
 * @since 2.15
 */
public interface LazyNumber {
    Number getNumber();
    String getText();
}
