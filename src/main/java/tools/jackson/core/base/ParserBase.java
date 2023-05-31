package tools.jackson.core.base;

import java.io.IOException;
//import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.exc.WrappedIOException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.NumberInput;
import tools.jackson.core.util.ByteArrayBuilder;
import tools.jackson.core.util.TextBuffer;

/**
 * Intermediate base class used by many (but not all) Jackson {@link JsonParser}
 * implementations. Contains most common things that are independent
 * of actual underlying input source.
 */
public abstract class ParserBase extends ParserMinimalBase
{
    /*
    /**********************************************************************
    /* Generic I/O state
    /**********************************************************************
     */

    /**
     * I/O context for this reader. It handles buffer allocation
     * for the reader.
     */
    protected final IOContext _ioContext;

    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;

    /*
    /**********************************************************************
    /* Current input data
    /**********************************************************************
     */

    // Note: type of actual buffer depends on sub-class, can't include

    /**
     * Pointer to next available character in buffer
     */
    protected int _inputPtr;

    /**
     * Index of character after last available one in the buffer.
     */
    protected int _inputEnd;

    /*
    /**********************************************************************
    /* Current input location information
    /**********************************************************************
     */

    /**
     * Number of characters/bytes that were contained in previous blocks
     * (blocks that were already processed prior to the current buffer).
     */
    protected long _currInputProcessed;

    /**
     * Current row location of current point in input buffer, starting
     * from 1, if available.
     */
    protected int _currInputRow = 1;

    /**
     * Current index of the first character of the current row in input
     * buffer. Needed to calculate column position, if necessary; benefit
     * of not having column itself is that this only has to be updated
     * once per line.
     */
    protected int _currInputRowStart;

    /*
    /**********************************************************************
    /* Information about starting location of event
    /* Reader is pointing to; updated on-demand
    /**********************************************************************
     */

    // // // Location info at point when current token was started

    /**
     * Total number of bytes/characters read before start of current token.
     * For big (gigabyte-sized) sizes are possible, needs to be long,
     * unlike pointers and sizes related to in-memory buffers.
     */
    protected long _tokenInputTotal;

    /**
     * Input row on which current token starts, 1-based
     */
    protected int _tokenInputRow = 1;

    /**
     * Column on input row that current token starts; 0-based (although
     * in the end it'll be converted to 1-based)
     */
    protected int _tokenInputCol;

    /*
    /**********************************************************************
    /* Buffer(s) for local name(s) and text content
    /**********************************************************************
     */

    /**
     * Buffer that contains contents of String values, including
     * property names if necessary (name split across boundary,
     * contains escape sequence, or access needed to char array)
     */
    protected final TextBuffer _textBuffer;

    /**
     * ByteArrayBuilder is needed if 'getBinaryValue' is called. If so,
     * we better reuse it for remainder of content.
     */
    protected ByteArrayBuilder _byteArrayBuilder;

    /**
     * We will hold on to decoded binary data, for duration of
     * current event, so that multiple calls to
     * {@link #getBinaryValue} will not need to decode data more
     * than once.
     */
    protected byte[] _binaryValue;

    /*
    /**********************************************************************
    /* Numeric value state; multiple fields used for efficiency
    /**********************************************************************
     */

    /**
     * Bitfield that indicates which numeric representations
     * have been calculated for the current type
     */
    protected int _numTypesValid = NR_UNKNOWN;

    // First primitives

    protected int _numberInt;

    protected long _numberLong;

    protected float _numberFloat;

    protected double _numberDouble;

    // And then object types

    protected BigInteger _numberBigInt;

    protected BigDecimal _numberBigDecimal;

    /**
     * Textual number representation captured from input in cases lazy-parsing
     * is desired.
     */
    protected String _numberString;

    // And then other information about value itself

    /**
     * Flag that indicates whether numeric value has a negative
     * value. That is, whether its textual representation starts
     * with minus character.
     */
    protected boolean _numberNegative;

    /**
     * Length of integer part of the number, in characters
     */
    protected int _intLength;

    /**
     * Length of the fractional part (not including decimal
     * point or exponent), in characters.
     * Not used for  pure integer values.
     */
    protected int _fractLength;

    /**
     * Length of the exponent part of the number, if any, not
     * including 'e' marker or sign, just digits.
     * Not used for  pure integer values.
     */
    protected int _expLength;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected ParserBase(ObjectReadContext readCtxt,
            IOContext ctxt, int streamReadFeatures) {
        super(readCtxt, ctxt, streamReadFeatures);
        _ioContext = ctxt;
        _textBuffer = ctxt.constructReadConstrainedTextBuffer();
    }

    /*
    /**********************************************************************
    /* Overrides for Feature handling
    /**********************************************************************
     */

    /*
    @Override
    public JsonParser enable(StreamReadFeature f) {
        _streamReadFeatures |= f.getMask();
        if (f == StreamReadFeature.STRICT_DUPLICATE_DETECTION) { // enabling dup detection?
            if (_parsingContext.getDupDetector() == null) { // but only if disabled currently
                _parsingContext = _parsingContext.withDupDetector(DupDetector.rootDetector(this));
            }
        }
        return this;
    }

    @Override
    public JsonParser disable(StreamReadFeature f) {
        _streamReadFeatures &= ~f.getMask();
        if (f == StreamReadFeature.STRICT_DUPLICATE_DETECTION) {
            _parsingContext = _parsingContext.withDupDetector(null);
        }
        return this;
    }
    */

    /*
    /**********************************************************************
    /* JsonParser impl
    /**********************************************************************
     */

    @Override
    public void assignCurrentValue(Object v) {
        TokenStreamContext ctxt = streamReadContext();
        if (ctxt != null) {
            ctxt.assignCurrentValue(v);
        }
    }

    @Override
    public Object currentValue() {
        TokenStreamContext ctxt = streamReadContext();
        return (ctxt == null) ? null : ctxt.currentValue();
    }

    /**
     * Method that can be called to get the name associated with
     * the current event.
     */
    /*
    @Override public String currentName() {
        // [JACKSON-395]: start markers require information from parent
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            JsonReadContext parent = _parsingContext.getParent();
            if (parent != null) {
                return parent.currentName();
            }
        }
        return _parsingContext.currentName();
    }
    */

    @Override public void close() throws JacksonException {
        if (!_closed) {
            // 19-Jan-2018, tatu: as per [core#440] need to ensure no more data assumed available
            _inputPtr = Math.max(_inputPtr, _inputEnd);
            _closed = true;
            try {
                _closeInput();
            } catch (IOException e) {
                throw _wrapIOFailure(e);
            } finally {
                // as per [JACKSON-324], do in finally block
                // Also, internal buffer(s) can now be released as well
                _releaseBuffers();
            }
        }
    }

    @Override public boolean isClosed() { return _closed; }

//    public JsonLocation getTokenLocation()
//   public JsonLocation getCurrentLocation()

    /*
    /**********************************************************************
    /* Public API, access to token information, text and similar
    /**********************************************************************
     */

    @Override
    public boolean hasTextCharacters() {
        return false;
    }

    @SuppressWarnings("resource")
    @Override
    public byte[] getBinaryValue(Base64Variant variant) throws JacksonException
    {
        if (_binaryValue == null) {
            if (_currToken != JsonToken.VALUE_STRING) {
                _reportError("Current token (%s) not VALUE_EMBEDDED_OBJECT or VALUE_STRING, can not access as binary", _currToken);
            }
            ByteArrayBuilder builder = _getByteArrayBuilder();
            _decodeBase64(getText(), builder, variant);
            _binaryValue = builder.toByteArray();
        }
        return _binaryValue;
    }

    /*
    /**********************************************************************
    /* Public low-level accessors
    /**********************************************************************
     */

    public long getTokenCharacterOffset() { return _tokenInputTotal; }
    public int getTokenLineNr() { return _tokenInputRow; }
    public int getTokenColumnNr() {
        // note: value of -1 means "not available"; otherwise convert from 0-based to 1-based
        int col = _tokenInputCol;
        return (col < 0) ? col : (col + 1);
    }

    /*
    /**********************************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************************
     */

    protected abstract void _closeInput() throws IOException;

    /*
    /**********************************************************************
    /* Low-level reading, other
    /**********************************************************************
     */

    /**
     * Method called to release internal buffers owned by the base
     * reader. This may be called along with {@link #_closeInput} (for
     * example, when explicitly closing this reader instance), or
     * separately (if need be).
     */
    protected void _releaseBuffers() {
        _textBuffer.releaseBuffers();
    }

    /**
     * Method called when an EOF is encountered between tokens.
     * If so, it may be a legitimate EOF, but only iff there
     * is no open non-root context.
     */
    @Override
    protected void _handleEOF() throws JacksonException {
        TokenStreamContext parsingContext = streamReadContext();
        if ((parsingContext != null) && !parsingContext.inRoot()) {
            String marker = parsingContext.inArray() ? "Array" : "Object";
            _reportInvalidEOF(String.format(
                    ": expected close marker for %s (start marker at %s)",
                    marker,
                    parsingContext.startLocation(_contentReference())),
                    null);
        }
    }

    /**
     * @return If no exception is thrown, {@code -1} which is used as marked for "end-of-input"
     *
     * @throws StreamReadException If check on {@code _handleEOF()} fails; usually because
     *    the current context is not root context (missing end markers in content)
     */
    protected final int _eofAsNextChar() throws StreamReadException {
        _handleEOF();
        return -1;
    }

    /*
    /**********************************************************************
    /* Internal/package methods: shared/reusable builders
    /**********************************************************************
     */

    protected ByteArrayBuilder _getByteArrayBuilder()
    {
        if (_byteArrayBuilder == null) {
            _byteArrayBuilder = new ByteArrayBuilder();
        } else {
            _byteArrayBuilder.reset();
        }
        return _byteArrayBuilder;
    }

    /*
    /**********************************************************************
    /* Methods related to number handling
    /**********************************************************************
     */

    // // // Life-cycle of number-parsing

    protected final JsonToken reset(boolean negative, int intLen, int fractLen, int expLen)
        throws JacksonException
    {
        if (fractLen < 1 && expLen < 1) { // integer
            return resetInt(negative, intLen);
        }
        return resetFloat(negative, intLen, fractLen, expLen);
    }

    protected final JsonToken resetInt(boolean negative, int intLen)
        throws JacksonException
    {
        // May throw StreamConstraintsException:
        _streamReadConstraints.validateIntegerLength(intLen);
        _numberNegative = negative;
        _intLength = intLen;
        _fractLength = 0;
        _expLength = 0;
        _numTypesValid = NR_UNKNOWN; // to force decoding
        return JsonToken.VALUE_NUMBER_INT;
    }

    protected final JsonToken resetFloat(boolean negative, int intLen, int fractLen, int expLen)
        throws JacksonException
    {
        // May throw StreamConstraintsException:
        _streamReadConstraints.validateFPLength(intLen + fractLen + expLen);
        _numberNegative = negative;
        _intLength = intLen;
        _fractLength = fractLen;
        _expLength = expLen;
        _numTypesValid = NR_UNKNOWN; // to force decoding
        return JsonToken.VALUE_NUMBER_FLOAT;
    }

    protected final JsonToken resetAsNaN(String valueStr, double value)
    {
        _textBuffer.resetWithString(valueStr);
        _numberDouble = value;
        _numTypesValid = NR_DOUBLE;
        return JsonToken.VALUE_NUMBER_FLOAT;
    }

    @Override
    public boolean isNaN() {
        if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
            if ((_numTypesValid & NR_DOUBLE) != 0) {
                return !Double.isFinite(_getNumberDouble());
            }
        }
        return false;
    }

    /*
    /**********************************************************************
    /* Numeric accessors of public API
    /**********************************************************************
     */

    @Override
    public Number getNumberValue()
    {
        if (_numTypesValid == NR_UNKNOWN) {
            _parseNumericValue(NR_UNKNOWN); // will also check event type
        }
        // Separate types for int types
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((_numTypesValid & NR_INT) != 0) {
                return _numberInt;
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return _numberLong;
            }
            if ((_numTypesValid & NR_BIGINT) != 0) {
                return _getBigInteger();
            }
            _throwInternal();
        }

        // And then floating point types. But here optimal type
        // needs to be big decimal, to avoid losing any data?
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return _getBigDecimal();
        }
        if ((_numTypesValid & NR_FLOAT) != 0) {
            return _getNumberFloat();
        }
        if ((_numTypesValid & NR_DOUBLE) == 0) { // sanity check
            _throwInternal();
        }
        return _getNumberDouble();
    }

    // NOTE: mostly copied from above
    @Override
    public Number getNumberValueExact()
    {
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_UNKNOWN);
            }
            if ((_numTypesValid & NR_INT) != 0) {
                return _numberInt;
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return _numberLong;
            }
            if ((_numTypesValid & NR_BIGINT) != 0) {
                return _getBigInteger();
            }
            _throwInternal();
        }
        // 09-Jul-2020, tatu: [databind#2644] requires we will retain accuracy, so:
        if (_numTypesValid == NR_UNKNOWN) {
            _parseNumericValue(NR_BIGDECIMAL);
        }
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return _getBigDecimal();
        }
        if ((_numTypesValid & NR_FLOAT) != 0) {
            return _getNumberFloat();
        }
        if ((_numTypesValid & NR_DOUBLE) == 0) { // sanity check
            _throwInternal();
        }
        return _getNumberDouble();
    }

    @Override
    public Object getNumberValueDeferred()
    {
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_UNKNOWN);
            }
            if ((_numTypesValid & NR_INT) != 0) {
                return _numberInt;
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return _numberLong;
            }
            if ((_numTypesValid & NR_BIGINT) != 0) {
                // from _getBigInteger()
                if (_numberBigInt != null) {
                    return _numberBigInt;
                } else if (_numberString != null) {
                    return _numberString;
                }
                return _getBigInteger(); // will fail
            }
            _throwInternal();
        }
        if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
            // Ok this gets tricky since flags are not set quite as with
            // integers
            if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
                return _getBigDecimal();
            }
            if ((_numTypesValid & NR_DOUBLE) != 0) { // sanity check
                return _getNumberDouble();
            }
            if ((_numTypesValid & NR_FLOAT) != 0) {
                return _getNumberFloat();
            }
            // Should be able to rely on this; might want to set _numberString
            // but state keeping looks complicated so don't do that yet
            return _textBuffer.contentsAsString();
        }
        // We'll just force exception by:
        return getNumberValue();
    }

    @Override
    public NumberType getNumberType()
    {
        if (_numTypesValid == NR_UNKNOWN) {
            _parseNumericValue(NR_UNKNOWN); // will also check event type
        }
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((_numTypesValid & NR_INT) != 0) {
                return NumberType.INT;
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return NumberType.LONG;
            }
            return NumberType.BIG_INTEGER;
        }

        /* And then floating point types. Here optimal type
         * needs to be big decimal, to avoid losing any data?
         * However... using BD is slow, so let's allow returning
         * double as type if no explicit call has been made to access
         * data as BD?
         */
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return NumberType.BIG_DECIMAL;
        }
        if ((_numTypesValid & NR_FLOAT) != 0) {
            return NumberType.FLOAT;
        }
        return NumberType.DOUBLE;
    }

    @Override
    public int getIntValue() throws JacksonException
    {
        if ((_numTypesValid & NR_INT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) { // not parsed at all
                return _parseIntValue();
            }
            if ((_numTypesValid & NR_INT) == 0) { // wasn't an int natively?
                convertNumberToInt(); // let's make it so, if possible
            }
        }
        return _numberInt;
    }

    @Override
    public long getLongValue() throws JacksonException
    {
        if ((_numTypesValid & NR_LONG) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_LONG);
            }
            if ((_numTypesValid & NR_LONG) == 0) {
                convertNumberToLong();
            }
        }
        return _numberLong;
    }

    @Override
    public BigInteger getBigIntegerValue() throws JacksonException
    {
        if ((_numTypesValid & NR_BIGINT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_BIGINT);
            }
            if ((_numTypesValid & NR_BIGINT) == 0) {
                convertNumberToBigInteger();
            }
        }
        return _getBigInteger();
    }

    @Override
    public float getFloatValue() throws JacksonException
    {
        // 22-Jan-2009, tatu: Bounds/range checks would be tricky
        //   here, so let's not bother even trying...
        /*
        if (value < -Float.MAX_VALUE || value > MAX_FLOAT_D) {
            _reportError("Numeric value ("+getText()+") out of range of Java float");
        }
        */
        if ((_numTypesValid & NR_FLOAT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_FLOAT);
            }
            if ((_numTypesValid & NR_FLOAT) == 0) {
                convertNumberToFloat();
            }
        }
        return _getNumberFloat();
    }

    @Override
    public double getDoubleValue() throws JacksonException
    {
        if ((_numTypesValid & NR_DOUBLE) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_DOUBLE);
            }
            if ((_numTypesValid & NR_DOUBLE) == 0) {
                convertNumberToDouble();
            }
        }
        return _getNumberDouble();
    }

    @Override
    public BigDecimal getDecimalValue() throws JacksonException
    {
        if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_BIGDECIMAL);
            }
            if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
                convertNumberToBigDecimal();
            }
        }
        return _getBigDecimal();
    }

    /*
    /**********************************************************************
    /* Abstract methods sub-classes will need to provide
    /**********************************************************************
     */

    /**
     * Method that will parse actual numeric value out of a syntactically
     * valid number value. Type it will parse into depends on whether
     * it is a floating point number, as well as its magnitude: smallest
     * legal type (of ones available) is used for efficiency.
     *
     * @param expType Numeric type that we will immediately need, if any;
     *   mostly necessary to optimize handling of floating point numbers
     *
     * @throws WrappedIOException for low-level read issues
     * @throws InputCoercionException if the current token not of numeric type
     * @throws tools.jackson.core.exc.StreamReadException for number decoding problems
     */
    protected abstract void _parseNumericValue(int expType)
        throws JacksonException, InputCoercionException;

    protected abstract int _parseIntValue() throws JacksonException;

    /*
    /**********************************************************************
    /* Numeric conversions
    /**********************************************************************
     */

    protected void convertNumberToInt() throws InputCoercionException
    {
        // First, converting from long ought to be easy
        if ((_numTypesValid & NR_LONG) != 0) {
            // Let's verify its lossless conversion by simple roundtrip
            int result = (int) _numberLong;
            if (((long) result) != _numberLong) {
                _reportOverflowInt(getText(), currentToken());
            }
            _numberInt = result;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            final BigInteger bigInteger = _getBigInteger();
            if (BI_MIN_INT.compareTo(bigInteger) > 0
                    || BI_MAX_INT.compareTo(bigInteger) < 0) {
                _reportOverflowInt();
            }
            _numberInt = bigInteger.intValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            final double d = _getNumberDouble();
            if (d < MIN_INT_D || d > MAX_INT_D) {
                _reportOverflowInt();
            }
            _numberInt = (int) d;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            final BigDecimal bigDecimal = _getBigDecimal();
            if (BD_MIN_INT.compareTo(bigDecimal) > 0
                || BD_MAX_INT.compareTo(bigDecimal) < 0) {
                _reportOverflowInt();
            }
            _numberInt = bigDecimal.intValue();
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_INT;
    }

    protected void convertNumberToLong() throws InputCoercionException
    {
        if ((_numTypesValid & NR_INT) != 0) {
            _numberLong = (long) _numberInt;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            final BigInteger bigInteger = _getBigInteger();
            if (BI_MIN_LONG.compareTo(bigInteger) > 0
                    || BI_MAX_LONG.compareTo(bigInteger) < 0) {
                _reportOverflowLong();
            }
            _numberLong = bigInteger.longValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            final double d = _getNumberDouble();
            if (d < MIN_LONG_D || d > MAX_LONG_D) {
                _reportOverflowLong();
            }
            _numberLong = (long) d;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            final BigDecimal bigDecimal = _getBigDecimal();
            if (BD_MIN_LONG.compareTo(bigDecimal) > 0
                || BD_MAX_LONG.compareTo(bigDecimal) < 0) {
                _reportOverflowLong();
            }
            _numberLong = bigDecimal.longValue();
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_LONG;
    }

    protected void convertNumberToBigInteger()
    {
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            // here it'll just get truncated, no exceptions thrown
            _numberBigInt = _convertBigDecimalToBigInteger(_getBigDecimal());
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberInt);
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            if (_numberString != null) {
                _numberBigInt = _convertBigDecimalToBigInteger(_getBigDecimal());
            } else {
                _numberBigInt = _convertBigDecimalToBigInteger(BigDecimal.valueOf(_getNumberDouble()));
            }
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_BIGINT;
    }

    protected void convertNumberToDouble()
    {
        /* 05-Aug-2008, tatus: Important note: this MUST start with
         *   more accurate representations, since we don't know which
         *   value is the original one (others get generated when
         *   requested)
         */

        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            if (_numberString != null) {
                _numberDouble = _getNumberDouble();
            } else {
                _numberDouble = _getBigDecimal().doubleValue();
            }
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            if (_numberString != null) {
                _numberDouble = _getNumberDouble();
            } else {
                _numberDouble = _getBigInteger().doubleValue();
            }
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberDouble = (double) _numberLong;
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberDouble = (double) _numberInt;
        } else if ((_numTypesValid & NR_FLOAT) != 0) {
            if (_numberString != null) {
                _numberDouble = _getNumberDouble();
            } else {
                _numberDouble = (double) _getNumberFloat();
            }
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_DOUBLE;
    }

    protected void convertNumberToFloat()
    {
        /* 05-Aug-2008, tatus: Important note: this MUST start with
         *   more accurate representations, since we don't know which
         *   value is the original one (others get generated when
         *   requested)
         */

        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            if (_numberString != null) {
                _numberFloat = _getNumberFloat();
            } else {
                _numberFloat = _getBigDecimal().floatValue();
            }
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            if (_numberString != null) {
                _numberFloat = _getNumberFloat();
            } else {
                _numberFloat = _getBigInteger().floatValue();
            }
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberFloat = (float) _numberLong;
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberFloat = (float) _numberInt;
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            if (_numberString != null) {
                _numberFloat = _getNumberFloat();
            } else {
                _numberFloat = (float) _getNumberDouble();
            }
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_FLOAT;
    }

    protected void convertNumberToBigDecimal()
    {
        // 05-Aug-2008, tatus: Important note: this MUST start with more
        //   accurate representations, since we don't know which value is
        //   the original one (others get generated when requested)

        if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Let's actually parse from String representation, to avoid
            // rounding errors that non-decimal floating operations could incur
            final String numStr = _numberString == null ? getText() : _numberString;
            _numberBigDecimal = NumberInput.parseBigDecimal(
                    numStr,
                    isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            _numberBigDecimal = new BigDecimal(_getBigInteger());
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigDecimal = BigDecimal.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigDecimal = BigDecimal.valueOf(_numberInt);
        } else {
            _throwInternal();
        }
        _numTypesValid |= NR_BIGDECIMAL;
    }

    // @since 2.15
    protected BigInteger _convertBigDecimalToBigInteger(BigDecimal bigDec) {
        // 04-Apr-2022, tatu: wrt [core#968] Need to limit max scale magnitude
        //   (may throw StreamConstraintsException)
        _streamReadConstraints.validateBigIntegerScale(bigDec.scale());
        return bigDec.toBigInteger();
    }

    /**
     * Internal accessor that needs to be used for accessing number value of type
     * {@link BigInteger} which -- as of 2.14 -- is typically lazily parsed.
     */
    protected BigInteger _getBigInteger() {
        if (_numberBigInt != null) {
            return _numberBigInt;
        } else if (_numberString == null) {
            throw new IllegalStateException("cannot get BigInteger from current parser state");
        }
        try {
            // NOTE! Length of number string has been validated earlier
            _numberBigInt = NumberInput.parseBigInteger(
                    _numberString,
                    isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        } catch (NumberFormatException nex) {
            throw _constructReadException("Malformed numeric value ("+_longNumberDesc(_numberString)+")", nex);
        }
        _numberString = null;
        return _numberBigInt;
    }

    /**
     * Internal accessor that needs to be used for accessing number value of type
     * {@link BigDecimal} which -- as of 2.14 -- is typically lazily parsed.
     */
    protected BigDecimal _getBigDecimal() {
        if (_numberBigDecimal != null) {
            return _numberBigDecimal;
        } else if (_numberString == null) {
            throw new IllegalStateException("cannot get BigDecimal from current parser state");
        }
        try {
            // NOTE! Length of number string has been validated earlier
            _numberBigDecimal = NumberInput.parseBigDecimal(
                    _numberString,
                    isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        } catch (NumberFormatException nex) {
            throw _constructReadException("Malformed numeric value ("+_longNumberDesc(_numberString)+")", nex);
        }
        _numberString = null;
        return _numberBigDecimal;
    }

    /**
     * Internal accessor that needs to be used for accessing number value of type
     * {@code double} which will be lazily parsed.
     */
    protected double _getNumberDouble() {
        if (_numberString != null) {
            try {
                _numberDouble = NumberInput.parseDouble(_numberString,
                        isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
            } catch (NumberFormatException nex) {
                throw _constructReadException("Malformed numeric value ("+_longNumberDesc(_numberString)+")", nex);
            }
            _numberString = null;
        }
        return _numberDouble;
    }

    /**
     * Internal accessor that needs to be used for accessing number value of type
     * {@code float} which will be lazily parsed.
     *
     * @since 2.15
     */
    protected float _getNumberFloat() {
        if (_numberString != null) {
            try {
                _numberFloat = NumberInput.parseFloat(_numberString,
                        isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
            } catch (NumberFormatException nex) {
                throw _constructReadException("Malformed numeric value ("+_longNumberDesc(_numberString)+")", nex);
            }
            _numberString = null;
        }
        return _numberFloat;
    }

    /*
    /**********************************************************************
    /* Base64 handling support
    /**********************************************************************
     */

    /**
     * Method that sub-classes must implement to support escaped sequences
     * in base64-encoded sections.
     * Sub-classes that do not need base64 support can leave this as is
     *
     * @return Character decoded, if any
     *
     * @throws JacksonException If escape decoding fails
     */
    protected char _decodeEscaped() throws JacksonException {
        throw new UnsupportedOperationException();
    }

    protected final int _decodeBase64Escape(Base64Variant b64variant, int ch, int index)
        throws JacksonException
    {
        // Need to handle escaped chars
        if (ch != '\\') {
            _reportInvalidBase64Char(b64variant, ch, index);
        }
        int unescaped = _decodeEscaped();
        // if white space, skip if first triplet; otherwise errors
        if (unescaped <= INT_SPACE) {
            if (index == 0) { // whitespace only allowed to be skipped between triplets
                return -1;
            }
        }
        // otherwise try to find actual triplet value
        int bits = b64variant.decodeBase64Char(unescaped);
        if (bits < 0) {
            if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                _reportInvalidBase64Char(b64variant, unescaped, index);
            }
        }
        return bits;
    }

    protected final int _decodeBase64Escape(Base64Variant b64variant, char ch, int index)
        throws JacksonException
    {
        if (ch != '\\') {
            _reportInvalidBase64Char(b64variant, ch, index);
            return -1; // never gets here
        }
        char unescaped = _decodeEscaped();
        // if white space, skip if first triplet; otherwise errors
        if (unescaped <= INT_SPACE) {
            if (index == 0) { // whitespace only allowed to be skipped between triplets
                return -1;
            }
        }
        // otherwise try to find actual triplet value
        int bits = b64variant.decodeBase64Char(unescaped);
        if (bits < 0) {
            // second check since padding can only be 3rd or 4th byte (index #2 or #3)
            if ((bits != Base64Variant.BASE64_VALUE_PADDING) || (index < 2)) {
                _reportInvalidBase64Char(b64variant, unescaped, index);
            }
        }
        return bits;
    }

    protected <T> T _reportInvalidBase64Char(Base64Variant b64variant, int ch, int bindex)
            throws StreamReadException {
        return _reportInvalidBase64Char(b64variant, ch, bindex, null);
    }

    /*
     * @param bindex Relative index within base64 character unit; between 0
     *  and 3 (as unit has exactly 4 characters)
     */
    protected <T> T _reportInvalidBase64Char(Base64Variant b64variant, int ch, int bindex, String msg)
            throws StreamReadException
    {
        String base;
        if (ch <= INT_SPACE) {
            base = String.format("Illegal white space character (code 0x%s) as character #%d of 4-char base64 unit: can only used between units",
                    Integer.toHexString(ch), (bindex+1));
        } else if (b64variant.usesPaddingChar(ch)) {
            base = "Unexpected padding character ('"+b64variant.getPaddingChar()+"') as character #"+(bindex+1)+" of 4-char base64 unit: padding only legal as 3rd or 4th character";
        } else if (!Character.isDefined(ch) || Character.isISOControl(ch)) {
            // Not sure if we can really get here... ? (most illegal xml chars are caught at lower level)
            base = "Illegal character (code 0x"+Integer.toHexString(ch)+") in base64 content";
        } else {
            base = "Illegal character '"+((char)ch)+"' (code 0x"+Integer.toHexString(ch)+") in base64 content";
        }
        if (msg != null) {
            base = base + ": " + msg;
        }
        return _reportError(base);
    }

    protected <T> T _handleBase64MissingPadding(Base64Variant b64variant)
            throws StreamReadException
    {
        return _reportError(b64variant.missingPaddingMessage());
    }

    /*
    /**********************************************************************
    /* Internal/package methods: other
    /**********************************************************************
     */

    /**
     * Helper method used to encapsulate logic of including (or not) of
     * "content reference" when constructing {@link JsonLocation} instances.
     *
     * @return ContentReference object to use.
     */
    protected ContentReference _contentReference() {
        if (isEnabled(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)) {
            return _ioContext.contentReference();
        }
        return _contentReferenceRedacted();
    }

    /**
     * Helper method used to encapsulate logic of providing
     * "content reference" when constructing {@link JsonLocation} instances
     * and source information is <b>NOT</b> to be included
     * ({@code StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION} disabled).
     *<p>
     * Default implementation will simply return {@code ContentReference.unknown()}.
     *
     * @return ContentReference object to use when source is not to be included
     */
    protected ContentReference _contentReferenceRedacted() {
        return ContentReference.redacted();
    }
}
