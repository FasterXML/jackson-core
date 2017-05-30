package com.fasterxml.jackson.core.json.async;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.async.NonBlockingInputFeeder;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ByteSourceJsonBootstrapper;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.VersionUtil;

public class NonBlockingJsonParser
    extends NonBlockingJsonParserBase
    implements ByteArrayFeeder
{
    // This is the main input-code lookup table, fetched eagerly
//    private final static int[] _icUTF8 = CharTypes.getInputCodeUtf8();

    // Latin1 encoding is not supported, but we do use 8-bit subset for
    // pre-processing task, to simplify first pass, keep it fast.
    protected final static int[] _icLatin1 = CharTypes.getInputCodeLatin1();

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
                // End-of-input within (possibly...) started token is bit complicated,
                // so offline
                if (_currToken == JsonToken.NOT_AVAILABLE) {
                    return _finishTokenWithEOF();
                }
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
        int ch = _inputBuffer[_inputPtr++] & 0xFF;

        switch (_majorState) {
        case MAJOR_INITIAL:
            return _startDocument(ch);

        case MAJOR_ROOT:
            return _startValue(ch);

        case MAJOR_OBJECT_FIELD_FIRST: // field or end-object
            // expect name
            return _startFieldName(ch);
        case MAJOR_OBJECT_FIELD_NEXT: // comma
            return _startFieldNameAfterComma(ch);

        case MAJOR_OBJECT_VALUE: // require semicolon first
            return _startValueAfterColon(ch);

        case MAJOR_ARRAY_ELEMENT_FIRST: // value without leading comma
            return _startValue(ch);

        case MAJOR_ARRAY_ELEMENT_NEXT: // require leading comma
            return _startValueAfterComma(ch);

        default:
        }
        VersionUtil.throwInternal();
        return null;
    }

    /**
     * Method called when decoding of a token has been started, but not yet completed due
     * to missing input; method is to continue decoding due to at least one more byte
     * being made available to decode.
     */
    protected final JsonToken _finishToken() throws IOException
    {
        // NOTE: caller ensures there's input available...
        switch (_minorState) {
        case MINOR_FIELD_LEADING_WS:
            return _startFieldName(_inputBuffer[_inputPtr++] & 0xFF);
        case MINOR_FIELD_LEADING_COMMA:
            return _startFieldNameAfterComma(_inputBuffer[_inputPtr++] & 0xFF);

        case MINOR_VALUE_LEADING_WS:
            return _startValue(_inputBuffer[_inputPtr++] & 0xFF);
        case MINOR_VALUE_LEADING_COMMA:
            return _startValueAfterComma(_inputBuffer[_inputPtr++] & 0xFF);
        case MINOR_VALUE_LEADING_COLON:
            return _startValueAfterColon(_inputBuffer[_inputPtr++] & 0xFF);

        case MINOR_VALUE_TOKEN_NULL:
            return _finishKeywordToken("null", _pending32, JsonToken.VALUE_NULL);
        case MINOR_VALUE_TOKEN_TRUE:
            return _finishKeywordToken("true", _pending32, JsonToken.VALUE_TRUE);
        case MINOR_VALUE_TOKEN_FALSE:
            return _finishKeywordToken("false", _pending32, JsonToken.VALUE_FALSE);
        case MINOR_VALUE_TOKEN_ERROR: // case of "almost token", just need tokenize for error
            return _finishErrorToken();

        case MINOR_NUMBER_LEADING_MINUS:
            return _finishNumberLeadingMinus(_inputBuffer[_inputPtr++] & 0xFF);
        case MINOR_NUMBER_LEADING_ZERO:
            return _finishNumberLeadingZeroes();
        case MINOR_NUMBER_INTEGER_DIGITS:
            return _finishNumberIntegralPart();
        case MINOR_NUMBER_DECIMAL_POINT:
        case MINOR_NUMBER_FRACTION_DIGITS:
        case MINOR_NUMBER_EXPONENT_MARKER:
        case MINOR_NUMBER_EXPONENT_SIGN:
        case MINOR_NUMBER_EXPONENT_DIGITS:
        }
        VersionUtil.throwInternal();
        return null;
    }

    /**
     * Method similar to {@link #_finishToken}, but called when no more input is
     * available, and end-of-input has been detected. This is usually problem
     * case, but not always: root-level values may be properly terminated by
     * this, and similarly trailing white-space may have been skipped.
     */
    protected final JsonToken _finishTokenWithEOF() throws IOException
    {
        // NOTE: caller ensures there's input available...
        JsonToken t = _currToken;
        switch (_minorState) {
        case MINOR_ROOT_GOT_SEPARATOR: // fine, just skip some trailing space
            return _eofAsNextToken();

        case MINOR_VALUE_LEADING_WS: // finished at token boundary; probably fine
        case MINOR_VALUE_LEADING_COMMA: // not fine
        case MINOR_VALUE_LEADING_COLON: // not fine
            return _eofAsNextToken();
        case MINOR_VALUE_TOKEN_NULL:
            return _finishKeywordTokenWithEOF("null", _pending32, JsonToken.VALUE_NULL);
        case MINOR_VALUE_TOKEN_TRUE:
            return _finishKeywordTokenWithEOF("true", _pending32, JsonToken.VALUE_TRUE);
        case MINOR_VALUE_TOKEN_FALSE:
            return _finishKeywordTokenWithEOF("false", _pending32, JsonToken.VALUE_FALSE);
        case MINOR_VALUE_TOKEN_ERROR: // case of "almost token", just need tokenize for error
            return _finishErrorTokenWithEOF();

        // Number-parsing states; first, valid:
        case MINOR_NUMBER_LEADING_ZERO:
            return _valueCompleteInt(0, "0");
        case MINOR_NUMBER_INTEGER_DIGITS:
            // Fine: just need to ensure we have value fully defined
            {
                int len = _textBuffer.getCurrentSegmentSize();
                if (_numberNegative) {
                    --len;
                }
                _intLength = len;
            }
            return _valueComplete(JsonToken.VALUE_NUMBER_INT);

        // !!! TODO: rest...
        default:
        }
        _reportInvalidEOF(": was expecting rest of token (internal state: "+_minorState+")", _currToken);
        return t; // never gets here
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
                _minorState = MINOR_ROOT_GOT_SEPARATOR;
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
     * Value may be preceded by leading white-space, but no separator (comma).
     */
    private final JsonToken _startValue(int ch) throws IOException
    {
        // First: any leading white space?
        if (ch <= 0x0020) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                _minorState = MINOR_VALUE_LEADING_WS;
                return _currToken;
            }
        }

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
            return _startLeadingZero();
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

    /**
     * Helper method called to parse token that is either a value token in array
     * or end-array marker
     */
    private final JsonToken _startValueAfterComma(int ch) throws IOException
    {
        // First: any leading white space?
        if (ch <= 0x0020) {
            ch = _skipWS(ch); // will skip through all available ws (and comments)
            if (ch <= 0) {
                _minorState = MINOR_VALUE_LEADING_COMMA;
                return _currToken;
            }
        }
        if (ch != INT_COMMA) {
            if (ch == INT_RBRACKET) {
                return _closeArrayScope();
            }
            if (ch == INT_RCURLY){
                return _closeObjectScope();
            }
            _reportUnexpectedChar(ch, "was expecting comma to separate "+_parsingContext.typeDesc()+" entries");
        }
        int ptr = _inputPtr;
        if (ptr >= _inputEnd) {
            _minorState = MINOR_VALUE_LEADING_WS;
            return (_currToken = JsonToken.NOT_AVAILABLE);
        }
        ch = _inputBuffer[ptr];
        _inputPtr = ptr+1;
        if (ch <= 0x0020) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                _minorState = MINOR_VALUE_LEADING_WS;
                return _currToken;
            }
        }
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
            return _startLeadingZero();

        case '1':
        case '2': case '3':
        case '4': case '5':
        case '6': case '7':
        case '8': case '9':
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
            if (JsonParser.Feature.ALLOW_TRAILING_COMMA.enabledIn(_features)) {
                return _closeArrayScope();
            }
            break;
        case '{':
            return _startObjectScope();
        case '}':
            if (JsonParser.Feature.ALLOW_TRAILING_COMMA.enabledIn(_features)) {
                return _closeObjectScope();
            }
            break;
        default:
        }
        return _startUnexpectedValue(ch);
    }

    /**
     * Helper method called to detect type of a value token (at any level), and possibly
     * decode it if contained in input buffer.
     * Value MUST be preceded by a semi-colon (which may be surrounded by white-space)
     */
    private final JsonToken _startValueAfterColon(int ch) throws IOException
    {
        // First: any leading white space?
        if (ch <= 0x0020) {
            ch = _skipWS(ch); // will skip through all available ws (and comments)
            if (ch <= 0) {
                _minorState = MINOR_VALUE_LEADING_COLON;
                return _currToken;
            }
        }
        if (ch != INT_COLON) {
            // can not omit colon here
            _reportUnexpectedChar(ch, "was expecting a colon to separate field name and value");
        }
        int ptr = _inputPtr;
        if (ptr >= _inputEnd) {
            _minorState = MINOR_VALUE_LEADING_WS;
            return (_currToken = JsonToken.NOT_AVAILABLE);
        }
        ch = _inputBuffer[ptr];
        _inputPtr = ptr+1;
        if (ch <= 0x0020) {
            ch = _skipWS(ch); // will skip through all available ws (and comments)
            if (ch <= 0) {
                _minorState = MINOR_VALUE_LEADING_WS;
                return _currToken;
            }
        }
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
            return _startLeadingZero();

        case '1':
        case '2': case '3':
        case '4': case '5':
        case '6': case '7':
        case '8': case '9':
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

    private final int _skipWS(int ch) throws IOException
    {
        do {
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
                if (_endOfInput) { // except for this special case
                    _eofAsNextToken();
                } else {
                    _currToken = JsonToken.NOT_AVAILABLE;
                }
                return 0;
            }
            ch = _inputBuffer[_inputPtr++] & 0xFF;
        } while (ch <= 0x0020);
        return ch;
    }

    /*
    /**********************************************************************
    /* Second-level decoding, simple tokens
    /**********************************************************************
     */

    protected JsonToken _startFalseToken() throws IOException
    {
        int ptr = _inputPtr;
        if ((ptr + 4) < _inputEnd) { // yes, can determine efficiently
            byte[] buf = _inputBuffer;
            if ((buf[ptr++] == 'a') 
                   && (buf[ptr++] == 'l')
                   && (buf[ptr++] == 's')
                   && (buf[ptr++] == 'e')) {
                int ch = buf[ptr] & 0xFF;
                if (ch < INT_0 || (ch == INT_RBRACKET) || (ch == INT_RCURLY)) { // expected/allowed chars
                    _inputPtr = ptr;
                    return _valueComplete(JsonToken.VALUE_FALSE);
                }
            }
        }
        _minorState = MINOR_VALUE_TOKEN_FALSE;
        return _finishKeywordToken("false", 1, JsonToken.VALUE_FALSE);
    }

    protected JsonToken _startTrueToken() throws IOException
    {
        int ptr = _inputPtr;
        if ((ptr + 3) < _inputEnd) { // yes, can determine efficiently
            byte[] buf = _inputBuffer;
            if ((buf[ptr++] == 'r') 
                   && (buf[ptr++] == 'u')
                   && (buf[ptr++] == 'e')) {
                int ch = buf[ptr] & 0xFF;
                if (ch < INT_0 || (ch == INT_RBRACKET) || (ch == INT_RCURLY)) { // expected/allowed chars
                    _inputPtr = ptr;
                    return _valueComplete(JsonToken.VALUE_TRUE);
                }
            }
        }
        _minorState = MINOR_VALUE_TOKEN_TRUE;
        return _finishKeywordToken("true", 1, JsonToken.VALUE_TRUE);
    }

    protected JsonToken _startNullToken() throws IOException
    {
        int ptr = _inputPtr;
        if ((ptr + 3) < _inputEnd) { // yes, can determine efficiently
            byte[] buf = _inputBuffer;
            if ((buf[ptr++] == 'u') 
                   && (buf[ptr++] == 'l')
                   && (buf[ptr++] == 'l')) {
                int ch = buf[ptr] & 0xFF;
                if (ch < INT_0 || (ch == INT_RBRACKET) || (ch == INT_RCURLY)) { // expected/allowed chars
                    _inputPtr = ptr;
                    return _valueComplete(JsonToken.VALUE_NULL);
                }
            }
        }
        _minorState = MINOR_VALUE_TOKEN_NULL;
        return _finishKeywordToken("null", 1, JsonToken.VALUE_NULL);
    }

    protected JsonToken _finishKeywordToken(String expToken, int matched,
            JsonToken result) throws IOException
    {
        final int end = expToken.length();

        while (true) {
            if (_inputPtr >= _inputEnd) {
                _pending32 = matched;
                return (_currToken = JsonToken.NOT_AVAILABLE);
            }
            int ch = _inputBuffer[_inputPtr] & 0xFF;
            if (matched == end) { // need to verify trailing separator
                if (ch < INT_0 || (ch == INT_RBRACKET) || (ch == INT_RCURLY)) { // expected/allowed chars
                    return _valueComplete(result);
                }
                break;
            }
            if (ch != expToken.charAt(matched)) {
                break;
            }
            ++matched;
            ++_inputPtr;
        }
        _minorState = MINOR_VALUE_TOKEN_ERROR;
        _textBuffer.resetWithCopy(expToken, 0, matched);
        return _finishErrorToken();
    }

    protected JsonToken _finishKeywordTokenWithEOF(String expToken, int matched,
            JsonToken result) throws IOException
    {
        if (matched == expToken.length()) {
            return (_currToken = result);
        }
        _textBuffer.resetWithCopy(expToken, 0, matched);
        return _finishErrorTokenWithEOF();
    }

    protected JsonToken _finishErrorToken() throws IOException
    {
        while (_inputPtr < _inputEnd) {
            int i = (int) _inputBuffer[_inputPtr++];

// !!! TODO: Decode UTF-8 characters properly...
//            char c = (char) _decodeCharForError(i);

            char ch = (char) i;
            if (Character.isJavaIdentifierPart(ch)) {
                // 11-Jan-2016, tatu: note: we will fully consume the character,
                // included or not, so if recovery was possible, it'd be off-by-one...
                _textBuffer.append(ch);
                if (_textBuffer.size() < MAX_ERROR_TOKEN_LENGTH) {
                    continue;
                }
            }
            _reportError("Unrecognized token '%s': was expecting %s", _textBuffer.contentsAsString(),
                    "'null', 'true' or 'false'");
        }
        return (_currToken = JsonToken.NOT_AVAILABLE);
    }

    protected JsonToken _finishErrorTokenWithEOF() throws IOException
    {
        _reportError("Unrecognized token '%s': was expecting %s", _textBuffer.contentsAsString(),
                "'null', 'true' or 'false'");
        return JsonToken.NOT_AVAILABLE; // never gets here
    }

    /*
    /**********************************************************************
    /* Second-level decoding, Number decoding
    /**********************************************************************
     */

    protected JsonToken _startPositiveNumber(int ch) throws IOException
    {
        _numberNegative = false;
        // in unlikely event of not having more input, denote location
        if (_inputPtr >= _inputEnd) {
            _minorState = MINOR_NUMBER_INTEGER_DIGITS;
            _textBuffer.emptyAndGetCurrentSegment();
            _textBuffer.append((char) ch);
            return (_currToken = JsonToken.NOT_AVAILABLE);
        }
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        outBuf[0] = (char) ch;
        ch = _inputBuffer[_inputPtr];

        int outPtr = 1;

        ch &= 0xFF;
        while (true) {
            if (ch < INT_0) {
                if (ch == INT_PERIOD) {
                    _intLength = outPtr;
                    ++_inputPtr;
                    return _startFloat(outBuf, outPtr, ch);
                }
                break;
            }
            if (ch > INT_9) {
                if (ch == INT_e || ch == INT_E) {
                    _intLength = outPtr;
                    ++_inputPtr;
                    return _startFloat(outBuf, outPtr, ch);
                }
                break;
            }
            if (outPtr >= outBuf.length) {
                // NOTE: must expand to ensure contents all in a single buffer (to keep
                // other parts of parsing simpler)
                outBuf = _textBuffer.expandCurrentSegment();
            }
            outBuf[outPtr++] = (char) ch;
            if (++_inputPtr >= _inputEnd) {
                _minorState = MINOR_NUMBER_INTEGER_DIGITS;
                _textBuffer.setCurrentLength(outPtr);
                return (_currToken = JsonToken.NOT_AVAILABLE);
            }
            ch = _inputBuffer[_inputPtr] & 0xFF;
        }
        _intLength = outPtr;
        _textBuffer.setCurrentLength(outPtr);
        return _valueComplete(JsonToken.VALUE_NUMBER_INT);
    }
    
    protected JsonToken _startNegativeNumber() throws IOException
    {
        if (_inputPtr >= _inputEnd) {
            _minorState = MINOR_NUMBER_LEADING_MINUS;
            return (_currToken = JsonToken.NOT_AVAILABLE);
        }
        int ch = _inputBuffer[_inputPtr++] & 0xFF;
        if (ch <= INT_0) {
            if (ch == INT_0) {
                return _startLeadingZero();
            }
            // One special case: if first char is 0, must not be followed by a digit
            reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        } else if (ch > INT_9) {
            // !!! TODO: -Infinite etc
            reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        }
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        _numberNegative = true;
        outBuf[0] = '-';
        outBuf[1] = (char) ch;
        if (_inputPtr >= _inputEnd) {
            _minorState = MINOR_NUMBER_INTEGER_DIGITS;
            _textBuffer.setCurrentLength(2);
            _intLength = 1;
            return (_currToken = JsonToken.NOT_AVAILABLE);
        }
        ch = _inputBuffer[_inputPtr];
        int outPtr = 2;

        while (true) {
            if (ch < INT_0) {
                if (ch == INT_PERIOD) {
                    _intLength = outPtr-1;
                    ++_inputPtr;
                    return _startFloat(outBuf, outPtr, ch);
                }
                break;
            }
            if (ch > INT_9) {
                if (ch == INT_e || ch == INT_E) {
                    _intLength = outPtr-1;
                    ++_inputPtr;
                    return _startFloat(outBuf, outPtr, ch);
                }
                break;
            }
            if (outPtr >= outBuf.length) {
                // NOTE: must expand, to ensure contiguous buffer, outPtr is the length
                outBuf = _textBuffer.expandCurrentSegment();
            }
            outBuf[outPtr++] = (char) ch;
            if (++_inputPtr >= _inputEnd) {
                _minorState = MINOR_NUMBER_INTEGER_DIGITS;
                _textBuffer.setCurrentLength(outPtr);
                return (_currToken = JsonToken.NOT_AVAILABLE);
            }
            ch = _inputBuffer[_inputPtr] & 0xFF;
        }
        _intLength = outPtr-1;
        _textBuffer.setCurrentLength(outPtr);
        return _valueComplete(JsonToken.VALUE_NUMBER_INT);
    }

    protected JsonToken _startLeadingZero() throws IOException
    {
        int ptr = _inputPtr;
        if (ptr >= _inputEnd) {
            _minorState = MINOR_NUMBER_LEADING_ZERO;
            return (_currToken = JsonToken.NOT_AVAILABLE);
        }

        // While we could call `_finishNumberLeadingZeroes()`, let's try checking
        // the very first char after first zero since the most common case is that
        // there is a separator

        int ch = _inputBuffer[ptr++] & 0xFF;
        // one early check: leading zeroes may or may not be allowed
        if (ch <= INT_0) {
            if (ch == INT_PERIOD) {
                _inputPtr = ptr;
                _intLength = 1;
                char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
                outBuf[0] = '0';
                outBuf[1] = '.';
                return _startFloat(outBuf, 2, ch);
            }
            // although not guaranteed, seems likely valid separator (white space,
            // comma, end bracket/curly); next time token needed will verify
            if (ch == INT_0) {
                _inputPtr = ptr;
                _minorState = MINOR_NUMBER_LEADING_ZERO;
                return _finishNumberLeadingZeroes();
            }
        } else if (ch > INT_9) {
            if (ch == INT_e || ch == INT_E) {
                _inputPtr = ptr;
                _intLength = 1;
                char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
                outBuf[0] = '0';
                outBuf[1] = (char) ch;
                return _startFloat(outBuf, 2, ch);
            }
            // Ok; unfortunately we have closing bracket/curly that are valid so need
            // (colon not possible since this is within value, not after key)
            // 
            if ((ch != INT_RBRACKET) && (ch != INT_RCURLY)) {
                reportUnexpectedNumberChar(ch,
                        "expected digit (0-9), decimal point (.) or exponent indicator (e/E) to follow '0'");
            }
        }
        // leave _inputPtr as-is, to push back byte we checked
        return _valueCompleteInt(0, "0");
    }

    protected JsonToken _finishNumberLeadingZeroes() throws IOException
    {
        // In general, skip further zeroes (if allowed), look for legal follow-up
        // numeric characters; likely legal separators, or, known illegal (letters).

        while (true) {
            if (_inputPtr >= _inputEnd) {
                return (_currToken = JsonToken.NOT_AVAILABLE);
            }
            int ch = _inputBuffer[_inputPtr] & 0xFF;
            if (ch <= INT_0) {
                if (ch == INT_PERIOD) {
                    _intLength = 1;
                    char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
                    outBuf[0] = '0';
                    outBuf[1] = '.';
                    return _startFloat(outBuf, 2, ch);
                }
                // although not guaranteed, seems likely valid separator (white space,
                // comma, end bracket/curly); next time token needed will verify
                if (ch == INT_0) {
                    if (!isEnabled(Feature.ALLOW_NUMERIC_LEADING_ZEROS)) {
                        reportInvalidNumber("Leading zeroes not allowed");
                    }
                } else {
                    break;
                }
            } else if (ch > INT_9) {
                if (ch == INT_e || ch == INT_E) {
                    _intLength = 1;
                    char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
                    outBuf[0] = '0';
                    outBuf[1] = (char) ch;
                    return _startFloat(outBuf, 2, ch);
                }
                // Ok; unfortunately we have closing bracket/curly that are valid so need
                // (colon not possible since this is within value, not after key)
                // 
                if ((ch != INT_RBRACKET) && (ch != INT_RCURLY)) {
                    reportUnexpectedNumberChar(ch,
                            "expected digit (0-9), decimal point (.) or exponent indicator (e/E) to follow '0'");
                }
                break;
            } else { // Number between 1 and 9; go integral
                return _finishNumberIntegralPart();
            }

            // If we stay here, it means we are skipping zeroes...
            if (_inputPtr >= _inputEnd) {
                // already got appropriate state set up so...
                return JsonToken.NOT_AVAILABLE;
            }
        }
        return _valueCompleteInt(0, "0");
    }

    protected JsonToken _finishNumberLeadingMinus(int ch) throws IOException
    {
        if (ch <= INT_0) {
            if (ch == INT_0) {
                return _startLeadingZero();
            }
            // One special case: if first char is 0, must not be followed by a digit
            reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        } else if (ch > INT_9) {
            // !!! TODO: -Infinite etc
            reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        }
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        _numberNegative = true;
        outBuf[0] = '-';
        outBuf[1] = (char) ch;
        _intLength = 1;
        _textBuffer.setCurrentLength(2);
        _minorState = MINOR_NUMBER_INTEGER_DIGITS;
        return _finishNumberIntegralPart();
    }

    protected JsonToken _finishNumberIntegralPart() throws IOException
    {
        char[] outBuf = _textBuffer.getBufferWithoutReset();
        int outPtr = _textBuffer.getCurrentSegmentSize();
        int neg = _numberNegative ? 1 : 0;

        while (true) {
            if (_inputPtr >= _inputEnd) {
                _textBuffer.setCurrentLength(outPtr);
                return (_currToken = JsonToken.NOT_AVAILABLE);
            }
            int ch = _inputBuffer[_inputPtr] & 0xFF;
            if (ch < INT_0) {
                if (ch == INT_PERIOD) {
                    _intLength = outPtr+neg;
                    ++_inputPtr;
                    return _startFloat(outBuf, outPtr, ch);
                }
                break;
            }
            if (ch > INT_9) {
                if (ch == INT_e || ch == INT_E) {
                    _intLength = outPtr+neg;
                    ++_inputPtr;
                    return _startFloat(outBuf, outPtr, ch);
                }
                break;
            }
            ++_inputPtr;
            if (outPtr >= outBuf.length) {
                // NOTE: must expand to ensure contents all in a single buffer (to keep
                // other parts of parsing simpler)
                outBuf = _textBuffer.expandCurrentSegment();
            }
            outBuf[outPtr++] = (char) ch;
        }
        _intLength = outPtr+neg;
        _textBuffer.setCurrentLength(outPtr);
        return _valueComplete(JsonToken.VALUE_NUMBER_INT);
    }

    protected JsonToken _startFloat(char[] outBuf, int outPtr, int ch) throws IOException
    {
        VersionUtil.throwInternal();
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
        // First: any leading white space?
        if (ch <= 0x0020) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                _minorState = MINOR_FIELD_LEADING_WS;
                return _currToken;
            }
        }
        if (ch == INT_RCURLY) {
            return _closeObjectScope();
        }
        String n = _parseName(ch);
        if (n == null) {
// !!! TODO: name parsing
            // note: called method should have set minor state
if (true) VersionUtil.throwInternal();
            return (_currToken = JsonToken.NOT_AVAILABLE);
        }
        _majorState = MAJOR_OBJECT_VALUE;
        _parsingContext.setCurrentName(n);
        return (_currToken = JsonToken.FIELD_NAME);
    }

    protected final JsonToken _startFieldNameAfterComma(int ch) throws IOException
    {
        // First: any leading white space?
        if (ch <= 0x0020) {
            ch = _skipWS(ch); // will skip through all available ws (and comments)
            if (ch <= 0) {
                _minorState = MINOR_FIELD_LEADING_COMMA;
                return _currToken;
            }
        }
        if (ch != INT_COMMA) { // either comma, separating entries, or closing right curly
            if (ch == INT_RCURLY) {
                return _closeObjectScope();
            }
            _reportUnexpectedChar(ch, "was expecting comma to separate "+_parsingContext.typeDesc()+" entries");
        }
        int ptr = _inputPtr;
        if (ptr >= _inputEnd) {
            _minorState = MINOR_FIELD_LEADING_WS;
            return (_currToken = JsonToken.NOT_AVAILABLE);
        }
        ch = _inputBuffer[ptr];
        _inputPtr = ptr+1;
        if (ch <= 0x0020) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                _minorState = MINOR_FIELD_LEADING_WS;
                return _currToken;
            }
        }

        _updateLocation();
        String n = _parseName(ch);
        if (n == null) {
// !!! TODO: name parsing
            // note: called method should have set minor state
if (true) VersionUtil.throwInternal();
            return (_currToken = JsonToken.NOT_AVAILABLE);
        }
        _majorState = MAJOR_OBJECT_VALUE;
        _parsingContext.setCurrentName(n);
        return (_currToken = JsonToken.FIELD_NAME);
    }

    protected final String _parseName(int i) throws IOException
    {
        if (i != INT_QUOTE) {
            return _handleOddName(i);
        }
        // First: can we optimize out bounds checks?
        if ((_inputPtr + 13) > _inputEnd) { // Need up to 12 chars, plus one trailing (quote)
            return slowParseName();
        }

        // If so, can also unroll loops nicely
        /* 25-Nov-2008, tatu: This may seem weird, but here we do
         *   NOT want to worry about UTF-8 decoding. Rather, we'll
         *   assume that part is ok (if not it will get caught
         *   later on), and just handle quotes and backslashes here.
         */
        final byte[] input = _inputBuffer;
        final int[] codes = _icLatin1;

        int q = input[_inputPtr++] & 0xFF;

        if (codes[q] == 0) {
            i = input[_inputPtr++] & 0xFF;
            if (codes[i] == 0) {
                q = (q << 8) | i;
                i = input[_inputPtr++] & 0xFF;
                if (codes[i] == 0) {
                    q = (q << 8) | i;
                    i = input[_inputPtr++] & 0xFF;
                    if (codes[i] == 0) {
                        q = (q << 8) | i;
                        i = input[_inputPtr++] & 0xFF;
                        if (codes[i] == 0) {
                            _quad1 = q;
                            return parseMediumName(i);
                        }
                        if (i == INT_QUOTE) { // 4 byte/char case or broken
                            return _findName(q, 4);
                        }
                        return parseName(q, i, 4);
                    }
                    if (i == INT_QUOTE) { // 3 byte/char case or broken
                        return _findName(q, 3);
                    }
                    return parseName(q, i, 3);
                }                
                if (i == INT_QUOTE) { // 2 byte/char case or broken
                    return _findName(q, 2);
                }
                return parseName(q, i, 2);
            }
            if (i == INT_QUOTE) { // one byte/char case or broken
                return _findName(q, 1);
            }
            return parseName(q, i, 1);
        }     
        if (q == INT_QUOTE) { // special case, ""
            return "";
        }
        return parseName(0, q, 0); // quoting or invalid char
    }

    protected final String parseMediumName(int q2) throws IOException
    {
        final byte[] input = _inputBuffer;
        final int[] codes = _icLatin1;

        // Ok, got 5 name bytes so far
        int i = input[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 5 bytes
                return _findName(_quad1, q2, 1);
            }
            return parseName(_quad1, q2, i, 1); // quoting or invalid char
        }
        q2 = (q2 << 8) | i;
        i = input[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 6 bytes
                return _findName(_quad1, q2, 2);
            }
            return parseName(_quad1, q2, i, 2);
        }
        q2 = (q2 << 8) | i;
        i = input[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 7 bytes
                return _findName(_quad1, q2, 3);
            }
            return parseName(_quad1, q2, i, 3);
        }
        q2 = (q2 << 8) | i;
        i = input[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 8 bytes
                return _findName(_quad1, q2, 4);
            }
            return parseName(_quad1, q2, i, 4);
        }
        return parseMediumName2(i, q2);
    }

    /**
     * @since 2.6
     */
    protected final String parseMediumName2(int q3, final int q2) throws IOException
    {
        final byte[] input = _inputBuffer;
        final int[] codes = _icLatin1;

        // Got 9 name bytes so far
        int i = input[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 9 bytes
                return _findName(_quad1, q2, q3, 1);
            }
            return parseName(_quad1, q2, q3, i, 1);
        }
        q3 = (q3 << 8) | i;
        i = input[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 10 bytes
                return _findName(_quad1, q2, q3, 2);
            }
            return parseName(_quad1, q2, q3, i, 2);
        }
        q3 = (q3 << 8) | i;
        i = input[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 11 bytes
                return _findName(_quad1, q2, q3, 3);
            }
            return parseName(_quad1, q2, q3, i, 3);
        }
        q3 = (q3 << 8) | i;
        i = input[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 12 bytes
                return _findName(_quad1, q2, q3, 4);
            }
            return parseName(_quad1, q2, q3, i, 4);
        }
        return parseLongName(i, q2, q3);
    }
    
    protected final String parseLongName(int q, final int q2, int q3) throws IOException
    {
        _quadBuffer[0] = _quad1;
        _quadBuffer[1] = q2;
        _quadBuffer[2] = q3;

        // As explained above, will ignore UTF-8 encoding at this point
        final byte[] input = _inputBuffer;
        final int[] codes = _icLatin1;
        int qlen = 3;

        while ((_inputPtr + 4) <= _inputEnd) {
            int i = input[_inputPtr++] & 0xFF;
            if (codes[i] != 0) {
                if (i == INT_QUOTE) {
                    return _findName(_quadBuffer, qlen, q, 1);
                }
                return parseEscapedName(_quadBuffer, qlen, q, i, 1);
            }

            q = (q << 8) | i;
            i = input[_inputPtr++] & 0xFF;
            if (codes[i] != 0) {
                if (i == INT_QUOTE) {
                    return _findName(_quadBuffer, qlen, q, 2);
                }
                return parseEscapedName(_quadBuffer, qlen, q, i, 2);
            }

            q = (q << 8) | i;
            i = input[_inputPtr++] & 0xFF;
            if (codes[i] != 0) {
                if (i == INT_QUOTE) {
                    return _findName(_quadBuffer, qlen, q, 3);
                }
                return parseEscapedName(_quadBuffer, qlen, q, i, 3);
            }

            q = (q << 8) | i;
            i = input[_inputPtr++] & 0xFF;
            if (codes[i] != 0) {
                if (i == INT_QUOTE) {
                    return _findName(_quadBuffer, qlen, q, 4);
                }
                return parseEscapedName(_quadBuffer, qlen, q, i, 4);
            }

            // Nope, no end in sight. Need to grow quad array etc
            if (qlen >= _quadBuffer.length) {
                _quadBuffer = growArrayBy(_quadBuffer, qlen);
            }
            _quadBuffer[qlen++] = q;
            q = i;
        }

        /* Let's offline if we hit buffer boundary (otherwise would
         * need to [try to] align input, which is bit complicated
         * and may not always be possible)
         */
        return parseEscapedName(_quadBuffer, qlen, 0, q, 0);
    }

    /**
     * Method called when not even first 8 bytes are guaranteed
     * to come consecutively. Happens rarely, so this is offlined;
     * plus we'll also do full checks for escaping etc.
     */
    protected String slowParseName() throws IOException
    {
        if (_inputPtr >= _inputEnd) {
            if (!_loadMore()) {
                _reportInvalidEOF(": was expecting closing '\"' for name", JsonToken.FIELD_NAME);
            }
        }
        int i = _inputBuffer[_inputPtr++] & 0xFF;
        if (i == INT_QUOTE) { // special case, ""
            return "";
        }
        return parseEscapedName(_quadBuffer, 0, 0, i, 0);
    }

    private final String parseName(int q1, int ch, int lastQuadBytes) throws IOException {
        return parseEscapedName(_quadBuffer, 0, q1, ch, lastQuadBytes);
    }

    private final String parseName(int q1, int q2, int ch, int lastQuadBytes) throws IOException {
        _quadBuffer[0] = q1;
        return parseEscapedName(_quadBuffer, 1, q2, ch, lastQuadBytes);
    }

    private final String parseName(int q1, int q2, int q3, int ch, int lastQuadBytes) throws IOException {
        _quadBuffer[0] = q1;
        _quadBuffer[1] = q2;
        return parseEscapedName(_quadBuffer, 2, q3, ch, lastQuadBytes);
    }
    
    /**
     * Slower parsing method which is generally branched to when
     * an escape sequence is detected (or alternatively for long
     * names, one crossing input buffer boundary).
     * Needs to be able to handle more exceptional cases, gets slower,
     * and hence is offlined to a separate method.
     */
    protected final String parseEscapedName(int[] quads, int qlen, int currQuad, int ch,
            int currQuadBytes) throws IOException
    {
        // 25-Nov-2008, tatu: This may seem weird, but here we do not want to worry about
        //   UTF-8 decoding yet. Rather, we'll assume that part is ok (if not it will get
        //   caught later on), and just handle quotes and backslashes here.
        final int[] codes = _icLatin1;

        while (true) {
            if (codes[ch] != 0) {
                if (ch == INT_QUOTE) { // we are done
                    break;
                }
                // Unquoted white space?
                if (ch != INT_BACKSLASH) {
                    // As per [JACKSON-208], call can now return:
                    _throwUnquotedSpace(ch, "name");
                } else {
                    // Nope, escape sequence
                    ch = _decodeEscaped();
                }
                // Oh crap. May need to UTF-8 (re-)encode it, if it's beyond
                // 7-bit ASCII. Gets pretty messy. If this happens often, may
                // want to use different name canonicalization to avoid these hits.
                if (ch > 127) {
                    // Ok, we'll need room for first byte right away
                    if (currQuadBytes >= 4) {
                        if (qlen >= quads.length) {
                            _quadBuffer = quads = growArrayBy(quads, quads.length);
                        }
                        quads[qlen++] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                    }
                    if (ch < 0x800) { // 2-byte
                        currQuad = (currQuad << 8) | (0xc0 | (ch >> 6));
                        ++currQuadBytes;
                        // Second byte gets output below:
                    } else { // 3 bytes; no need to worry about surrogates here
                        currQuad = (currQuad << 8) | (0xe0 | (ch >> 12));
                        ++currQuadBytes;
                        // need room for middle byte?
                        if (currQuadBytes >= 4) {
                            if (qlen >= quads.length) {
                                _quadBuffer = quads = growArrayBy(quads, quads.length);
                            }
                            quads[qlen++] = currQuad;
                            currQuad = 0;
                            currQuadBytes = 0;
                        }
                        currQuad = (currQuad << 8) | (0x80 | ((ch >> 6) & 0x3f));
                        ++currQuadBytes;
                    }
                    // And same last byte in both cases, gets output below:
                    ch = 0x80 | (ch & 0x3f);
                }
            }
            // Ok, we have one more byte to add at any rate:
            if (currQuadBytes < 4) {
                ++currQuadBytes;
                currQuad = (currQuad << 8) | ch;
            } else {
                if (qlen >= quads.length) {
                    _quadBuffer = quads = growArrayBy(quads, quads.length);
                }
                quads[qlen++] = currQuad;
                currQuad = ch;
                currQuadBytes = 1;
            }
            if (_inputPtr >= _inputEnd) {
                if (!_loadMore()) {
                    _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
                }
            }
            ch = _inputBuffer[_inputPtr++] & 0xFF;
        }

        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                _quadBuffer = quads = growArrayBy(quads, quads.length);
            }
            quads[qlen++] = _padLastQuad(currQuad, currQuadBytes);
        }
        String name = _symbols.findName(quads, qlen);
        if (name == null) {
            name = _addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    /**
     * Method called when we see non-white space character other
     * than double quote, when expecting a field name.
     * In standard mode will just throw an exception; but
     * in non-standard modes may be able to parse name.
     */
    protected String _handleOddName(int ch) throws IOException
    {
        // First: may allow single quotes
        if (ch == '\'' && isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
            return _parseAposName();
        }
        // [JACKSON-69]: allow unquoted names if feature enabled:
        if (!isEnabled(Feature.ALLOW_UNQUOTED_FIELD_NAMES)) {
         // !!! TODO: Decode UTF-8 characters properly...
//            char c = (char) _decodeCharForError(ch);
            char c = (char) ch;
            _reportUnexpectedChar(c, "was expecting double-quote to start field name");
        }
        /* Also: note that although we use a different table here,
         * it does NOT handle UTF-8 decoding. It'll just pass those
         * high-bit codes as acceptable for later decoding.
         */
        final int[] codes = CharTypes.getInputCodeUtf8JsNames();
        // Also: must start with a valid character...
        if (codes[ch] != 0) {
            _reportUnexpectedChar(ch, "was expecting either valid name character (for unquoted name) or double-quote (for quoted) to start field name");
        }

        /* Ok, now; instead of ultra-optimizing parsing here (as with
         * regular JSON names), let's just use the generic "slow"
         * variant. Can measure its impact later on if need be
         */
        int[] quads = _quadBuffer;
        int qlen = 0;
        int currQuad = 0;
        int currQuadBytes = 0;

        while (true) {
            // Ok, we have one more byte to add at any rate:
            if (currQuadBytes < 4) {
                ++currQuadBytes;
                currQuad = (currQuad << 8) | ch;
            } else {
                if (qlen >= quads.length) {
                    _quadBuffer = quads = growArrayBy(quads, quads.length);
                }
                quads[qlen++] = currQuad;
                currQuad = ch;
                currQuadBytes = 1;
            }
            if (_inputPtr >= _inputEnd) {
                if (!_loadMore()) {
                    _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
                }
            }
            ch = _inputBuffer[_inputPtr] & 0xFF;
            if (codes[ch] != 0) {
                break;
            }
            ++_inputPtr;
        }

        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                _quadBuffer = quads = growArrayBy(quads, quads.length);
            }
            quads[qlen++] = currQuad;
        }
        String name = _symbols.findName(quads, qlen);
        if (name == null) {
            name = _addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    /* Parsing to support [JACKSON-173]. Plenty of duplicated code;
     * main reason being to try to avoid slowing down fast path
     * for valid JSON -- more alternatives, more code, generally
     * bit slower execution.
     */
    protected String _parseAposName() throws IOException
    {
        if (_inputPtr >= _inputEnd) {
            if (!_loadMore()) {
                _reportInvalidEOF(": was expecting closing '\'' for field name", JsonToken.FIELD_NAME);
            }
        }
        int ch = _inputBuffer[_inputPtr++] & 0xFF;
        if (ch == '\'') { // special case, ''
            return "";
        }
        int[] quads = _quadBuffer;
        int qlen = 0;
        int currQuad = 0;
        int currQuadBytes = 0;

        // Copied from parseEscapedFieldName, with minor mods:

        final int[] codes = _icLatin1;

        while (true) {
            if (ch == '\'') {
                break;
            }
            // additional check to skip handling of double-quotes
            if (ch != '"' && codes[ch] != 0) {
                if (ch != '\\') {
                    // Unquoted white space?
                    _throwUnquotedSpace(ch, "name");
                } else {
                    // Nope, escape sequence
                    ch = _decodeEscaped();
                }
                if (ch > 127) {
                    // Ok, we'll need room for first byte right away
                    if (currQuadBytes >= 4) {
                        if (qlen >= quads.length) {
                            _quadBuffer = quads = growArrayBy(quads, quads.length);
                        }
                        quads[qlen++] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                    }
                    if (ch < 0x800) { // 2-byte
                        currQuad = (currQuad << 8) | (0xc0 | (ch >> 6));
                        ++currQuadBytes;
                        // Second byte gets output below:
                    } else { // 3 bytes; no need to worry about surrogates here
                        currQuad = (currQuad << 8) | (0xe0 | (ch >> 12));
                        ++currQuadBytes;
                        // need room for middle byte?
                        if (currQuadBytes >= 4) {
                            if (qlen >= quads.length) {
                                _quadBuffer = quads = growArrayBy(quads, quads.length);
                            }
                            quads[qlen++] = currQuad;
                            currQuad = 0;
                            currQuadBytes = 0;
                        }
                        currQuad = (currQuad << 8) | (0x80 | ((ch >> 6) & 0x3f));
                        ++currQuadBytes;
                    }
                    // And same last byte in both cases, gets output below:
                    ch = 0x80 | (ch & 0x3f);
                }
            }
            // Ok, we have one more byte to add at any rate:
            if (currQuadBytes < 4) {
                ++currQuadBytes;
                currQuad = (currQuad << 8) | ch;
            } else {
                if (qlen >= quads.length) {
                    _quadBuffer = quads = growArrayBy(quads, quads.length);
                }
                quads[qlen++] = currQuad;
                currQuad = ch;
                currQuadBytes = 1;
            }
            if (_inputPtr >= _inputEnd) {
                if (!_loadMore()) {
                    _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
                }
            }
            ch = _inputBuffer[_inputPtr++] & 0xFF;
        }

        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                _quadBuffer = quads = growArrayBy(quads, quads.length);
            }
            quads[qlen++] = _padLastQuad(currQuad, currQuadBytes);
        }
        String name = _symbols.findName(quads, qlen);
        if (name == null) {
            name = _addName(quads, qlen, currQuadBytes);
        }
        return name;
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


    // !!! TODO: only temporarily here

    private final boolean _loadMore() {
        VersionUtil.throwInternal();
        return false;
    }
}
