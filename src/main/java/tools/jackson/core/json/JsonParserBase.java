package tools.jackson.core.json;

import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.base.ParserBase;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.CharTypes;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.NumberInput;
import tools.jackson.core.util.JacksonFeatureSet;

/**
 * Another intermediate base class, only used by actual JSON-backed parser
 * implementations.
 *
 * @since 3.0
 */
public abstract class JsonParserBase
    extends ParserBase
{
    private final static char[] NO_CHARS = new char[0];

    /*
    /**********************************************************************
    /* JSON-specific configuration
    /**********************************************************************
     */

    /**
     * Bit flag for {@link JsonReadFeature}s that are enabled.
     */
    protected int _formatReadFeatures;

    /*
    /**********************************************************************
    /* Parsing state
    /**********************************************************************
     */

    /**
     * Information about parser context, context in which
     * the next token is to be parsed (root, array, object).
     */
    protected JsonReadContext _streamReadContext;

    /**
     * Secondary token related to the next token after current one;
     * used if its type is known. This may be value token that
     * follows {@link JsonToken#PROPERTY_NAME}, for example.
     */
    protected JsonToken _nextToken;

    /**
     * Marker for integer values read using JSON5 hexadecimal notation
     * ({@code 0x} / {@code 0X} prefix), enabled via
     * {@link JsonReadFeature#ALLOW_HEXADECIMAL_NUMBERS}.
     * When {@code true}, the textual representation buffered for the current
     * token is the original hex literal (including any sign and the
     * {@code 0x}/{@code 0X} prefix) and {@link #_intLength} records the
     * number of hexadecimal digits (excluding sign and prefix).
     *
     * @since 3.2
     */
    protected boolean _numberIsHex;

    /*
    /**********************************************************************
    /* Helper buffer recycling
    /**********************************************************************
     */

    /**
     * Temporary buffer that is needed if an Object property name is accessed
     * using {@link #getTextCharacters} method (instead of String
     * returning alternatives)
     */
    private char[] _nameCopyBuffer = NO_CHARS;

    /**
     * Flag set to indicate whether the Object property name is available
     * from the name copy buffer or not (in addition to its String
     * representation  being available via read context)
     */
    protected boolean _nameCopied;

    /**
     * Lazily-allocated intermediate buffer used by {@code _streamString()}
     * implementations to batch writes to the target {@link java.io.Writer}.
     * Allocated on first call and reused on subsequent calls to avoid
     * repeated allocation for parsers that call {@code readString(Writer)}
     * multiple times.
     *
     * @since 3.1
     */
    private char[] _streamStringBuffer;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected JsonParserBase(ObjectReadContext readCtxt,
            IOContext ctxt, int streamReadFeatures, int formatReadFeatures)
    {
        super(readCtxt, ctxt, streamReadFeatures);
        _formatReadFeatures = formatReadFeatures;
        DupDetector dups = StreamReadFeature.STRICT_DUPLICATE_DETECTION.enabledIn(streamReadFeatures)
                ? DupDetector.rootDetector(this) : null;
        _streamReadContext = JsonReadContext.createRootContext(dups);
    }

    /*
    /**********************************************************************
    /* Versioned, capabilities, config
    /**********************************************************************
     */

    @Override public Version version() { return PackageVersion.VERSION; }

    @Override
    public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() {
        // For now, JSON settings do not differ from general defaults:
        return DEFAULT_READ_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* ParserBase method implementions/overrides
    /**********************************************************************
     */

    @Override public TokenStreamContext streamReadContext() { return _streamReadContext; }

    @Override
    public Object currentValue() {
        return _streamReadContext.currentValue();
    }

    @Override
    public void assignCurrentValue(Object v) {
        _streamReadContext.assignCurrentValue(v);
    }

    /**
     * Method that can be called to get the name associated with
     * the current event.
     */
    @Override public String currentName() {
        // [JACKSON-395]: start markers require information from parent
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            JsonReadContext parent = _streamReadContext.getParent();
            if (parent != null) {
                return parent.currentName();
            }
        }
        return _streamReadContext.currentName();
    }

    @Override
    public boolean hasStringCharacters() {
        if (_currToken == JsonToken.VALUE_STRING) { return true; } // usually true
        if (_currToken == JsonToken.PROPERTY_NAME) { return _nameCopied; }
        return false;
    }

    // 03-Nov-2019, tatu: Will not recycle "name copy buffer" any more as it seems
    //   unlikely to be of much real benefit
    /*
    @Override
    protected void _releaseBuffers() {
        super._releaseBuffers();
        char[] buf = _nameCopyBuffer;
        if (buf != null) {
            _nameCopyBuffer = null;
            _ioContext.releaseNameCopyBuffer(buf);
        }
    }
    */

    /*
    /**********************************************************************
    /* Internal/package methods: Context handling
    /**********************************************************************
     */

    protected void createChildArrayContext(final int lineNr, final int colNr) throws JacksonException {
        _streamReadContext = _streamReadContext.createChildArrayContext(lineNr, colNr);
        _streamReadConstraints.validateNestingDepth(_streamReadContext.getNestingDepth());
    }

    protected void createChildObjectContext(final int lineNr, final int colNr) throws JacksonException {
        _streamReadContext = _streamReadContext.createChildObjectContext(lineNr, colNr);
        _streamReadConstraints.validateNestingDepth(_streamReadContext.getNestingDepth());
    }

    /*
    /**********************************************************************
    /* Numeric parsing method implementations
    /**********************************************************************
     */

    // Overridden to also clear the JSON-only `_numberIsHex` flag, so a
    // subsequent regular integer is not mis-decoded as hex. Hex literals go
    // through `resetIntHex` instead, which sets the flag.
    @Override
    protected JsonToken resetInt(boolean negative, int intLen)
        throws JacksonException
    {
        _numberIsHex = false;
        return super.resetInt(negative, intLen);
    }

    /**
     * Variant of {@link #resetInt} used for integer values read in JSON5
     * hexadecimal notation ({@code 0x...}). {@code hexDigitLen} is the
     * number of hexadecimal digits (excluding sign and {@code 0x}/{@code 0X}
     * prefix); the textual representation buffered by the caller is expected
     * to contain the original literal including sign and prefix.
     *
     * @since 3.2
     */
    protected final JsonToken resetIntHex(boolean negative, int hexDigitLen)
        throws JacksonException
    {
        // May throw StreamConstraintsException:
        _streamReadConstraints.validateIntegerLength(hexDigitLen);
        _numberNegative = negative;
        _numberIsNaN = false;
        _numberIsHex = true;
        _intLength = hexDigitLen;
        _fractLength = 0;
        _expLength = 0;
        _numTypesValid = NR_UNKNOWN; // to force decoding
        _numberString = null;
        return JsonToken.VALUE_NUMBER_INT;
    }

    @Override
    protected void _parseNumericValue(int expType)
        throws JacksonException, InputCoercionException
    {
        // Int or float?
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if (_numberIsHex) {
                _parseHexInt(expType);
                return;
            }
            int len = _intLength;
            // First: optimization for simple int
            if (len <= 9) {
                int i = _textBuffer.contentsAsInt(_numberNegative);
                _numberInt = i;
                _numTypesValid = NR_INT;
                return;
            }
            if (len <= 18) { // definitely fits AND is easy to parse using 2 int parse calls
                long l = _textBuffer.contentsAsLong(_numberNegative);
                // Might still fit in int, need to check
                if (len == 10) {
                    if (_numberNegative) {
                        if (l >= MIN_INT_L) {
                            _numberInt = (int) l;
                            _numTypesValid = NR_INT;
                            return;
                        }
                    } else {
                        if (l <= MAX_INT_L) {
                            _numberInt = (int) l;
                            _numTypesValid = NR_INT;
                            return;
                        }
                    }
                }
                _numberLong = l;
                _numTypesValid = NR_LONG;
                return;
            }
             // For [core#865]: handle remaining 19-char cases as well
            if (len == 19) {
                char[] buf = _textBuffer.getTextBuffer();
                int offset = _textBuffer.getTextOffset();
                if (_numberNegative) {
                    ++offset;
                }
                if (NumberInput.inLongRange(buf, offset, len, _numberNegative)) {
                    _numberLong = NumberInput.parseLong19(buf, offset, _numberNegative);
                    _numTypesValid = NR_LONG;
                    return;
                }
            }
            _parseSlowInt(expType);
            return;
        }
        if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
            _parseSlowFloat(expType);
            return;
        }
        throw _constructNotNumericType(_currToken, expType);
    }

    @Override
    protected int _parseIntValue() throws JacksonException
    {
        // Inlined variant of: _parseNumericValue(NR_INT)
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            // Hex integers go through the generic path so the base-16 decode is
            // applied (the base-10 fast path below would mis-read the literal):
            if (_intLength <= 9 && !_numberIsHex) {
                int i = _textBuffer.contentsAsInt(_numberNegative);
                _numberInt = i;
                _numTypesValid = NR_INT;
                return i;
            }
        }
        // if not optimizable, use more generic
        _parseNumericValue(NR_INT);
        if ((_numTypesValid & NR_INT) == 0) {
            convertNumberToInt();
        }
        return _numberInt;
    }

    private void _parseSlowFloat(int expType) throws JacksonException
    {
        /* Nope: floating point. Here we need to be careful to get
         * optimal parsing strategy: choice is between accurate but
         * slow (BigDecimal) and lossy but fast (Double). For now
         * let's only use BD when explicitly requested -- it can
         * still be constructed correctly at any point since we do
         * retain textual representation
         */
        if (expType == NR_BIGDECIMAL) {
            // 04-Dec-2022, tatu: Let's defer actual decoding until it is certain
            //    value is actually needed.
            // 24-Jun-2024, tatu: No; we shouldn't have to defer unless specifically
            //    request w/ `getNumberValueDeferred()` or so
            _numberBigDecimal = _textBuffer.contentsAsDecimal(isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
            _numTypesValid = NR_BIGDECIMAL;
        } else if (expType == NR_DOUBLE) {
            _numberDouble = _textBuffer.contentsAsDouble(isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
            _numTypesValid = NR_DOUBLE;
        } else if (expType == NR_FLOAT) {
            _numberFloat = _textBuffer.contentsAsFloat(isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
            _numTypesValid = NR_FLOAT;
        } else { // NR_UNKOWN, or one of int types
            // 04-Dec-2022, tatu: We can get all kinds of values here
            //    (NR_INT, NR_LONG or even NR_UNKNOWN). Should we try further
            //    deferring some typing?
            _numberDouble = 0.0;
            _numberString = _textBuffer.contentsAsString();
            _numTypesValid = NR_DOUBLE;
        }
    }

    /**
     * Decode a JSON5 hexadecimal integer that was buffered as the original
     * textual literal (sign + {@code 0x}/{@code 0X} prefix + hex digits).
     * {@link #_intLength} holds the count of hex digits.
     *
     * @since 3.2
     */
    private void _parseHexInt(int expType) throws JacksonException
    {
        final int hexLen = _intLength;
        final char[] buf = _textBuffer.getTextBuffer();
        // Locate the first hex digit: skip optional sign and "0x" / "0X" prefix
        int idx = _textBuffer.getTextOffset();
        final char first = buf[idx];
        if (first == '-' || first == '+') {
            ++idx;
        }
        idx += 2; // skip "0x" / "0X"

        // Up to 7 hex digits always fit in a positive signed int (<= 0x0FFFFFFF).
        // 8 hex digits may overflow signed int (e.g. 0x80000000), so we defer to
        // the long path which handles range checks uniformly.
        if (hexLen <= 7) {
            int v = 0;
            for (int i = 0; i < hexLen; ++i) {
                v = (v << 4) | CharTypes.charToHex(buf[idx + i]);
            }
            _numberInt = _numberNegative ? -v : v;
            _numTypesValid = NR_INT;
            return;
        }
        // 9..15 hex digits always fit in a positive long (63 bits used at most)
        if (hexLen <= 15) {
            long v = 0L;
            for (int i = 0; i < hexLen; ++i) {
                v = (v << 4) | CharTypes.charToHex(buf[idx + i]);
            }
            _numberLong = _numberNegative ? -v : v;
            _numTypesValid = NR_LONG;
            return;
        }
        // 16 hex digits: may or may not fit in signed long, depending on top bit
        if (hexLen == 16) {
            int topNibble = CharTypes.charToHex(buf[idx]);
            if (topNibble < 0x8) { // fits in positive signed long
                long v = topNibble;
                for (int i = 1; i < 16; ++i) {
                    v = (v << 4) | CharTypes.charToHex(buf[idx + i]);
                }
                _numberLong = _numberNegative ? -v : v;
                _numTypesValid = NR_LONG;
                return;
            }
            // else fall through to BigInteger path
        }
        // Larger values -> BigInteger. We must eagerly decode here (the lazy
        // base-10 path via _numberString would mis-read hex digits). Pass the
        // char[] slice directly so the fast path avoids an intermediate String.
        BigInteger bi = NumberInput.parseBigIntegerWithRadix(buf, idx, hexLen, 16,
                isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        if (_numberNegative) {
            bi = bi.negate();
        }
        _numberBigInt = bi;
        _numberString = null;
        _numTypesValid = NR_BIGINT;
        if ((expType == NR_INT) || (expType == NR_LONG)) {
            // Force the overflow path to surface a meaningful error
            _reportTooLongIntegral(expType, _textBuffer.contentsAsString());
        }
    }

    /**
     * Standard error message used by all JSON parser variants when a
     * {@code 0x}/{@code 0X} hex prefix is not followed by any hex digit.
     *
     * @since 3.2
     */
    protected static String _hexPrefixNotFollowedMessage(char prefixChar) {
        return "Hexadecimal number prefix '0" + prefixChar
                + "' must be followed by at least one hex digit (0-9, a-f, A-F)";
    }

    /**
     * Called after seeing the {@code 'x'} or {@code 'X'} that follows a leading
     * {@code '0'} in a number literal. Returns silently if
     * {@link JsonReadFeature#ALLOW_HEXADECIMAL_NUMBERS} is enabled; otherwise
     * throws a {@link StreamReadException} naming the feature that must be
     * enabled, so the user gets a specific actionable error instead of a
     * generic "unexpected character".
     *
     * @since 3.2
     */
    protected void _checkHexNumbersAllowed(int prefixChar) throws StreamReadException {
        if (!isEnabled(JsonReadFeature.ALLOW_HEXADECIMAL_NUMBERS)) {
            _reportUnexpectedChar(prefixChar,
                    "hexadecimal number literals require enabling `JsonReadFeature.ALLOW_HEXADECIMAL_NUMBERS`");
        }
    }

    private void _parseSlowInt(int expType) throws JacksonException
    {
        final String numStr = _textBuffer.contentsAsString();
        // 16-Oct-2018, tatu: Need to catch "too big" early due to [jackson-core#488]
        if ((expType == NR_INT) || (expType == NR_LONG)) {
            _reportTooLongIntegral(expType, numStr);
        }
        if ((expType == NR_DOUBLE) || (expType == NR_FLOAT)) {
            _numberString = numStr;
            _numTypesValid = NR_DOUBLE;
        } else {
            // nope, need the heavy guns... (rare case) - since Jackson v2.14, BigInteger parsing is lazy
            _numberBigInt = null;
            _numberString = numStr;
            _numTypesValid = NR_BIGINT;
        }
    }

    protected void _reportTooLongIntegral(int expType, String rawNum) throws JacksonException
    {
        if (expType == NR_INT) {
            _reportOverflowInt(rawNum);
        }
        _reportOverflowLong(rawNum);
    }

    /*
    /**********************************************************************
    /* Internal/package methods: config access
    /**********************************************************************
     */

    public boolean isEnabled(JsonReadFeature f) { return f.enabledIn(_formatReadFeatures); }

    /*
    /**********************************************************************
    /* Internal/package methods: buffer handling
    /**********************************************************************
     */

    protected char[] currentNameInBuffer() {
        if (_nameCopied) {
            return _nameCopyBuffer;
        }
        final String name = _streamReadContext.currentName();
        final int nameLen = name.length();
        if (_nameCopyBuffer.length < nameLen) {
            _nameCopyBuffer = new char[Math.max(32, nameLen)];
        }
        name.getChars(0, nameLen, _nameCopyBuffer, 0);
        _nameCopied = true;
        return _nameCopyBuffer;
    }

    /**
     * Returns the lazily-allocated intermediate buffer used by
     * {@code _streamString()} to batch-write decoded characters to a
     * {@link java.io.Writer}. The same buffer is reused across calls.
     *
     * @since 3.1
     */
    protected char[] _bufferForStringStreaming() {
        char[] buf = _streamStringBuffer;
        if (buf == null) {
            _streamStringBuffer = buf = new char[1024];
        }
        return buf;
    }
    
    /*
    /**********************************************************************
    /* Internal/package methods: Error reporting
    /**********************************************************************
     */

    protected char _handleUnrecognizedCharacterEscape(char ch) throws StreamReadException {
        // It is possible we allow all kinds of non-standard escapes...
        if (isEnabled(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
            return ch;
        }
        // and if allowing single-quoted names, String values, single-quote needs to be escapable regardless
        if (ch == '\'' && isEnabled(JsonReadFeature.ALLOW_SINGLE_QUOTES)) {
            return ch;
        }
        throw _constructReadException("Unrecognized character escape "+_getCharDesc(ch),
                _currentLocationMinusOne());
    }

    // Promoted from `ParserBase` in 3.0
    protected void _reportMismatchedEndMarker(int actCh, char expCh) throws StreamReadException {
        final TokenStreamContext ctxt = streamReadContext();
        // 31-Jan-2025, tatu: [core#1394] Need to check case of no open scope
        if (ctxt.inRoot()) {
            _reportExtraEndMarker(actCh);
            return;
        }
        final String msg = String.format(
                "Unexpected close marker '%s': expected '%c' (for %s starting at %s)",
                (char) actCh, expCh, ctxt.typeDesc(), ctxt.startLocation(_contentReference()));
        throw _constructReadException(msg, _currentLocationMinusOne());
    }

    protected void _reportExtraEndMarker(int actCh) throws StreamReadException {
        final String scopeDesc = (actCh == '}') ? "Object" : "Array";
        final String msg = String.format(
                "Unexpected close marker '%s': no open %s to close", (char) actCh, scopeDesc);
        throw _constructReadException(msg, _currentLocationMinusOne());
    }

    // Method called to report a problem with unquoted control character.
    // Note: it is possible to suppress some instances of
    // exception by enabling {@link JsonReadFeature#ALLOW_UNESCAPED_CONTROL_CHARS}.
    protected void _throwUnquotedSpace(int i, String ctxtDesc) throws StreamReadException {
        // It is possible to allow unquoted control chars:
        if (!isEnabled(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS) || i > INT_SPACE) {
            char c = (char) i;
            String msg = "Illegal unquoted character ("+_getCharDesc(c)+"): has to be escaped using backslash to be included in "+ctxtDesc;
            throw _constructReadException(msg, _currentLocationMinusOne());
        }
    }

    // @return Description to use as "valid tokens" in an exception message about
    //    invalid (unrecognized) JSON token: called when parser finds something that
    //    looks like unquoted textual token
    protected String _validJsonTokenList() {
        return _validJsonValueList();
    }

    // @return Description to use as "valid JSON values" in an exception message about
    //   invalid (unrecognized) JSON value: called when parser finds something that
    //    does not look like a value or separator.
    protected String _validJsonValueList() {
        if (isEnabled(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)) {
            return "(JSON String, Number (or 'NaN'/'+INF'/'-INF'), Array, Object or token 'null', 'true' or 'false')";
        }
        return "(JSON String, Number, Array, Object or token 'null', 'true' or 'false')";
    }

    /*
    /**********************************************************************
    /* Internal/package methods: surrogate handling
    /**********************************************************************
     */

    /**
     * Validate that {@code lo} is a valid low surrogate (DC00-DFFF) and combine
     * with high surrogate {@code hi} into a supplementary code point.
     *
     * @since 3.1
     */
    protected int _decodeSurrogate(int hi, int lo) throws StreamReadException {
        if (lo < 0xDC00 || lo > 0xDFFF) {
            _reportError(String.format(
                    "Broken surrogate pair in property name: expected low surrogate (DC00-DFFF), got %04X", lo));
        }
        return 0x10000 + ((hi - 0xD800) << 10) + (lo - 0xDC00);
    }

    /**
     * Report an error for a lone low surrogate encountered without a preceding
     * high surrogate.
     *
     * @since 3.1
     */
    protected <T> T _reportUnexpectedLowSurrogate(int ch) throws StreamReadException {
        return _reportError(String.format(
                "Unexpected low surrogate in property name (%04X) without preceding high surrogate", ch));
    }

    /*
    /**********************************************************************
    /* Internal/package methods: other
    /**********************************************************************
     */

    protected boolean _isAllowedCtrlCharRS(int i) {
        return (i == INT_RS) && JsonReadFeature.ALLOW_RS_CONTROL_CHAR.enabledIn(_formatReadFeatures);
    }
}
