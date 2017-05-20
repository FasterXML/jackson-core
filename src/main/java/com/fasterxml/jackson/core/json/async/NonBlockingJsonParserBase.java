package com.fasterxml.jackson.core.json.async;

import java.io.*;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;

/**
 * Intermediate base class for non-blocking JSON parsers.
 */
public abstract class NonBlockingJsonParserBase
    extends ParserBase
{
    /*
    /**********************************************************************
    /* Major state constants
    /**********************************************************************
     */

    /**
     * State right after parser has been constructed, before seeing the first byte
     * to know if there's header.
     */
    protected final static int MAJOR_INITIAL = 0;

    /**
     * State right after parser a root value has been
     * finished, but next token has not yet been recognized.
     */
    protected final static int MAJOR_ROOT = 1;

    protected final static int MAJOR_OBJECT_FIELD = 2;
    protected final static int MAJOR_OBJECT_VALUE = 3;

    protected final static int MAJOR_ARRAY_ELEMENT = 4;

    /**
     * State after non-blocking input source has indicated that no more input
     * is forthcoming AND we have exhausted all the input
     */
    protected final static int MAJOR_CLOSED = 5;

    /*
    /**********************************************************************
    /* Minor state constants
    /**********************************************************************
     */

    /**
     * State between root-level value, waiting for at least one white-space
     * character as separator
     */
    protected final static int MINOR_FIELD_ROOT_NEED_SEPARATOR = 1;

    /**
     * State between root-level value, having processed at least one white-space
     * character, and expecting either more, start of a value, or end of input
     * stream.
     */
    protected final static int MINOR_FIELD_ROOT_GOT_SEPARATOR = 2;
    
    protected final static int MINOR_FIELD_NAME = 10;

    protected final static int MINOR_VALUE_NUMBER = 11;

    protected final static int MINOR_VALUE_STRING = 15;

    protected final static int MINOR_VALUE_TOKEN_NULL = 15;
    protected final static int MINOR_VALUE_TOKEN_TRUE = 15;
    protected final static int MINOR_VALUE_TOKEN_FALSE = 15;

    /*
    /**********************************************************************
    /* Helper objects, symbols (field names)
    /**********************************************************************
     */

    /**
     * Symbol table that contains field names encountered so far
     */
    final protected ByteQuadsCanonicalizer _symbols;

    /**
     * Temporary buffer used for name parsing.
     */
    protected int[] _quadBuffer = NO_INTS;

    /**
     * Quads used for hash calculation
     */
    protected int _quad1, _quad2;

    /*
    /**********************************************************************
    /* Additional parsing state
    /**********************************************************************
     */

    /**
     * Current main decoding state
     */
    protected int _majorState;

    /**
     * Addition indicator within state; contextually relevant for just that state
     */
    protected int _minorState;

    /**
     * Value of {@link #_majorState} after completing a scalar value
     */
    protected int _majorStateAfterValue;

    /**
     * Flag that is sent when calling application indicates that there will
     * be no more input to parse.
     */
    protected boolean _endOfInput = false;

    /*
    /**********************************************************************
    /* Other buffering
    /**********************************************************************
     */
    
    /**
     * Temporary buffer for holding content if input not contiguous (but can
     * fit in buffer)
     */
    protected byte[] _inputCopy;

    /**
     * Number of bytes buffered in <code>_inputCopy</code>
     */
    protected int _inputCopyLen;

    /**
     * Temporary storage for 32-bit values (int, float), as well as length markers
     * for length-prefixed values.
     */
    protected int _pending32;

    /**
     * Temporary storage for 64-bit values (long, double), secondary storage
     * for some other things (scale of BigDecimal values)
     */
    protected long _pending64;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public NonBlockingJsonParserBase(IOContext ctxt, int parserFeatures,
            ByteQuadsCanonicalizer sym)
    {
        super(ctxt, parserFeatures);
        _symbols = sym;
        // We don't need a lot; for most things maximum known a-priori length below 70 bytes
        _inputCopy = ctxt.allocReadIOBuffer(500);

        _currToken = null;
        _majorState = MAJOR_INITIAL;
        _majorStateAfterValue = MAJOR_ROOT;
    }

    @Override
    public ObjectCodec getCodec() {
        return null;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        throw new UnsupportedOperationException("Can not use ObjectMapper with non-blocking parser");
    }

    /**
     * @since 2.9
     */
    @Override
    public boolean canParseAsync() { return true; }

    /*
    /**********************************************************
    /* Test support
    /**********************************************************
     */

    protected ByteQuadsCanonicalizer symbolTableForTests() {
        return _symbols;
    }

    /*
    /**********************************************************
    /* Abstract methods from JsonParser
    /**********************************************************
     */

    @Override
    public abstract int releaseBuffered(OutputStream out) throws IOException;

    @Override
    public Object getInputSource() {
        // since input is "pushed", to traditional source...
        return null;
    }

    @Override
    protected void _closeInput() throws IOException {
        // nothing to do here
    }

    /*
    /**********************************************************************
    /* Overridden methods
    /**********************************************************************
     */

    @Override
    public boolean hasTextCharacters()
    {
        if (_currToken == JsonToken.VALUE_STRING) {
            // yes; is or can be made available efficiently as char[]
            return _textBuffer.hasTextAsCharacters();
        }
        if (_currToken == JsonToken.FIELD_NAME) {
            // not necessarily; possible but:
            return _nameCopied;
        }
        // other types, no benefit from accessing as char[]
        return false;
    }

    /*
    /**********************************************************************
    /* Public API, access to token information, text
    /**********************************************************************
     */

    /**
     * Method for accessing textual representation of the current event;
     * if no current event (before first call to {@link #nextToken}, or
     * after encountering end-of-input), returns null.
     * Method can be called for any event.
     */
    @Override
    public String getText() throws IOException
    {
        if (_currToken == JsonToken.VALUE_STRING) {
            return _textBuffer.contentsAsString();
        }
        JsonToken t = _currToken;
        if (t == null || _currToken == JsonToken.NOT_AVAILABLE) { // null only before/after document
            return null;
        }
        if (t == JsonToken.FIELD_NAME) {
            return _parsingContext.getCurrentName();
        }
        if (t.isNumeric()) {
            // TODO: optimize?
            return getNumberValue().toString();
        }
        return _currToken.asString();
    }

    @Override
    public char[] getTextCharacters() throws IOException
    {
        switch (currentTokenId()) {
        case JsonTokenId.ID_STRING:
            return _textBuffer.getTextBuffer();
        case JsonTokenId.ID_FIELD_NAME:
            if (!_nameCopied) {
                String name = _parsingContext.getCurrentName();
                int nameLen = name.length();
                if (_nameCopyBuffer == null) {
                    _nameCopyBuffer = _ioContext.allocNameCopyBuffer(nameLen);
                } else if (_nameCopyBuffer.length < nameLen) {
                    _nameCopyBuffer = new char[nameLen];
                }
                name.getChars(0, nameLen, _nameCopyBuffer, 0);
                _nameCopied = true;
            }
            return _nameCopyBuffer;
        case JsonTokenId.ID_NUMBER_INT:
        case JsonTokenId.ID_NUMBER_FLOAT:
            return getNumberValue().toString().toCharArray();
        case JsonTokenId.ID_NO_TOKEN:
        case JsonTokenId.ID_NOT_AVAILABLE:
            return null;
        default:
            return _currToken.asCharArray();
        }
    }

    @Override    
    public int getTextLength() throws IOException
    {
        switch (currentTokenId()) {
        case JsonTokenId.ID_STRING:
            return _textBuffer.size();
        case JsonTokenId.ID_FIELD_NAME:
            return _parsingContext.getCurrentName().length();
        case JsonTokenId.ID_NUMBER_INT:
        case JsonTokenId.ID_NUMBER_FLOAT:
            return getNumberValue().toString().length();
        case JsonTokenId.ID_NO_TOKEN:
        case JsonTokenId.ID_NOT_AVAILABLE:
            return 0; // or throw exception?
        default:
            return _currToken.asCharArray().length;
        }
    }

    @Override
    public int getTextOffset() throws IOException {
        return 0;
    }

    @Override
    public int getText(Writer w) throws IOException
    {
        if (_currToken == JsonToken.VALUE_STRING) {
            return _textBuffer.contentsToWriter(w);
        }
        if (_currToken == JsonToken.NOT_AVAILABLE) {
            _reportError("Current token not available: can not call this method");
        }
        // otherwise default handling works fine
        return super.getText(w);
    }
    
    /*
    /**********************************************************************
    /* Public API, access to token information, binary
    /**********************************************************************
     */

    @Override
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException
    {
        if (_currToken != JsonToken.VALUE_EMBEDDED_OBJECT ) {
            _reportError("Current token (%s) not VALUE_EMBEDDED_OBJECT, can not access as binary", _currToken);
        }
        return _binaryValue;
    }

    @Override
    public Object getEmbeddedObject() throws IOException
    {
        if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT ) {
            return _binaryValue;
        }
        return null;
    }

    @Override
    public int readBinaryValue(Base64Variant b64variant, OutputStream out)
            throws IOException {
        if (_currToken != JsonToken.VALUE_EMBEDDED_OBJECT ) {
            _reportError("Current token (%s) not VALUE_EMBEDDED_OBJECT, can not access as binary", _currToken);
        }
        out.write(_binaryValue);
        return _binaryValue.length;
    }

    /*
    /**********************************************************************
    /* Handling of nested scope, state
    /**********************************************************************
     */

    protected final JsonToken _startArrayScope() throws IOException
    {
        _parsingContext = _parsingContext.createChildArrayContext(-1, -1);
        _majorState = MAJOR_ARRAY_ELEMENT;
        _majorStateAfterValue = MAJOR_ARRAY_ELEMENT;
        return (_currToken = JsonToken.START_ARRAY);
    }

    protected final JsonToken _startObjectScope() throws IOException
    {
        _parsingContext = _parsingContext.createChildObjectContext(-1, -1);
        _majorState = MAJOR_OBJECT_FIELD;
        _majorStateAfterValue = MAJOR_OBJECT_FIELD;
        return (_currToken = JsonToken.START_OBJECT);
    }

    protected final JsonToken _closeArrayScope() throws IOException
    {
        if (!_parsingContext.inArray()) {
            _reportMismatchedEndMarker(']', '}');
        }
        JsonReadContext ctxt = _parsingContext.getParent();
        _parsingContext = ctxt;
        int st;
        if (ctxt.inObject()) {
            st = MAJOR_OBJECT_FIELD;
        } else if (ctxt.inArray()) {
            st = MAJOR_ARRAY_ELEMENT;
        } else {
            st = MAJOR_ROOT;
        }
        _majorState = st;
        _majorStateAfterValue = st;
        return (_currToken = JsonToken.END_ARRAY);
    }

    protected final JsonToken _closeObjectScope() throws IOException
    {
        if (!_parsingContext.inObject()) {
            _reportMismatchedEndMarker('}', ']');
        }
        JsonReadContext ctxt = _parsingContext.getParent();
        _parsingContext = ctxt;
        int st;
        if (ctxt.inObject()) {
            st = MAJOR_OBJECT_FIELD;
        } else if (ctxt.inArray()) {
            st = MAJOR_ARRAY_ELEMENT;
        } else {
            st = MAJOR_ROOT;
        }
        _majorState = st;
        _majorStateAfterValue = st;
        return (_currToken = JsonToken.END_OBJECT);
    }

    /*
    /**********************************************************************
    /* Internal methods, field name parsing
    /**********************************************************************
     */

    // Helper method for trying to find specified encoded UTF-8 byte sequence
    // from symbol table; if successful avoids actual decoding to String
    protected final String _findDecodedFromSymbols(byte[] inBuf, int inPtr, int len) throws IOException
    {
        // First: maybe we already have this name decoded?
        if (len < 5) {
            int q = inBuf[inPtr] & 0xFF;
            if (--len > 0) {
                q = (q << 8) + (inBuf[++inPtr] & 0xFF);
                if (--len > 0) {
                    q = (q << 8) + (inBuf[++inPtr] & 0xFF);
                    if (--len > 0) {
                        q = (q << 8) + (inBuf[++inPtr] & 0xFF);
                    }
                }
            }
            _quad1 = q;
            return _symbols.findName(q);
        }
        if (len < 9) {
            // First quadbyte is easy
            int q1 = (inBuf[inPtr] & 0xFF) << 8;
            q1 += (inBuf[++inPtr] & 0xFF);
            q1 <<= 8;
            q1 += (inBuf[++inPtr] & 0xFF);
            q1 <<= 8;
            q1 += (inBuf[++inPtr] & 0xFF);
            int q2 = (inBuf[++inPtr] & 0xFF);
            len -= 5;
            if (len > 0) {
                q2 = (q2 << 8) + (inBuf[++inPtr] & 0xFF);
                if (--len > 0) {
                    q2 = (q2 << 8) + (inBuf[++inPtr] & 0xFF);
                    if (--len > 0) {
                        q2 = (q2 << 8) + (inBuf[++inPtr] & 0xFF);
                    }
                }
            }
            _quad1 = q1;
            _quad2 = q2;
            return _symbols.findName(q1, q2);
        }
        return _findDecodedLonger(inBuf, inPtr, len);
    }
    
    // Method for locating names longer than 8 bytes (in UTF-8)
    private final String _findDecodedLonger(byte[] inBuf, int inPtr, int len) throws IOException
    {
        // first, need enough buffer to store bytes as ints:
        {
            int bufLen = (len + 3) >> 2;
            if (bufLen > _quadBuffer.length) {
                _quadBuffer = Arrays.copyOf(_quadBuffer, bufLen+4);
            }
        }
        // then decode, full quads first
        int offset = 0;
        do {
            int q = (inBuf[inPtr++] & 0xFF) << 8;
            q |= inBuf[inPtr++] & 0xFF;
            q <<= 8;
            q |= inBuf[inPtr++] & 0xFF;
            q <<= 8;
            q |= inBuf[inPtr++] & 0xFF;
            _quadBuffer[offset++] = q;
        } while ((len -= 4) > 3);
        // and then leftovers
        if (len > 0) {
            int q = inBuf[inPtr] & 0xFF;
            if (--len > 0) {
                q = (q << 8) + (inBuf[++inPtr] & 0xFF);
                if (--len > 0) {
                    q = (q << 8) + (inBuf[++inPtr] & 0xFF);
                }
            }
            _quadBuffer[offset++] = q;
        }
        return _symbols.findName(_quadBuffer, offset);
    }

    protected final String _addDecodedToSymbols(int len, String name)
    {
        if (len < 5) {
            return _symbols.addName(name, _quad1);
        }
        if (len < 9) {
            return _symbols.addName(name, _quad1, _quad2);
        }
        int qlen = (len + 3) >> 2;
        return _symbols.addName(name, _quadBuffer, qlen);
    }

    /*
    /**********************************************************************
    /* Internal methods, state changes
    /**********************************************************************
     */
    
    /**
     * Helper method called at point when all input has been exhausted and
     * input feeder has indicated no more input will be forthcoming.
     */
    protected final JsonToken _eofAsNextToken() throws IOException {
        _majorState = MAJOR_CLOSED;
        if (!_parsingContext.inRoot()) {
            _handleEOF();
        }
        close();
        return (_currToken = null);
    }

    protected final JsonToken _valueComplete(JsonToken t) throws IOException
    {
        _majorState = _majorStateAfterValue;
        _currToken = t;
        return t;
    }

    /*
    /**********************************************************************
    /* Internal methods, error reporting
    /**********************************************************************
     */

    protected void _reportInvalidInitial(int mask) throws JsonParseException {
        _reportError("Invalid UTF-8 start byte 0x"+Integer.toHexString(mask));
    }
	
    protected void _reportInvalidOther(int mask, int ptr) throws JsonParseException {
        _inputPtr = ptr;
        _reportError("Invalid UTF-8 middle byte 0x"+Integer.toHexString(mask));
    }
}
