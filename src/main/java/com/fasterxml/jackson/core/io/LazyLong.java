package com.fasterxml.jackson.core.io;

/**
 * This implementation stores the {@link Long} as text until it is needed. It can be expensive
 * to parse a number and in some cases, the number will never be retrieved.
 *
 * @since 2.15
 */
public class LazyLong implements LazyNumber {
    private String _value;
    private final Long _long;

    public LazyLong(final Long l) {
        this._long = l;
    }

    @Override
    public Number getNumber() {
        return _long;
    }

    @Override
    public String getText() {
        if (_value == null) {
            _value = Long.toString(_long);
        }
        return _value;
    }
}
