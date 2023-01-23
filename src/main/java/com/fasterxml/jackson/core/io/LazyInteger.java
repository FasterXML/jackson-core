package com.fasterxml.jackson.core.io;

/**
 * This implementation stores the {@link Integer} as text until it is needed. It can be expensive
 * to parse a number and in some cases, the number will never be retrieved.
 *
 * @since 2.15
 */
public class LazyInteger implements LazyNumber {
    private String _value;
    private final Integer _int;

    public LazyInteger(final Integer i) {
        this._int = i;
    }

    @Override
    public Number getNumber() {
        return _int;
    }

    @Override
    public String getText() {
        if (_value == null) {
            _value = Integer.toString(_int);
        }
        return _value;
    }
}
