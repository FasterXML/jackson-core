package com.fasterxml.jackson.core.io;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This implementation stores the {@link BigDecimal} as text until it is needed. It can be expensive
 * to parse a number and in some cases, the number will never be retrieved.
 *
 * @since 2.15
 */
public class LazyBigDecimal implements LazyNumber {
    private final String _value;
    private final boolean _useFastParser;
    private BigDecimal _decimal;

    public LazyBigDecimal(final String value, final boolean useFastParser) {
        this._value = value;
        this._useFastParser = useFastParser;
    }

    public LazyBigDecimal(final BigDecimal bigDecimal) {
        this._decimal = bigDecimal;
        this._value = bigDecimal.toString();
        this._useFastParser = false;
    }

    @Override
    public Number getNumber() {
        if (_decimal == null) {
            _decimal = NumberInput.parseBigDecimal(_value, _useFastParser);;
        }
        return _decimal;
    }

    @Override
    public String getText() {
        return _value;
    }
}
