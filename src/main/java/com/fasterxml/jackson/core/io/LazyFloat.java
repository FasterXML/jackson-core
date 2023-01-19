package com.fasterxml.jackson.core.io;

/**
 * This implementation stores the {@link Float} as text until it is needed. It can be expensive
 * to parse a number and in some cases, the number will never be retrieved.
 *
 * @since 2.15
 */
public class LazyFloat implements LazyNumber {
    private String _value;
    private final boolean _useFastParser;
    private Float _float;

    public LazyFloat(final String value, final boolean useFastParser) {
        this._value = value;
        this._useFastParser = useFastParser;
    }

    public LazyFloat(final Float f) {
        this._float = f;
        this._useFastParser = false;
    }

    @Override
    public Number getNumber() {
        if (_float == null) {
            _float = NumberInput.parseFloat(_value, _useFastParser);
        }
        return _float;
    }

    @Override
    public String getText() {
        if (_value == null) {
            _value = Float.toString(_float);
        }
        return _value;
    }
}
