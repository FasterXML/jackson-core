package com.fasterxml.jackson.core.json.async;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.async.NonBlockingInputFeeder;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ByteSourceJsonBootstrapper;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.VersionUtil;

public class NonBlockingJsonParser
    extends NonBlockingJsonParserBase
    implements ByteArrayFeeder
{
    /*
    /**********************************************************************
    /* Input source config
    /**********************************************************************
     */

    /**
     * This buffer is actually provided via {@link NonBlockingInputFeeder}
     */
    protected byte[] _inputBuffer = NO_BYTES;

    /**
     * In addition to current buffer pointer, and end pointer,
     * we will also need to know number of bytes originally
     * contained. This is needed to correctly update location
     * information when the block has been completed.
     */
    protected int _origBufferLen;

    // And from ParserBase:
//  protected int _inputPtr;
//  protected int _inputEnd;

    /*
    /**********************************************************************
    /* Location tracking, additional
    /**********************************************************************
     */

    /**
     * Alternate row tracker, used to keep track of position by `\r` marker
     * (whereas <code>_currInputRow</code> tracks `\n`). Used to simplify
     * tracking of linefeeds, assuming that input typically uses various
     * linefeed combinations (`\r`, `\n` or `\r\n`) consistently, in which
     * case we can simply choose max of two row candidates.
     */
    protected int _currInputRowAlt = 1;

    /*
    /**********************************************************************
    /* Other state
    /**********************************************************************
     */

    protected int _currentQuote;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public NonBlockingJsonParser(IOContext ctxt, int parserFeatures,
            ByteQuadsCanonicalizer sym)
    {
        super(ctxt, parserFeatures, sym);
    }

    /*
    /**********************************************************************
    /* AsyncInputFeeder impl
    /**********************************************************************
     */

    @Override
    public ByteArrayFeeder getNonBlockingInputFeeder() {
        return this;
    }

    @Override
    public final boolean needMoreInput() {
        return (_inputPtr >=_inputEnd) && !_endOfInput;
    }

    @Override
    public void feedInput(byte[] buf, int start, int end) throws IOException
    {
        // Must not have remaining input
        if (_inputPtr < _inputEnd) {
            _reportError("Still have %d undecoded bytes, should not call 'feedInput'", _inputEnd - _inputPtr);
        }
        if (end < start) {
            _reportError("Input end (%d) may not be before start (%d)", end, start);
        }
        // and shouldn't have been marked as end-of-input
        if (_endOfInput) {
            _reportError("Already closed, can not feed more input");
        }
        // Time to update pointers first
        _currInputProcessed += _origBufferLen;

        // And then update buffer settings
        _inputBuffer = buf;
        _inputPtr = start;
        _inputEnd = end;
        _origBufferLen = end - start;
    }

    @Override
    public void endOfInput() {
        _endOfInput = true;
    }

    /*
    /**********************************************************************
    /* Abstract methods/overrides from JsonParser
    /**********************************************************************
     */

    /* Implementing these methods efficiently for non-blocking cases would
     * be complicated; so for now let's just use the default non-optimized
     * implementation
     */

//    public boolean nextFieldName(SerializableString str) throws IOException
//    public String nextTextValue() throws IOException
//    public int nextIntValue(int defaultValue) throws IOException
//    public long nextLongValue(long defaultValue) throws IOException
//    public Boolean nextBooleanValue() throws IOException

    @Override
    public int releaseBuffered(OutputStream out) throws IOException {
        int avail = _inputEnd - _inputPtr;
        if (avail > 0) {
            out.write(_inputBuffer, _inputPtr, avail);
        }
        return avail;
    }
    
    /*
    /**********************************************************************
    /* Main-level decoding
    /**********************************************************************
     */

    @Override
    public JsonToken nextToken() throws IOException
    {
        // First: regardless of where we really are, need at least one more byte;
        // can simplify some of the checks by short-circuiting right away
        if (_inputPtr >= _inputEnd) {
            if (_closed) {
                return null;
            }
            // note: if so, do not even bother changing state
            if (_endOfInput) { // except for this special case
                return _eofAsNextToken();
            }
            return JsonToken.NOT_AVAILABLE;
        }
        // in the middle of tokenization?
        if (_currToken == JsonToken.NOT_AVAILABLE) {
            return _finishToken();
        }

        // No: fresh new token; may or may not have existing one
        _numTypesValid = NR_UNKNOWN;
        _tokenInputTotal = _currInputProcessed + _inputPtr;
        // also: clear any data retained so far
        _binaryValue = null;
        int ch = _inputBuffer[_inputPtr++];

        switch (_majorState) {
        case MAJOR_INITIAL:
            return _startDocument(ch);

        case MAJOR_ROOT:
            return _startValue(ch);

        case MAJOR_OBJECT_FIELD: // field or end-object
            // expect name
            return _startFieldName(ch);
            
        case MAJOR_OBJECT_VALUE:
        case MAJOR_ARRAY_ELEMENT: // element or end-array
            return _startValue(ch);

        default:
        }
        VersionUtil.throwInternal();
        return null;
    }

    /**
     * Method called when a (scalar) value type has been detected, but not all of
     * contents have been decoded due to incomplete input available.
     */
    protected final JsonToken _finishToken() throws IOException
    {
        // NOTE: caller ensures availability of at least one byte

        switch (_minorState) {
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Second-level decoding, root level
    /**********************************************************************
     */

    private final JsonToken _startDocument(int ch) throws IOException
    {
        ch &= 0xFF;

        // Very first byte: could be BOM
        if (ch == ByteSourceJsonBootstrapper.UTF8_BOM_1) {
            // !!! TODO
        }

        // If not BOM (or we got past it), could be whitespace or comment to skip
        while (ch <= 0x020) {
            if (ch != INT_SPACE) {
                if (ch == INT_LF) {
                    ++_currInputRow;
                    _currInputRowStart = _inputPtr;
                } else if (ch == INT_CR) {
                    ++_currInputRowAlt;
                    _currInputRowStart = _inputPtr;
                } else if (ch != INT_TAB) {
                    _throwInvalidSpace(ch);
                }
            }
            if (_inputPtr >= _inputEnd) {
                _minorState = MINOR_FIELD_ROOT_GOT_SEPARATOR;
                if (_closed) {
                    return null;
                }
                // note: if so, do not even bother changing state
                if (_endOfInput) { // except for this special case
                    return _eofAsNextToken();
                }
                return JsonToken.NOT_AVAILABLE;
            }
            ch = _inputBuffer[_inputPtr++] & 0xFF;
        }
        return _startValue(ch);
    }

    /*
    /**********************************************************************
    /* Second-level decoding, value parsing
    /**********************************************************************
     */
    
    /**
     * Helper method called to detect type of a value token (at any level), and possibly
     * decode it if contained in input buffer.
     * Note that possible header has been ruled out by caller and is not checked here.
     */
    private final JsonToken _startValue(int ch) throws IOException
    {
        if (ch == INT_QUOTE) {
            return _startString(ch);
        }
        switch (ch) {
        case '-':
            return _startNegativeNumber();

        // Should we have separate handling for plus? Although
        // it is not allowed per se, it may be erroneously used,
        // and could be indicate by a more specific error message.
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return _startPositiveNumber(ch);
        case 'f':
            return _startFalseToken();
        case 'n':
            return _startNullToken();
        case 't':
            return _startTrueToken();
        case '[':
            return _startArrayScope();
        case ']':
            return _closeArrayScope();
        case '{':
            return _startObjectScope();
        case '}':
            return _closeObjectScope();
        default:
        }
        return _startUnexpectedValue(ch);
    }

    protected JsonToken _startUnexpectedValue(int ch) throws IOException
    {
        // TODO: Maybe support non-standard tokens that streaming parser does:
        //
        // * NaN
        // * Infinity
        // * Plus-prefix for numbers
        // * Apostrophe for Strings

        switch (ch) {
        case '\'':
            return _startString(ch);
            
        case ',':
            // If Feature.ALLOW_MISSING_VALUES is enabled we may allow "missing values",
            // that is, encountering a trailing comma or closing marker where value would be expected
            if (!_parsingContext.inObject() && isEnabled(Feature.ALLOW_MISSING_VALUES)) {
                // Important to "push back" separator, to be consumed before next value;
                // does not lead to infinite loop
               --_inputPtr;
               return _valueComplete(JsonToken.VALUE_NULL);
            }
            break;
        }
        // !!! TODO: maybe try to collect more information for better diagnostics
        _reportUnexpectedChar(ch, "expected a valid value (number, String, array, object, 'true', 'false' or 'null')");
        return null;
    }

    /*
    /**********************************************************************
    /* Second-level decoding, simple tokens
    /**********************************************************************
     */

    protected JsonToken _startFalseToken() throws IOException
    {
        return null;
    }

    protected JsonToken _startTrueToken() throws IOException
    {
        return null;
    }

    protected JsonToken _startNullToken() throws IOException
    {
        return null;
    }

    /*
    /**********************************************************************
    /* Second-level decoding, String decoding
    /**********************************************************************
     */

    protected JsonToken _startString(int q) throws IOException
    {
        _currentQuote = q;
        return null;
    }

    /*
    /**********************************************************************
    /* Second-level decoding, String decoding
    /**********************************************************************
     */

    protected JsonToken _startPositiveNumber(int ch) throws IOException
    {
        return null;
    }
    
    protected JsonToken _startNegativeNumber() throws IOException
    {
        return null;
    }

    /*
    /**********************************************************************
    /* Second-level decoding, Name decoding
    /**********************************************************************
     */

    /**
     * Method that handles initial token type recognition for token
     * that has to be either FIELD_NAME or END_OBJECT.
     */
    protected final JsonToken _startFieldName(int ch) throws IOException
    {
        return null;
    }
}
