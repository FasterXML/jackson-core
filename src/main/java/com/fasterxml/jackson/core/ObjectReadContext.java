package com.fasterxml.jackson.core;

/**
 * Defines API for accessing configuration and state exposed by
 * higher level databind
 * functionality during read (token stream  to Object deserialization) process.
 * Access is mostly needed during construction of
 * {@link JsonParser} instances by {@link TokenStreamFactory}.
 *
 * @since 3.0
 */
public interface ObjectReadContext {
}
