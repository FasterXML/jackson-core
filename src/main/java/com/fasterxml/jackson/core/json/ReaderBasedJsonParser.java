package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.core.util.*;

import static com.fasterxml.jackson.core.JsonTokenId.*;

/**
 * This is a concrete implementation of {@link JsonParser}, which is
 * based on a {@link java.io.Reader} to handle low-level character
 * conversion tasks.
 */
public class ReaderBasedJsonParser // final in 2.3, earlier
    extends ParserBase
{
    // Latin1 encoding is not supported, but we do use 8-bit subset for
    // pre-processing task, to simplify first pass, keep it fast.
    protected final static int[] _icLatin1 = CharTypes.getInputCodeLatin1();

    /*
    /**********************************************************
    /* Input configuration
    /**********************************************************
     */

    /**
     * Reader that can be used for reading more content, if one
     * buffer from input source, but in some cases pre-loaded buffer
     * is handed to the parser.
     */
    protected Reader _reader;

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source.
     */
    protected char[] _inputBuffer;

    /**
     * Flag that indicates whether the input buffer is recycable (and
     * needs to be returned to recycler once we are done) or not.
     *<p>
     * If it is not, it also means that parser can NOT modify underlying
     * buffer.
     */
    protected boolean _bufferRecyclable;
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected ObjectCodec _objectCodec;

    final protected CharsToNameCanonicalizer _symbols;
    
    final protected int _hashSeed;

    /*
    /**********************************************************
    /* Parsing state
    /**********************************************************
     */
    
    /**
     * Flag that indicates that the current token has not yet
     * been fully processed, and needs to be finished for
     * some access (or skipped to obtain the next token)
     */
    protected boolean _tokenIncomplete = false;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Method called when caller wants to provide input buffer directly,
     * and it may or may not be recyclable use standard recycle context.
     * 
     * @since 2.4
     */
    public ReaderBasedJsonParser(IOContext ctxt, int features, Reader r,
            ObjectCodec codec, CharsToNameCanonicalizer st,
            char[] inputBuffer, int start, int end,
            boolean bufferRecyclable)
    {
        super(ctxt, features);
        _reader = r;
        _inputBuffer = inputBuffer;
        _inputPtr = start;
        _inputEnd = end;
        _objectCodec = codec;
        _symbols = st;
        _hashSeed = st.hashSeed();
        _bufferRecyclable = bufferRecyclable;
    }

    /**
     * Method called when input comes as a {@link java.io.Reader}, and buffer allocation
     * can be done using default mechanism.
     */
    public ReaderBasedJsonParser(IOContext ctxt, int features, Reader r,
        ObjectCodec codec, CharsToNameCanonicalizer st)
    {
        super(ctxt, features);
        _reader = r;
        _inputBuffer = ctxt.allocTokenBuffer();
        _inputPtr = 0;
        _inputEnd = 0;
        _objectCodec = codec;
        _symbols = st;
        _hashSeed = st.hashSeed();
        _bufferRecyclable = true;
    }

    /*
    /**********************************************************
    /* Base method defs, overrides
    /**********************************************************
     */

    @Override public ObjectCodec getCodec() { return _objectCodec; }
    @Override public void setCodec(ObjectCodec c) { _objectCodec = c; }
    
    @Override
    public int releaseBuffered(Writer w) throws IOException {
        int count = _inputEnd - _inputPtr;
        if (count < 1) { return 0; }
        // let's just advance ptr to end
        int origPtr = _inputPtr;
        w.write(_inputBuffer, origPtr, count);
        return count;
    }

    @Override public Object getInputSource() { return _reader; }

    @Override
    protected boolean loadMore() throws IOException
    {
        _currInputProcessed += _inputEnd;
        _currInputRowStart -= _inputEnd;

        if (_reader != null) {
            int count = _reader.read(_inputBuffer, 0, _inputBuffer.length);
            if (count > 0) {
                _inputPtr = 0;
                _inputEnd = count;
                return true;
            }
            // End of input
            _closeInput();
            // Should never return 0, so let's fail
            if (count == 0) {
                throw new IOException("Reader returned 0 characters when trying to read "+_inputEnd);
            }
        }
        return false;
    }

    protected char getNextChar(String eofMsg) throws IOException {
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) { _reportInvalidEOF(eofMsg); }
        }
        return _inputBuffer[_inputPtr++];
    }

    @Override
    protected void _closeInput() throws IOException {
        /* 25-Nov-2008, tatus: As per [JACKSON-16] we are not to call close()
         *   on the underlying Reader, unless we "own" it, or auto-closing
         *   feature is enabled.
         *   One downside is that when using our optimized
         *   Reader (granted, we only do that for UTF-32...) this
         *   means that buffer recycling won't work correctly.
         */
        if (_reader != null) {
            if (_ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_SOURCE)) {
                _reader.close();
            }
            _reader = null;
        }
    }

    /**
     * Method called to release internal buffers owned by the base
     * reader. This may be called along with {@link #_closeInput} (for
     * example, when explicitly closing this reader instance), or
     * separately (if need be).
     */
    @Override
    protected void _releaseBuffers() throws IOException {
        super._releaseBuffers();
        // merge new symbols, if any
        _symbols.release();
        // and release buffers, if they are recyclable ones
        if (_bufferRecyclable) {
            char[] buf = _inputBuffer;
            if (buf != null) {
                _inputBuffer = null;
                _ioContext.releaseTokenBuffer(buf);
            }
        }
    }
    
    /*
    /**********************************************************
    /* Public API, data access
    /**********************************************************
     */
    
    /**
     * Method for accessing textual representation of the current event;
     * if no current event (before first call to {@link #nextToken}, or
     * after encountering end-of-input), returns null.
     * Method can be called for any event.
     */
    @Override
    public final String getText() throws IOException
    {
        JsonToken t = _currToken;
        if (t == JsonToken.VALUE_STRING) {
            if (_tokenIncomplete) {
                _tokenIncomplete = false;
                _finishString(); // only strings can be incomplete
            }
            return _textBuffer.contentsAsString();
        }
        return _getText2(t);
    }

    // // // Let's override default impls for improved performance
    
    // @since 2.1
    @Override
    public final String getValueAsString() throws IOException
    {
        if (_currToken == JsonToken.VALUE_STRING) {
            if (_tokenIncomplete) {
                _tokenIncomplete = false;
                _finishString(); // only strings can be incomplete
            }
            return _textBuffer.contentsAsString();
        }
        return super.getValueAsString(null);
    }
    
    // @since 2.1
    @Override
    public final String getValueAsString(String defValue) throws IOException {
        if (_currToken == JsonToken.VALUE_STRING) {
            if (_tokenIncomplete) {
                _tokenIncomplete = false;
                _finishString(); // only strings can be incomplete
            }
            return _textBuffer.contentsAsString();
        }
        return super.getValueAsString(defValue);
    }

    protected final String _getText2(JsonToken t) {
        if (t == null) {
            return null;
        }
        switch (t.id()) {
        case ID_FIELD_NAME:
            return _parsingContext.getCurrentName();

        case ID_STRING:
            // fall through
        case ID_NUMBER_INT:
        case ID_NUMBER_FLOAT:
            return _textBuffer.contentsAsString();
        default:
            return t.asString();
        }
    }

    @Override
    public final char[] getTextCharacters() throws IOException
    {
        if (_currToken != null) { // null only before/after document
            switch (_currToken.id()) {
            case ID_FIELD_NAME:
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
    
            case ID_STRING:
                if (_tokenIncomplete) {
                    _tokenIncomplete = false;
                    _finishString(); // only strings can be incomplete
                }
                // fall through
            case ID_NUMBER_INT:
            case ID_NUMBER_FLOAT:
                return _textBuffer.getTextBuffer();
                
            default:
                return _currToken.asCharArray();
            }
        }
        return null;
    }

    @Override
    public final int getTextLength() throws IOException
    {
        if (_currToken != null) { // null only before/after document
            switch (_currToken.id()) {
                
            case ID_FIELD_NAME:
                return _parsingContext.getCurrentName().length();
            case ID_STRING:
                if (_tokenIncomplete) {
                    _tokenIncomplete = false;
                    _finishString(); // only strings can be incomplete
                }
                // fall through
            case ID_NUMBER_INT:
            case ID_NUMBER_FLOAT:
                return _textBuffer.size();
                
            default:
                return _currToken.asCharArray().length;
            }
        }
        return 0;
    }

    @Override
    public final int getTextOffset() throws IOException
    {
        // Most have offset of 0, only some may have other values:
        if (_currToken != null) {
            switch (_currToken.id()) {
            case ID_FIELD_NAME:
                return 0;
            case ID_STRING:
                if (_tokenIncomplete) {
                    _tokenIncomplete = false;
                    _finishString(); // only strings can be incomplete
                }
                // fall through
            case ID_NUMBER_INT:
            case ID_NUMBER_FLOAT:
                return _textBuffer.getTextOffset();
            default:
            }
        }
        return 0;
    }

    @Override
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException
    {
        if (_currToken != JsonToken.VALUE_STRING &&
                (_currToken != JsonToken.VALUE_EMBEDDED_OBJECT || _binaryValue == null)) {
            _reportError("Current token ("+_currToken+") not VALUE_STRING or VALUE_EMBEDDED_OBJECT, can not access as binary");
        }
        /* To ensure that we won't see inconsistent data, better clear up
         * state...
         */
        if (_tokenIncomplete) {
            try {
                _binaryValue = _decodeBase64(b64variant);
            } catch (IllegalArgumentException iae) {
                throw _constructError("Failed to decode VALUE_STRING as base64 ("+b64variant+"): "+iae.getMessage());
            }
            /* let's clear incomplete only now; allows for accessing other
             * textual content in error cases
             */
            _tokenIncomplete = false;
        } else { // may actually require conversion...
            if (_binaryValue == null) {
                @SuppressWarnings("resource")
                ByteArrayBuilder builder = _getByteArrayBuilder();
                _decodeBase64(getText(), builder, b64variant);
                _binaryValue = builder.toByteArray();
            }
        }
        return _binaryValue;
    }
    
    @Override
    public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException
    {
        // if we have already read the token, just use whatever we may have
        if (!_tokenIncomplete || _currToken != JsonToken.VALUE_STRING) {
            byte[] b = getBinaryValue(b64variant);
            out.write(b);
            return b.length;
        }
        // otherwise do "real" incremental parsing...
        byte[] buf = _ioContext.allocBase64Buffer();
        try {
            return _readBinary(b64variant, out, buf);
        } finally {
            _ioContext.releaseBase64Buffer(buf);
        }
    }

    protected int _readBinary(Base64Variant b64variant, OutputStream out, byte[] buffer) throws IOException
    {
        int outputPtr = 0;
        final int outputEnd = buffer.length - 3;
        int outputCount = 0;

        while (true) {
            // first, we'll skip preceding white space, if any
            char ch;
            do {
                if (_inputPtr >= _inputEnd) {
                    loadMoreGuaranteed();
                }
                ch = _inputBuffer[_inputPtr++];
            } while (ch <= INT_SPACE);
            int bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) { // reached the end, fair and square?
                if (ch == '"') {
                    break;
                }
                bits = _decodeBase64Escape(b64variant, ch, 0);
                if (bits < 0) { // white space to skip
                    continue;
                }
            }

            // enough room? If not, flush
            if (outputPtr > outputEnd) {
                outputCount += outputPtr;
                out.write(buffer, 0, outputPtr);
                outputPtr = 0;
            }

            int decodedData = bits;

            // then second base64 char; can't get padding yet, nor ws

            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            ch = _inputBuffer[_inputPtr++];
            bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                bits = _decodeBase64Escape(b64variant, ch, 1);
            }
            decodedData = (decodedData << 6) | bits;

            // third base64 char; can be padding, but not ws
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            ch = _inputBuffer[_inputPtr++];
            bits = b64variant.decodeBase64Char(ch);

            // First branch: can get padding (-> 1 byte)
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    // as per [JACKSON-631], could also just be 'missing'  padding
                    if (ch == '"' && !b64variant.usesPadding()) {
                        decodedData >>= 4;
                        buffer[outputPtr++] = (byte) decodedData;
                        break;
                    }
                    bits = _decodeBase64Escape(b64variant, ch, 2);
                }
                if (bits == Base64Variant.BASE64_VALUE_PADDING) {
                    // Ok, must get padding
                    if (_inputPtr >= _inputEnd) {
                        loadMoreGuaranteed();
                    }
                    ch = _inputBuffer[_inputPtr++];
                    if (!b64variant.usesPaddingChar(ch)) {
                        throw reportInvalidBase64Char(b64variant, ch, 3, "expected padding character '"+b64variant.getPaddingChar()+"'");
                    }
                    // Got 12 bits, only need 8, need to shift
                    decodedData >>= 4;
                    buffer[outputPtr++] = (byte) decodedData;
                    continue;
                }
            }
            // Nope, 2 or 3 bytes
            decodedData = (decodedData << 6) | bits;
            // fourth and last base64 char; can be padding, but not ws
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            ch = _inputBuffer[_inputPtr++];
            bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    // as per [JACKSON-631], could also just be 'missing'  padding
                    if (ch == '"' && !b64variant.usesPadding()) {
                        decodedData >>= 2;
                        buffer[outputPtr++] = (byte) (decodedData >> 8);
                        buffer[outputPtr++] = (byte) decodedData;
                        break;
                    }
                    bits = _decodeBase64Escape(b64variant, ch, 3);
                }
                if (bits == Base64Variant.BASE64_VALUE_PADDING) {
                    /* With padding we only get 2 bytes; but we have
                     * to shift it a bit so it is identical to triplet
                     * case with partial output.
                     * 3 chars gives 3x6 == 18 bits, of which 2 are
                     * dummies, need to discard:
                     */
                    decodedData >>= 2;
                    buffer[outputPtr++] = (byte) (decodedData >> 8);
                    buffer[outputPtr++] = (byte) decodedData;
                    continue;
                }
            }
            // otherwise, our triplet is now complete
            decodedData = (decodedData << 6) | bits;
            buffer[outputPtr++] = (byte) (decodedData >> 16);
            buffer[outputPtr++] = (byte) (decodedData >> 8);
            buffer[outputPtr++] = (byte) decodedData;
        }
        _tokenIncomplete = false;
        if (outputPtr > 0) {
            outputCount += outputPtr;
            out.write(buffer, 0, outputPtr);
        }
        return outputCount;
    }

    /*
   /**********************************************************
   /* Public API, traversal
   /**********************************************************
    */

    /**
     * @return Next token from the stream, if any found, or null
     *   to indicate end-of-input
     */
    @Override
    public final JsonToken nextToken() throws IOException
    {
        _numTypesValid = NR_UNKNOWN;

        /* First: field names are special -- we will always tokenize
         * (part of) value along with field name to simplify
         * state handling. If so, can and need to use secondary token:
         */
        if (_currToken == JsonToken.FIELD_NAME) {
            return _nextAfterName();
        }
        if (_tokenIncomplete) {
            _skipString(); // only strings can be partial
        }
        int i = _skipWSOrEnd();
        if (i < 0) { // end-of-input
            /* 19-Feb-2009, tatu: Should actually close/release things
             *    like input source, symbol table and recyclable buffers now.
             */
            close();
            return (_currToken = null);
        }

        /* First, need to ensure we know the starting location of token
         * after skipping leading white space
         */
        _tokenInputTotal = _currInputProcessed + _inputPtr - 1;
        _tokenInputRow = _currInputRow;
        _tokenInputCol = _inputPtr - _currInputRowStart - 1;

        // finally: clear any data retained so far
        _binaryValue = null;

        // Closing scope?
        if (i == INT_RBRACKET) {
            if (!_parsingContext.inArray()) {
                _reportMismatchedEndMarker(i, '}');
            }
            _parsingContext = _parsingContext.getParent();
            return (_currToken = JsonToken.END_ARRAY);
        }
        if (i == INT_RCURLY) {
            if (!_parsingContext.inObject()) {
                _reportMismatchedEndMarker(i, ']');
            }
            _parsingContext = _parsingContext.getParent();
            return (_currToken = JsonToken.END_OBJECT);
        }

        // Nope: do we then expect a comma?
        if (_parsingContext.expectComma()) {
            i = _skipComma(i);
        }

        /* And should we now have a name? Always true for
         * Object contexts, since the intermediate 'expect-value'
         * state is never retained.
         */
        boolean inObject = _parsingContext.inObject();
        if (inObject) {
           // First, field name itself:
            String name = (i == INT_QUOTE) ? _parseName() : _handleOddName(i);
            _parsingContext.setCurrentName(name);
            _currToken = JsonToken.FIELD_NAME;
            i = _skipColon();
        }

        // Ok: we must have a value... what is it?

        JsonToken t;

        switch (i) {
        case '"':
            _tokenIncomplete = true;
            t = JsonToken.VALUE_STRING;
            break;
        case '[':
            if (!inObject) {
                _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
            }
            t = JsonToken.START_ARRAY;
            break;
        case '{':
            if (!inObject) {
                _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
            }
            t = JsonToken.START_OBJECT;
            break;
        case ']':
        case '}':
            // Error: neither is valid at this point; valid closers have
            // been handled earlier
            _reportUnexpectedChar(i, "expected a value");
        case 't':
            _matchTrue();
            t = JsonToken.VALUE_TRUE;
            break;
        case 'f':
            _matchFalse();
            t = JsonToken.VALUE_FALSE;
            break;
        case 'n':
            _matchNull();
            t = JsonToken.VALUE_NULL;
            break;

        case '-':
            /* Should we have separate handling for plus? Although
             * it is not allowed per se, it may be erroneously used,
             * and could be indicate by a more specific error message.
             */
            t = _parseNegNumber();
            break;
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
            t = _parsePosNumber(i);
            break;
        default:
            t = _handleOddValue(i);
            break;
        }

        if (inObject) {
            _nextToken = t;
            return _currToken;
        }
        _currToken = t;
        return t;
    }

    private final JsonToken _nextAfterName()
    {
        _nameCopied = false; // need to invalidate if it was copied
        JsonToken t = _nextToken;
        _nextToken = null;
        // Also: may need to start new context?
        if (t == JsonToken.START_ARRAY) {
            _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
        } else if (t == JsonToken.START_OBJECT) {
            _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
        }
        return (_currToken = t);
    }

    /*
    @Override
    public boolean nextFieldName(SerializableString str)
         throws IOException
     */

    // note: identical to one in UTF8StreamJsonParser
    @Override
    public final String nextTextValue() throws IOException
    {
        if (_currToken == JsonToken.FIELD_NAME) { // mostly copied from '_nextAfterName'
            _nameCopied = false;
            JsonToken t = _nextToken;
            _nextToken = null;
            _currToken = t;
            if (t == JsonToken.VALUE_STRING) {
                if (_tokenIncomplete) {
                    _tokenIncomplete = false;
                    _finishString();
                }
                return _textBuffer.contentsAsString();
            }
            if (t == JsonToken.START_ARRAY) {
                _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
            } else if (t == JsonToken.START_OBJECT) {
                _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
            }
            return null;
        }
        // !!! TODO: optimize this case as well
        return (nextToken() == JsonToken.VALUE_STRING) ? getText() : null;
    }

    // note: identical to one in Utf8StreamParser
    @Override
    public final int nextIntValue(int defaultValue) throws IOException
    {
        if (_currToken == JsonToken.FIELD_NAME) {
            _nameCopied = false;
            JsonToken t = _nextToken;
            _nextToken = null;
            _currToken = t;
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return getIntValue();
            }
            if (t == JsonToken.START_ARRAY) {
                _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
            } else if (t == JsonToken.START_OBJECT) {
                _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
            }
            return defaultValue;
        }
        // !!! TODO: optimize this case as well
        return (nextToken() == JsonToken.VALUE_NUMBER_INT) ? getIntValue() : defaultValue;
    }

    // note: identical to one in Utf8StreamParser
    @Override
    public final long nextLongValue(long defaultValue) throws IOException
    {
        if (_currToken == JsonToken.FIELD_NAME) { // mostly copied from '_nextAfterName'
            _nameCopied = false;
            JsonToken t = _nextToken;
            _nextToken = null;
            _currToken = t;
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return getLongValue();
            }
            if (t == JsonToken.START_ARRAY) {
                _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
            } else if (t == JsonToken.START_OBJECT) {
                _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
            }
            return defaultValue;
        }
        // !!! TODO: optimize this case as well
        return (nextToken() == JsonToken.VALUE_NUMBER_INT) ? getLongValue() : defaultValue;
    }

    // note: identical to one in UTF8StreamJsonParser
    @Override
    public final Boolean nextBooleanValue() throws IOException
    {
        if (_currToken == JsonToken.FIELD_NAME) { // mostly copied from '_nextAfterName'
            _nameCopied = false;
            JsonToken t = _nextToken;
            _nextToken = null;
            _currToken = t;
            if (t == JsonToken.VALUE_TRUE) {
                return Boolean.TRUE;
            }
            if (t == JsonToken.VALUE_FALSE) {
                return Boolean.FALSE;
            }
            if (t == JsonToken.START_ARRAY) {
                _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
            } else if (t == JsonToken.START_OBJECT) {
                _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
            }
            return null;
        }
        JsonToken t = nextToken();
        if (t != null) {
            int id = t.id();
            if (id == ID_TRUE) return Boolean.TRUE;
            if (id == ID_FALSE) return Boolean.FALSE;
        }
        return null;
    }

    /*
    /**********************************************************
    /* Internal methods, number parsing
    /**********************************************************
     */

    /**
     * Initial parsing method for number values. It needs to be able
     * to parse enough input to be able to determine whether the
     * value is to be considered a simple integer value, or a more
     * generic decimal value: latter of which needs to be expressed
     * as a floating point number. The basic rule is that if the number
     * has no fractional or exponential part, it is an integer; otherwise
     * a floating point number.
     *<p>
     * Because much of input has to be processed in any case, no partial
     * parsing is done: all input text will be stored for further
     * processing. However, actual numeric value conversion will be
     * deferred, since it is usually the most complicated and costliest
     * part of processing.
     */
    protected final JsonToken _parsePosNumber(int ch) throws IOException
    {
        /* Although we will always be complete with respect to textual
         * representation (that is, all characters will be parsed),
         * actual conversion to a number is deferred. Thus, need to
         * note that no representations are valid yet
         */
        int ptr = _inputPtr;
        int startPtr = ptr-1; // to include digit already read
        final int inputLen = _inputEnd;

        // One special case, leading zero(es):
        if (ch == INT_0) {
            return _parseNumber2(false, startPtr);
        }
            
        /* First, let's see if the whole number is contained within
         * the input buffer unsplit. This should be the common case;
         * and to simplify processing, we will just reparse contents
         * in the alternative case (number split on buffer boundary)
         */
        
        int intLen = 1; // already got one
        
        // First let's get the obligatory integer part:
        int_loop:
        while (true) {
            if (ptr >= inputLen) {
                _inputPtr = startPtr;
                return _parseNumber2(false, startPtr);
            }
            ch = (int) _inputBuffer[ptr++];
            if (ch < INT_0 || ch > INT_9) {
                break int_loop;
            }
            ++intLen;
        }
        if (ch == INT_PERIOD || ch == INT_e || ch == INT_E) {
            _inputPtr = ptr;
            return _parseFloat(ch, startPtr, ptr, false, intLen);
        }
        // Got it all: let's add to text buffer for parsing, access
        --ptr; // need to push back following separator
        _inputPtr = ptr;
        // As per #105, need separating space between root values; check here
        if (_parsingContext.inRoot()) {
            _verifyRootSpace(ch);
        }
        int len = ptr-startPtr;
        _textBuffer.resetWithShared(_inputBuffer, startPtr, len);
        return resetInt(false, intLen);
    }

    private final JsonToken _parseFloat(int ch, int startPtr, int ptr, boolean neg, int intLen)
        throws IOException
    {
        final int inputLen = _inputEnd;
        int fractLen = 0;

        // And then see if we get other parts
        if (ch == '.') { // yes, fraction
            fract_loop:
            while (true) {
                if (ptr >= inputLen) {
                    return _parseNumber2(neg, startPtr);
                }
                ch = (int) _inputBuffer[ptr++];
                if (ch < INT_0 || ch > INT_9) {
                    break fract_loop;
                }
                ++fractLen;
            }
            // must be followed by sequence of ints, one minimum
            if (fractLen == 0) {
                reportUnexpectedNumberChar(ch, "Decimal point not followed by a digit");
            }
        }
        int expLen = 0;
        if (ch == 'e' || ch == 'E') { // and/or exponent
            if (ptr >= inputLen) {
                _inputPtr = startPtr;
                return _parseNumber2(neg, startPtr);
            }
            // Sign indicator?
            ch = (int) _inputBuffer[ptr++];
            if (ch == INT_MINUS || ch == INT_PLUS) { // yup, skip for now
                if (ptr >= inputLen) {
                    _inputPtr = startPtr;
                    return _parseNumber2(neg, startPtr);
                }
                ch = (int) _inputBuffer[ptr++];
            }
            while (ch <= INT_9 && ch >= INT_0) {
                ++expLen;
                if (ptr >= inputLen) {
                    _inputPtr = startPtr;
                    return _parseNumber2(neg, startPtr);
                }
                ch = (int) _inputBuffer[ptr++];
            }
            // must be followed by sequence of ints, one minimum
            if (expLen == 0) {
                reportUnexpectedNumberChar(ch, "Exponent indicator not followed by a digit");
            }
        }
        --ptr; // need to push back following separator
        _inputPtr = ptr;
        // As per #105, need separating space between root values; check here
        if (_parsingContext.inRoot()) {
            _verifyRootSpace(ch);
        }
        int len = ptr-startPtr;
        _textBuffer.resetWithShared(_inputBuffer, startPtr, len);
        // And there we have it!
        return resetFloat(neg, intLen, fractLen, expLen);
    }

    protected final JsonToken _parseNegNumber() throws IOException
    {
        int ptr = _inputPtr;
        int startPtr = ptr-1; // to include sign/digit already read
        final int inputLen = _inputEnd;

        if (ptr >= inputLen) {
            return _parseNumber2(true, startPtr);
        }
        int ch = _inputBuffer[ptr++];
        // First check: must have a digit to follow minus sign
        if (ch > INT_9 || ch < INT_0) {
            _inputPtr = ptr;
            return _handleInvalidNumberStart(ch, true);
        }
        // One special case, leading zero(es):
        if (ch == INT_0) {
            return _parseNumber2(true, startPtr);
        }
        int intLen = 1; // already got one
        
        // First let's get the obligatory integer part:
        int_loop:
        while (true) {
            if (ptr >= inputLen) {
                return _parseNumber2(true, startPtr);
            }
            ch = (int) _inputBuffer[ptr++];
            if (ch < INT_0 || ch > INT_9) {
                break int_loop;
            }
            ++intLen;
        }

        if (ch == INT_PERIOD || ch == INT_e || ch == INT_E) {
            _inputPtr = ptr;
            return _parseFloat(ch, startPtr, ptr, true, intLen);
        }
        --ptr;
        _inputPtr = ptr;
        if (_parsingContext.inRoot()) {
            _verifyRootSpace(ch);
        }
        int len = ptr-startPtr;
        _textBuffer.resetWithShared(_inputBuffer, startPtr, len);
        return resetInt(true, intLen);
    }

    /**
     * Method called to parse a number, when the primary parse
     * method has failed to parse it, due to it being split on
     * buffer boundary. As a result code is very similar, except
     * that it has to explicitly copy contents to the text buffer
     * instead of just sharing the main input buffer.
     */
    private final JsonToken _parseNumber2(boolean neg, int startPtr) throws IOException
    {
        _inputPtr = neg ? (startPtr+1) : startPtr;
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int outPtr = 0;

        // Need to prepend sign?
        if (neg) {
            outBuf[outPtr++] = '-';
        }

        // This is the place to do leading-zero check(s) too:
        int intLen = 0;
        char c = (_inputPtr < _inputEnd) ? _inputBuffer[_inputPtr++] : getNextChar("No digit following minus sign");
        if (c == '0') {
            c = _verifyNoLeadingZeroes();
        }
        boolean eof = false;

        // Ok, first the obligatory integer part:
        int_loop:
        while (c >= '0' && c <= '9') {
            ++intLen;
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            outBuf[outPtr++] = c;
            if (_inputPtr >= _inputEnd && !loadMore()) {
                // EOF is legal for main level int values
                c = CHAR_NULL;
                eof = true;
                break int_loop;
            }
            c = _inputBuffer[_inputPtr++];
        }
        // Also, integer part is not optional
        if (intLen == 0) {
            return _handleInvalidNumberStart(c, neg);
        }

        int fractLen = 0;
        // And then see if we get other parts
        if (c == '.') { // yes, fraction
            outBuf[outPtr++] = c;

            fract_loop:
            while (true) {
                if (_inputPtr >= _inputEnd && !loadMore()) {
                    eof = true;
                    break fract_loop;
                }
                c = _inputBuffer[_inputPtr++];
                if (c < INT_0 || c > INT_9) {
                    break fract_loop;
                }
                ++fractLen;
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outBuf[outPtr++] = c;
            }
            // must be followed by sequence of ints, one minimum
            if (fractLen == 0) {
                reportUnexpectedNumberChar(c, "Decimal point not followed by a digit");
            }
        }

        int expLen = 0;
        if (c == 'e' || c == 'E') { // exponent?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            outBuf[outPtr++] = c;
            // Not optional, can require that we get one more char
            c = (_inputPtr < _inputEnd) ? _inputBuffer[_inputPtr++]
                : getNextChar("expected a digit for number exponent");
            // Sign indicator?
            if (c == '-' || c == '+') {
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outBuf[outPtr++] = c;
                // Likewise, non optional:
                c = (_inputPtr < _inputEnd) ? _inputBuffer[_inputPtr++]
                    : getNextChar("expected a digit for number exponent");
            }

            exp_loop:
            while (c <= INT_9 && c >= INT_0) {
                ++expLen;
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outBuf[outPtr++] = c;
                if (_inputPtr >= _inputEnd && !loadMore()) {
                    eof = true;
                    break exp_loop;
                }
                c = _inputBuffer[_inputPtr++];
            }
            // must be followed by sequence of ints, one minimum
            if (expLen == 0) {
                reportUnexpectedNumberChar(c, "Exponent indicator not followed by a digit");
            }
        }

        // Ok; unless we hit end-of-input, need to push last char read back
        if (!eof) {
            --_inputPtr;
            if (_parsingContext.inRoot()) {
                _verifyRootSpace(c);
            }
        }
        _textBuffer.setCurrentLength(outPtr);
        // And there we have it!
        return reset(neg, intLen, fractLen, expLen);
    }

    /**
     * Method called when we have seen one zero, and want to ensure
     * it is not followed by another
     */
    private final char _verifyNoLeadingZeroes() throws IOException
    {
        // Fast case first:
        if (_inputPtr < _inputEnd) {
            char ch = _inputBuffer[_inputPtr];
            // if not followed by a number (probably '.'); return zero as is, to be included
            if (ch < '0' || ch > '9') {
                return '0';
            }
        }
        // and offline the less common case
        return _verifyNLZ2();
    }
        
    private char _verifyNLZ2() throws IOException
    {
        if (_inputPtr >= _inputEnd && !loadMore()) {
            return '0';
        }
        char ch = _inputBuffer[_inputPtr];
        if (ch < '0' || ch > '9') {
            return '0';
        }
        if (!isEnabled(Feature.ALLOW_NUMERIC_LEADING_ZEROS)) {
            reportInvalidNumber("Leading zeroes not allowed");
        }
        // if so, just need to skip either all zeroes (if followed by number); or all but one (if non-number)
        ++_inputPtr; // Leading zero to be skipped
        if (ch == INT_0) {
            while (_inputPtr < _inputEnd || loadMore()) {
                ch = _inputBuffer[_inputPtr];
                if (ch < '0' || ch > '9') { // followed by non-number; retain one zero
                    return '0';
                }
                ++_inputPtr; // skip previous zero
                if (ch != '0') { // followed by other number; return 
                    break;
                }
            }
        }
        return ch;
    }

    /**
     * Method called if expected numeric value (due to leading sign) does not
     * look like a number
     */
    protected JsonToken _handleInvalidNumberStart(int ch, boolean negative) throws IOException
    {
        if (ch == 'I') {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) { _reportInvalidEOFInValue(); }
            }
            ch = _inputBuffer[_inputPtr++];
            if (ch == 'N') {
                String match = negative ? "-INF" :"+INF";
                _matchToken(match, 3);
                if (isEnabled(Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
                    return resetAsNaN(match, negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                }
                _reportError("Non-standard token '"+match+"': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow");
            } else if (ch == 'n') {
                String match = negative ? "-Infinity" :"+Infinity";
                _matchToken(match, 3);
                if (isEnabled(Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
                    return resetAsNaN(match, negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                }
                _reportError("Non-standard token '"+match+"': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow");
            }
        }
        reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        return null;
    }

    /**
     * Method called to ensure that a root-value is followed by a space
     * token.
     *<p>
     * NOTE: caller MUST ensure there is at least one character available;
     * and that input pointer is AT given char (not past)
     */
    private final void _verifyRootSpace(int ch) throws IOException
    {
        // caller had pushed it back, before calling; reset
        ++_inputPtr;
        switch (ch) {
        case ' ':
        case '\t':
            return;
        case '\r':
            _skipCR();
            return;
        case '\n':
            ++_currInputRow;
            _currInputRowStart = _inputPtr;
            return;
        }
        _reportMissingRootWS(ch);
    }
    
    /*
    /**********************************************************
    /* Internal methods, secondary parsing
    /**********************************************************
     */

    protected final String _parseName() throws IOException
    {
        // First: let's try to see if we have a simple name: one that does
        // not cross input buffer boundary, and does not contain escape sequences.
        int ptr = _inputPtr;
        int hash = _hashSeed;
        final int[] codes = _icLatin1;

        while (ptr < _inputEnd) {
            int ch = _inputBuffer[ptr];
            if (ch < codes.length && codes[ch] != 0) {
                if (ch == '"') {
                    int start = _inputPtr;
                    _inputPtr = ptr+1; // to skip the quote
                    return _symbols.findSymbol(_inputBuffer, start, ptr - start, hash);
                }
                break;
            }
            hash = (hash * CharsToNameCanonicalizer.HASH_MULT) + ch;
            ++ptr;
        }
        int start = _inputPtr;
        _inputPtr = ptr;
        return _parseName2(start, hash, INT_QUOTE);
    }

    private String _parseName2(int startPtr, int hash, int endChar) throws IOException
    {
        _textBuffer.resetWithShared(_inputBuffer, startPtr, (_inputPtr - startPtr));

        /* Output pointers; calls will also ensure that the buffer is
         * not shared and has room for at least one more char.
         */
        char[] outBuf = _textBuffer.getCurrentSegment();
        int outPtr = _textBuffer.getCurrentSegmentSize();

        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(": was expecting closing '"+((char) endChar)+"' for name");
                }
            }
            char c = _inputBuffer[_inputPtr++];
            int i = (int) c;
            if (i <= INT_BACKSLASH) {
                if (i == INT_BACKSLASH) {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    c = _decodeEscaped();
                } else if (i <= endChar) {
                    if (i == endChar) {
                        break;
                    }
                    if (i < INT_SPACE) {
                        _throwUnquotedSpace(i, "name");
                    }
                }
            }
            hash = (hash * CharsToNameCanonicalizer.HASH_MULT) + i;
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;

            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
        }
        _textBuffer.setCurrentLength(outPtr);
        {
            TextBuffer tb = _textBuffer;
            char[] buf = tb.getTextBuffer();
            int start = tb.getTextOffset();
            int len = tb.size();

            return _symbols.findSymbol(buf, start, len, hash);
        }
    }

    /**
     * Method called when we see non-white space character other
     * than double quote, when expecting a field name.
     * In standard mode will just throw an expection; but
     * in non-standard modes may be able to parse name.
     */
    protected String _handleOddName(int i) throws IOException
    {
        // [JACKSON-173]: allow single quotes
        if (i == '\'' && isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
            return _parseAposName();
        }
        // [JACKSON-69]: allow unquoted names if feature enabled:
        if (!isEnabled(Feature.ALLOW_UNQUOTED_FIELD_NAMES)) {
            _reportUnexpectedChar(i, "was expecting double-quote to start field name");
        }
        final int[] codes = CharTypes.getInputCodeLatin1JsNames();
        final int maxCode = codes.length;

        // Also: first char must be a valid name char, but NOT be number
        boolean firstOk;

        if (i < maxCode) { // identifier, or a number ([Issue#102])
            firstOk = (codes[i] == 0);
        } else {
            firstOk = Character.isJavaIdentifierPart((char) i);
        }
        if (!firstOk) {
            _reportUnexpectedChar(i, "was expecting either valid name character (for unquoted name) or double-quote (for quoted) to start field name");
        }
        int ptr = _inputPtr;
        int hash = _hashSeed;
        final int inputLen = _inputEnd;

        if (ptr < inputLen) {
            do {
                int ch = _inputBuffer[ptr];
                if (ch < maxCode) {
                    if (codes[ch] != 0) {
                        int start = _inputPtr-1; // -1 to bring back first char
                        _inputPtr = ptr;
                        return _symbols.findSymbol(_inputBuffer, start, ptr - start, hash);
                    }
                } else if (!Character.isJavaIdentifierPart((char) ch)) {
                    int start = _inputPtr-1; // -1 to bring back first char
                    _inputPtr = ptr;
                    return _symbols.findSymbol(_inputBuffer, start, ptr - start, hash);
                }
                hash = (hash * CharsToNameCanonicalizer.HASH_MULT) + ch;
                ++ptr;
            } while (ptr < inputLen);
        }
        int start = _inputPtr-1;
        _inputPtr = ptr;
        return _handleOddName2(start, hash, codes);
    }

    protected String _parseAposName() throws IOException
    {
        // Note: mostly copy of_parseFieldName
        int ptr = _inputPtr;
        int hash = _hashSeed;
        final int inputLen = _inputEnd;

        if (ptr < inputLen) {
            final int[] codes = _icLatin1;
            final int maxCode = codes.length;

            do {
                int ch = _inputBuffer[ptr];
                if (ch == '\'') {
                    int start = _inputPtr;
                    _inputPtr = ptr+1; // to skip the quote
                    return _symbols.findSymbol(_inputBuffer, start, ptr - start, hash);
                }
                if (ch < maxCode && codes[ch] != 0) {
                    break;
                }
                hash = (hash * CharsToNameCanonicalizer.HASH_MULT) + ch;
                ++ptr;
            } while (ptr < inputLen);
        }

        int start = _inputPtr;
        _inputPtr = ptr;

        return _parseName2(start, hash, '\'');
    }

    /**
     * Method for handling cases where first non-space character
     * of an expected value token is not legal for standard JSON content.
     */
    protected JsonToken _handleOddValue(int i) throws IOException
    {
        // Most likely an error, unless we are to allow single-quote-strings
        switch (i) {
        case '\'':
            /* [JACKSON-173]: allow single quotes. Unlike with regular
             * Strings, we'll eagerly parse contents; this so that there's
             * no need to store information on quote char used.
             *
             * Also, no separation to fast/slow parsing; we'll just do
             * one regular (~= slowish) parsing, to keep code simple
             */
            if (isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
                return _handleApos();
            }
            break;
        case 'N':
            _matchToken("NaN", 1);
            if (isEnabled(Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
                return resetAsNaN("NaN", Double.NaN);
            }
            _reportError("Non-standard token 'NaN': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow");
            break;
        case 'I':
            _matchToken("Infinity", 1);
            if (isEnabled(Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
                return resetAsNaN("Infinity", Double.POSITIVE_INFINITY);
            }
            _reportError("Non-standard token 'Infinity': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow");
            break;
        case '+': // note: '-' is taken as number
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOFInValue();
                }
            }
            return _handleInvalidNumberStart(_inputBuffer[_inputPtr++], false);
        }
        // [Issue#77] Try to decode most likely token
        if (Character.isJavaIdentifierStart(i)) {
            _reportInvalidToken(""+((char) i), "('true', 'false' or 'null')");
        }
        // but if it doesn't look like a token:
        _reportUnexpectedChar(i, "expected a valid value (number, String, array, object, 'true', 'false' or 'null')");
        return null;
    }
    
    protected JsonToken _handleApos() throws IOException
    {
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int outPtr = _textBuffer.getCurrentSegmentSize();

        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(": was expecting closing quote for a string value");
                }
            }
            char c = _inputBuffer[_inputPtr++];
            int i = (int) c;
            if (i <= '\\') {
                if (i == '\\') {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    c = _decodeEscaped();
                } else if (i <= '\'') {
                    if (i == '\'') {
                        break;
                    }
                    if (i < INT_SPACE) {
                        _throwUnquotedSpace(i, "string value");
                    }
                }
            }
            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;
        }
        _textBuffer.setCurrentLength(outPtr);
        return JsonToken.VALUE_STRING;
    }
    
    private String _handleOddName2(int startPtr, int hash, int[] codes) throws IOException
    {
        _textBuffer.resetWithShared(_inputBuffer, startPtr, (_inputPtr - startPtr));
        char[] outBuf = _textBuffer.getCurrentSegment();
        int outPtr = _textBuffer.getCurrentSegmentSize();
        final int maxCode = codes.length;

        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) { // acceptable for now (will error out later)
                    break;
                }
            }
            char c = _inputBuffer[_inputPtr];
            int i = (int) c;
            if (i <= maxCode) {
                if (codes[i] != 0) {
                    break;
                }
            } else if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            ++_inputPtr;
            hash = (hash * CharsToNameCanonicalizer.HASH_MULT) + i;
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;

            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
        }
        _textBuffer.setCurrentLength(outPtr);
        {
            TextBuffer tb = _textBuffer;
            char[] buf = tb.getTextBuffer();
            int start = tb.getTextOffset();
            int len = tb.size();

            return _symbols.findSymbol(buf, start, len, hash);
        }
    }
  
    @Override
    protected final void _finishString() throws IOException
    {
        /* First: let's try to see if we have simple String value: one
         * that does not cross input buffer boundary, and does not
         * contain escape sequences.
         */
        int ptr = _inputPtr;
        final int inputLen = _inputEnd;

        if (ptr < inputLen) {
            final int[] codes = _icLatin1;
            final int maxCode = codes.length;

            do {
                int ch = _inputBuffer[ptr];
                if (ch < maxCode && codes[ch] != 0) {
                    if (ch == '"') {
                        _textBuffer.resetWithShared(_inputBuffer, _inputPtr, (ptr-_inputPtr));
                        _inputPtr = ptr+1;
                        // Yes, we got it all
                        return;
                    }
                    break;
                }
                ++ptr;
            } while (ptr < inputLen);
        }

        /* Either ran out of input, or bumped into an escape
         * sequence...
         */
        _textBuffer.resetWithCopy(_inputBuffer, _inputPtr, (ptr-_inputPtr));
        _inputPtr = ptr;
        _finishString2();
    }

    protected void _finishString2() throws IOException
    {
        char[] outBuf = _textBuffer.getCurrentSegment();
        int outPtr = _textBuffer.getCurrentSegmentSize();
        final int[] codes = _icLatin1;
        final int maxCode = codes.length;

        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(": was expecting closing quote for a string value");
                }
            }
            char c = _inputBuffer[_inputPtr++];
            int i = (int) c;
            if (i < maxCode && codes[i] != 0) {
                if (i == INT_QUOTE) {
                    break;
                } else if (i == INT_BACKSLASH) {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    c = _decodeEscaped();
                } else if (i < INT_SPACE) {
                    _throwUnquotedSpace(i, "string value");
                } // anything else?
            }
            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;
        }
        _textBuffer.setCurrentLength(outPtr);
    }

    /**
     * Method called to skim through rest of unparsed String value,
     * if it is not needed. This can be done bit faster if contents
     * need not be stored for future access.
     */
    protected final void _skipString() throws IOException
    {
        _tokenIncomplete = false;

        int inPtr = _inputPtr;
        int inLen = _inputEnd;
        char[] inBuf = _inputBuffer;

        while (true) {
            if (inPtr >= inLen) {
                _inputPtr = inPtr;
                if (!loadMore()) {
                    _reportInvalidEOF(": was expecting closing quote for a string value");
                }
                inPtr = _inputPtr;
                inLen = _inputEnd;
            }
            char c = inBuf[inPtr++];
            int i = (int) c;
            if (i <= INT_BACKSLASH) {
                if (i == INT_BACKSLASH) {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    _inputPtr = inPtr;
                    c = _decodeEscaped();
                    inPtr = _inputPtr;
                    inLen = _inputEnd;
                } else if (i <= INT_QUOTE) {
                    if (i == INT_QUOTE) {
                        _inputPtr = inPtr;
                        break;
                    }
                    if (i < INT_SPACE) {
                        _inputPtr = inPtr;
                        _throwUnquotedSpace(i, "string value");
                    }
                }
            }
        }
    }

    /*
    /**********************************************************
    /* Internal methods, other parsing
    /**********************************************************
     */
    
    /**
     * We actually need to check the character value here
     * (to see if we have \n following \r).
     */
    protected final void _skipCR() throws IOException {
        if (_inputPtr < _inputEnd || loadMore()) {
            if (_inputBuffer[_inputPtr] == '\n') {
                ++_inputPtr;
            }
        }
        ++_currInputRow;
        _currInputRowStart = _inputPtr;
    }
    
    private final int _skipColon() throws IOException
    {
        if ((_inputPtr + 4) >= _inputEnd) {
            return _skipColon2(false);
        }
        char c = _inputBuffer[_inputPtr];
        if (c == ':') { // common case, no leading space
            int i = _inputBuffer[++_inputPtr];
            if (i > INT_SPACE) { // nor trailing
                if (i == INT_SLASH || i == INT_HASH) {
                    return _skipColon2(true);
                }
                ++_inputPtr;
                return i;
            }
            if (i == INT_SPACE || i == INT_TAB) {
                i = (int) _inputBuffer[++_inputPtr];
                if (i > INT_SPACE) {
                    if (i == INT_SLASH || i == INT_HASH) {
                        return _skipColon2(true);
                    }
                    ++_inputPtr;                    
                    return i;
                }
            }
            return _skipColon2(true); // true -> skipped colon
        }
        if (c == ' ' || c == '\t') {
            c = _inputBuffer[++_inputPtr];
        }
        if (c == ':') {
            int i = _inputBuffer[++_inputPtr];
            if (i > INT_SPACE) {
                if (i == INT_SLASH || i == INT_HASH) {
                    return _skipColon2(true);
                }
                ++_inputPtr;
                return i;
            }
            if (i == INT_SPACE || i == INT_TAB) {
                i = (int) _inputBuffer[++_inputPtr];
                if (i > INT_SPACE) {
                    if (i == INT_SLASH || i == INT_HASH) {
                        return _skipColon2(true);
                    }
                    ++_inputPtr;
                    return i;
                }
            }
            return _skipColon2(true);
        }
        return _skipColon2(false);
    }

    private final int _skipColon2(boolean gotColon) throws IOException
    {
        while (true) {
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            int i = (int) _inputBuffer[_inputPtr++];
            if (i > INT_SPACE) {
                if (i == INT_SLASH) {
                    _skipComment();
                    continue;
                }
                if (i == INT_HASH) {
                    if (_skipYAMLComment()) {
                        continue;
                    }
                }
                if (gotColon) {
                    return i;
                }
                if (i != INT_COLON) {
                    if (i < INT_SPACE) {
                        _throwInvalidSpace(i);
                    }
                    _reportUnexpectedChar(i, "was expecting a colon to separate field name and value");
                }
                gotColon = true;
                continue;
            }
            if (i < INT_SPACE) {
                if (i == INT_LF) {
                    ++_currInputRow;
                    _currInputRowStart = _inputPtr;
                } else if (i == INT_CR) {
                    _skipCR();
                } else if (i != INT_TAB) {
                    _throwInvalidSpace(i);
                }
            }
        }
    }
 
    // Primary loop: no reloading, comment handling
    private final int _skipComma(int i) throws IOException
    {
        if (i != INT_COMMA) {
            _reportUnexpectedChar(i, "was expecting comma to separate "+_parsingContext.getTypeDesc()+" entries");
        }
        while (_inputPtr < _inputEnd) {
            i = (int) _inputBuffer[_inputPtr++];
            if (i > INT_SPACE) {
                if (i == INT_SLASH || i == INT_HASH) {
                    --_inputPtr;
                    return _skipAfterComma2();
                }
                return i;
            }
            if (i < INT_SPACE) {
                if (i == INT_LF) {
                    ++_currInputRow;
                    _currInputRowStart = _inputPtr;
                } else if (i == INT_CR) {
                    _skipCR();
                } else if (i != INT_TAB) {
                    _throwInvalidSpace(i);
                }
            }
        }
        return _skipAfterComma2();
    }

    private final int _skipAfterComma2() throws IOException
    {
        while (_inputPtr < _inputEnd || loadMore()) {
            int i = (int) _inputBuffer[_inputPtr++];
            if (i > INT_SPACE) {
                if (i == INT_SLASH) {
                    _skipComment();
                    continue;
                }
                if (i == INT_HASH) {
                    if (_skipYAMLComment()) {
                        continue;
                    }
                }
                return i;
            }
            if (i < INT_SPACE) {
                if (i == INT_LF) {
                    ++_currInputRow;
                    _currInputRowStart = _inputPtr;
                } else if (i == INT_CR) {
                    _skipCR();
                } else if (i != INT_TAB) {
                    _throwInvalidSpace(i);
                }
            }
        }
        throw _constructError("Unexpected end-of-input within/between "+_parsingContext.getTypeDesc()+" entries");
    }
    
    private final int _skipWSOrEnd() throws IOException
    {
        // Let's handle first character separately since it is likely that
        // it is either non-whitespace; or we have longer run of white space
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                return _eofAsNextChar();
            }
        }
        int i = _inputBuffer[_inputPtr++];
        if (i > INT_SPACE) {
            if (i == INT_SLASH || i == INT_HASH) {
                --_inputPtr;
                return _skipWSOrEnd2();
            }
            return i;
        }
        if (i != INT_SPACE) {
            if (i == INT_LF) {
                ++_currInputRow;
                _currInputRowStart = _inputPtr;
            } else if (i == INT_CR) {
                _skipCR();
            } else if (i != INT_TAB) {
                _throwInvalidSpace(i);
            }
        }
        
        while (_inputPtr < _inputEnd) {
            i = (int) _inputBuffer[_inputPtr++];
            if (i > INT_SPACE) {
                if (i == INT_SLASH || i == INT_HASH) {
                    --_inputPtr;
                    return _skipWSOrEnd2();
                }
                return i;
            }
            if (i != INT_SPACE) {
                if (i == INT_LF) {
                    ++_currInputRow;
                    _currInputRowStart = _inputPtr;
                } else if (i == INT_CR) {
                    _skipCR();
                } else if (i != INT_TAB) {
                    _throwInvalidSpace(i);
                }
            }
        }
        return _skipWSOrEnd2();
    }

    private int _skipWSOrEnd2() throws IOException
    {
        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) { // We ran out of input...
                    return _eofAsNextChar();
                }
            }
            int i = (int) _inputBuffer[_inputPtr++];
            if (i > INT_SPACE) {
                if (i == INT_SLASH) {
                    _skipComment();
                    continue;
                }
                if (i == INT_HASH) {
                    if (_skipYAMLComment()) {
                        continue;
                    }
                }
                return i;
            } else if (i != INT_SPACE) {
                if (i == INT_LF) {
                    ++_currInputRow;
                    _currInputRowStart = _inputPtr;
                } else if (i == INT_CR) {
                    _skipCR();
                } else if (i != INT_TAB) {
                    _throwInvalidSpace(i);
                }
            }
        }
    }
    
    private void _skipComment() throws IOException
    {
        if (!isEnabled(Feature.ALLOW_COMMENTS)) {
            _reportUnexpectedChar('/', "maybe a (non-standard) comment? (not recognized as one since Feature 'ALLOW_COMMENTS' not enabled for parser)");
        }
        // First: check which comment (if either) it is:
        if (_inputPtr >= _inputEnd && !loadMore()) {
            _reportInvalidEOF(" in a comment");
        }
        char c = _inputBuffer[_inputPtr++];
        if (c == '/') {
            _skipLine();
        } else if (c == '*') {
            _skipCComment();
        } else {
            _reportUnexpectedChar(c, "was expecting either '*' or '/' for a comment");
        }
    }

    private void _skipCComment() throws IOException
    {
        // Ok: need the matching '*/'
        while ((_inputPtr < _inputEnd) || loadMore()) {
            int i = (int) _inputBuffer[_inputPtr++];
            if (i <= '*') {
                if (i == '*') { // end?
                    if ((_inputPtr >= _inputEnd) && !loadMore()) {
                        break;
                    }
                    if (_inputBuffer[_inputPtr] == INT_SLASH) {
                        ++_inputPtr;
                        return;
                    }
                    continue;
                }
                if (i < INT_SPACE) {
                    if (i == INT_LF) {
                        ++_currInputRow;
                        _currInputRowStart = _inputPtr;
                    } else if (i == INT_CR) {
                        _skipCR();
                    } else if (i != INT_TAB) {
                        _throwInvalidSpace(i);
                    }
                }
            }
        }
        _reportInvalidEOF(" in a comment");
    }

    private boolean _skipYAMLComment() throws IOException
    {
        if (!isEnabled(Feature.ALLOW_YAML_COMMENTS)) {
            return false;
        }
        _skipLine();
        return true;
    }
    
    private void _skipLine() throws IOException
    {
        // Ok: need to find EOF or linefeed
        while ((_inputPtr < _inputEnd) || loadMore()) {
            int i = (int) _inputBuffer[_inputPtr++];
            if (i < INT_SPACE) {
                if (i == INT_LF) {
                    ++_currInputRow;
                    _currInputRowStart = _inputPtr;
                    break;
                } else if (i == INT_CR) {
                    _skipCR();
                    break;
                } else if (i != INT_TAB) {
                    _throwInvalidSpace(i);
                }
            }
        }
    }

    @Override
    protected char _decodeEscaped() throws IOException
    {
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                _reportInvalidEOF(" in character escape sequence");
            }
        }
        char c = _inputBuffer[_inputPtr++];

        switch ((int) c) {
            // First, ones that are mapped
        case 'b':
            return '\b';
        case 't':
            return '\t';
        case 'n':
            return '\n';
        case 'f':
            return '\f';
        case 'r':
            return '\r';

            // And these are to be returned as they are
        case '"':
        case '/':
        case '\\':
            return c;

        case 'u': // and finally hex-escaped
            break;

        default:
            return _handleUnrecognizedCharacterEscape(c);
        }

        // Ok, a hex escape. Need 4 characters
        int value = 0;
        for (int i = 0; i < 4; ++i) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(" in character escape sequence");
                }
            }
            int ch = (int) _inputBuffer[_inputPtr++];
            int digit = CharTypes.charToHex(ch);
            if (digit < 0) {
                _reportUnexpectedChar(ch, "expected a hex-digit for character escape sequence");
            }
            value = (value << 4) | digit;
        }
        return (char) value;
    }
    
    private final void _matchTrue() throws IOException {
        int ptr = _inputPtr;
        if ((ptr + 3) < _inputEnd) {
            final char[] b = _inputBuffer;
            if (b[ptr] == 'r' && b[++ptr] == 'u' && b[++ptr] == 'e') {
                char c = b[++ptr];
                if (c < '0' || c == ']' || c == '}') { // expected/allowed chars
                    _inputPtr = ptr;
                    return;
                }
            }
        }
        // buffer boundary, or problem, offline
        _matchToken("true", 1);
    }

    private final void _matchFalse() throws IOException {
        int ptr = _inputPtr;
        if ((ptr + 4) < _inputEnd) {
            final char[] b = _inputBuffer;
            if (b[ptr] == 'a' && b[++ptr] == 'l' && b[++ptr] == 's' && b[++ptr] == 'e') {
                char c = b[++ptr];
                if (c < '0' || c == ']' || c == '}') { // expected/allowed chars
                    _inputPtr = ptr;
                    return;
                }
            }
        }
        // buffer boundary, or problem, offline
        _matchToken("false", 1);
    }

    private final void _matchNull() throws IOException {
        int ptr = _inputPtr;
        if ((ptr + 3) < _inputEnd) {
            final char[] b = _inputBuffer;
            if (b[ptr] == 'u' && b[++ptr] == 'l' && b[++ptr] == 'l') {
                char c = b[++ptr];
                if (c < '0' || c == ']' || c == '}') { // expected/allowed chars
                    _inputPtr = ptr;
                    return;
                }
            }
        }
        // buffer boundary, or problem, offline
        _matchToken("null", 1);
    }

    /**
     * Helper method for checking whether input matches expected token
     */
    protected final void _matchToken(String matchStr, int i) throws IOException
    {
        final int len = matchStr.length();

        do {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidToken(matchStr.substring(0, i));
                }
            }
            if (_inputBuffer[_inputPtr] != matchStr.charAt(i)) {
                _reportInvalidToken(matchStr.substring(0, i));
            }
            ++_inputPtr;
        } while (++i < len);

        // but let's also ensure we either get EOF, or non-alphanum char...
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                return;
            }
        }
        char c = _inputBuffer[_inputPtr];
        if (c < '0' || c == ']' || c == '}') { // expected/allowed chars
            return;
        }
        // if Java letter, it's a problem tho
        if (Character.isJavaIdentifierPart(c)) {
            _reportInvalidToken(matchStr.substring(0, i));
        }
        return;
    }

    /*
    /**********************************************************
    /* Binary access
    /**********************************************************
     */

    /**
     * Efficient handling for incremental parsing of base64-encoded
     * textual content.
     */
    @SuppressWarnings("resource")
    protected byte[] _decodeBase64(Base64Variant b64variant) throws IOException
    {
        ByteArrayBuilder builder = _getByteArrayBuilder();

        //main_loop:
        while (true) {
            // first, we'll skip preceding white space, if any
            char ch;
            do {
                if (_inputPtr >= _inputEnd) {
                    loadMoreGuaranteed();
                }
                ch = _inputBuffer[_inputPtr++];
            } while (ch <= INT_SPACE);
            int bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                if (ch == '"') { // reached the end, fair and square?
                    return builder.toByteArray();
                }
                bits = _decodeBase64Escape(b64variant, ch, 0);
                if (bits < 0) { // white space to skip
                    continue;
                }
            }
            int decodedData = bits;
            
            // then second base64 char; can't get padding yet, nor ws
            
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            ch = _inputBuffer[_inputPtr++];
            bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                bits = _decodeBase64Escape(b64variant, ch, 1);
            }
            decodedData = (decodedData << 6) | bits;
            
            // third base64 char; can be padding, but not ws
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            ch = _inputBuffer[_inputPtr++];
            bits = b64variant.decodeBase64Char(ch);

            // First branch: can get padding (-> 1 byte)
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    // as per [JACKSON-631], could also just be 'missing'  padding
                    if (ch == '"' && !b64variant.usesPadding()) {
                        decodedData >>= 4;
                        builder.append(decodedData);
                        return builder.toByteArray();
                    }
                    bits = _decodeBase64Escape(b64variant, ch, 2);
                }
                if (bits == Base64Variant.BASE64_VALUE_PADDING) {
                    // Ok, must get more padding chars, then
                    if (_inputPtr >= _inputEnd) {
                        loadMoreGuaranteed();
                    }
                    ch = _inputBuffer[_inputPtr++];
                    if (!b64variant.usesPaddingChar(ch)) {
                        throw reportInvalidBase64Char(b64variant, ch, 3, "expected padding character '"+b64variant.getPaddingChar()+"'");
                    }
                    // Got 12 bits, only need 8, need to shift
                    decodedData >>= 4;
                    builder.append(decodedData);
                    continue;
                }
                // otherwise we got escaped other char, to be processed below
            }
            // Nope, 2 or 3 bytes
            decodedData = (decodedData << 6) | bits;
            // fourth and last base64 char; can be padding, but not ws
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            ch = _inputBuffer[_inputPtr++];
            bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    // as per [JACKSON-631], could also just be 'missing'  padding
                    if (ch == '"' && !b64variant.usesPadding()) {
                        decodedData >>= 2;
                        builder.appendTwoBytes(decodedData);
                        return builder.toByteArray();
                    }
                    bits = _decodeBase64Escape(b64variant, ch, 3);
                }
                if (bits == Base64Variant.BASE64_VALUE_PADDING) {
                    // With padding we only get 2 bytes; but we have
                    // to shift it a bit so it is identical to triplet
                    // case with partial output.
                    // 3 chars gives 3x6 == 18 bits, of which 2 are
                    // dummies, need to discard:
                    decodedData >>= 2;
                    builder.appendTwoBytes(decodedData);
                    continue;
                }
                // otherwise we got escaped other char, to be processed below
            }
            // otherwise, our triplet is now complete
            decodedData = (decodedData << 6) | bits;
            builder.appendThreeBytes(decodedData);
        }
    }

    /*
    /**********************************************************
    /* Error reporting
    /**********************************************************
     */

    protected void _reportInvalidToken(String matchedPart) throws IOException {
        _reportInvalidToken(matchedPart, "'null', 'true', 'false' or NaN");
    }
    
    protected void _reportInvalidToken(String matchedPart, String msg) throws IOException
    {
        StringBuilder sb = new StringBuilder(matchedPart);
        /* Let's just try to find what appears to be the token, using
         * regular Java identifier character rules. It's just a heuristic,
         * nothing fancy here.
         */
        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    break;
                }
            }
            char c = _inputBuffer[_inputPtr];
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            ++_inputPtr;
            sb.append(c);
        }
        _reportError("Unrecognized token '"+sb.toString()+"': was expecting "+msg);
    }
}
