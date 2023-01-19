package com.fasterxml.jackson.core.io;

/**
 * This implementation stores the {@link Double} as text until it is needed. It can be expensive
 * to parse a number and in some cases, the number will never be retrieved.
 *
 * @since 2.15
 */
public class LazyDouble implements LazyNumber {
    private String _value;
    private final boolean _useFastParser;
    private Double _double;

    public LazyDouble(final String value, final boolean useFastParser) {
        this._value = value;
        this._useFastParser = useFastParser;
    }

    public LazyDouble(final Double d) {
        this._double = d;
        this._useFastParser = false;
    }

    @Override
    public Number getNumber() {
        if (_double == null) {
            _double = NumberInput.parseDouble(_value, _useFastParser);
        }
        return _double;
    }

    @Override
    public String getText() {
        if (_value == null) {
            _value = Double.toString(_double);
        }
        return _value;
    }
}
