package com.fasterxml.jackson.core.io;

import java.math.BigInteger;

/**
 * This implementation stores the {@link BigInteger} as text until it is needed. It can be expensive
 * to parse a number and in some cases, the number will never be retrieved.
 *
 * @since 2.15
 */
public class LazyBigInteger implements LazyNumber {
    private String _value;
    private final boolean _useFastParser;
    private BigInteger _integer;

    public LazyBigInteger(final String value, final boolean useFastParser) {
        this._value = value;
        this._useFastParser = useFastParser;
    }

    public LazyBigInteger(final BigInteger integer) {
        this._useFastParser = false;
        this._integer = integer;
        this._value = integer.toString();
    }

    @Override
    public Number getNumber() {
        if (_integer == null) {
            _integer = NumberInput.parseBigInteger(_value, _useFastParser);
        }
        return _integer;
    }

    @Override
    public String getText() {
        if (_value == null) {
            _value = getNumber().toString();
        }
        return _value;
    }
}
