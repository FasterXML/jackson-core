package tools.jackson.core.json;

import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.base.ParserBase;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.CharTypes;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.NumberInput;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;
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
                // 09-Oct-2026, tatu: [core#784] leading '+' is retained in text
                //    (if enabled), so must be skipped same as '-'
                if (_numberNegative || (buf[offset] == '+')) {
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

    // 09-Oct-2026, tatu: [core#1746] Decimal point must be followed by a digit,
    //   unless trailing decimal point is allowed AND there is an integer part
    // @since 3.1.8
    protected boolean _missingFractionDigits(int fractLen, int intLen) {
        return (fractLen == 0)
                && ((intLen == 0) || !isEnabled(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS));
    }

    // @since 3.1.8
    protected void _verifyFractionDigits(int fractLen, int intLen, int ch) throws StreamReadException {
        if (_missingFractionDigits(fractLen, intLen)) {
            _reportUnexpectedNumberChar(ch, "Decimal point not followed by a digit");
        }
    }

    // @since 3.1.8
    protected void _reportLeadingPlusSignNotAllowed() throws StreamReadException {
        _reportUnexpectedNumberChar('+', "JSON spec does not allow numbers to have plus signs: enable `JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS` to allow");
    }

    // 09-Oct-2026, tatu: [core#1748] Byte-based parsers accept all multi-byte UTF-8
    //   characters when scanning unquoted names, so decoded name must be verified to
    //   only contain chars `ReaderBasedJsonParser` accepts (Java identifier parts)
    private void _verifyUnquotedName(String name) throws StreamReadException {
        for (int i = 0, len = name.length(); i < len; ++i) {
            final char c = name.charAt(i);
            if ((c > 0x7F) && !Character.isJavaIdentifierPart(c)) {
                _reportUnexpectedChar(c, (i == 0)
                        ? "was expecting either valid name character (for unquoted name) or double-quote (for quoted) to start property name"
                        : "was expecting a colon to separate property name and value");
            }
        }
    }

    /**
     * Helper method used by UTF-8 byte-based parsers to decode property name from
     * quads collected while scanning it, and add it to the symbol table.
     *
     * @param symbols Symbol table to add name to
     * @param quads Name bytes, packed 4 per quad (big-endian), last quad padded
     * @param qlen Number of quads used
     * @param lastQuadBytes Number of bytes used in the last quad (1 - 4)
     *
     * @return Decoded (and canonicalized, if enabled) name
     *
     * @since 3.1.8
     */
    // 09-Oct-2026, tatu: [core#1748] Moved from `UTF8StreamJsonParser`,
    //   `UTF8DataInputJsonParser` and `NonBlockingJsonParserBase`
    protected final String _decodeAndAddUTF8Name(ByteQuadsCanonicalizer symbols,
            int[] quads, int qlen, int lastQuadBytes)
        throws StreamReadException, StreamConstraintsException
    {
        return _addUTF8Name(symbols, _decodeUTF8Name(quads, qlen, lastQuadBytes), quads, qlen);
    }

    /**
     * Helper method used by UTF-8 byte-based parsers to find or decode unquoted
     * property name, verifying it to only contain valid name characters before
     * adding it to the symbol table.
     *
     * @param symbols Symbol table to find name in or add it to
     * @param quads Name bytes, packed 4 per quad (big-endian), last quad padded
     * @param qlen Number of quads used
     * @param lastQuadBytes Number of bytes used in the last quad (1 - 4)
     *
     * @return Decoded (and canonicalized, if enabled) name
     *
     * @since 3.1.8
     */
    protected final String _findOrAddUnquotedUTF8Name(ByteQuadsCanonicalizer symbols,
            int[] quads, int qlen, int lastQuadBytes)
        throws StreamReadException, StreamConstraintsException
    {
        final boolean nonAscii = _hasNonAsciiBytes(quads, qlen, lastQuadBytes);
        String name = symbols.findName(quads, qlen);
        if (name == null) {
            name = _decodeUTF8Name(quads, qlen, lastQuadBytes);
            // Must verify before adding, to not add invalid names in symbol table
            if (nonAscii) {
                _verifyUnquotedName(name);
            }
            return _addUTF8Name(symbols, name, quads, qlen);
        }
        // Found names may have been added as quoted names, so need to verify too
        if (nonAscii) {
            _verifyUnquotedName(name);
        }
        return name;
    }

    private static boolean _hasNonAsciiBytes(int[] quads, int qlen, int lastQuadBytes) {
        int bits = 0;
        for (int i = 0, end = qlen - 1; i < end; ++i) {
            bits |= quads[i];
        }
        // Last quad is padded with 0xFF bytes, need to mask those out
        final int last = quads[qlen - 1];
        bits |= (lastQuadBytes == 4) ? last : (last & ((1 << (lastQuadBytes << 3)) - 1));
        return (bits & 0x80808080) != 0;
    }

    private String _addUTF8Name(ByteQuadsCanonicalizer symbols, String name,
            int[] quads, int qlen)
        throws StreamConstraintsException
    {
        // 5-May-2023, ckozak: [core#1015] respect CANONICALIZE_FIELD_NAMES factory config.
        if (!symbols.isCanonicalizing()) {
            return name;
        }
        return symbols.addName(name, quads, qlen);
    }

    // Decodes name from quads; last quad must be restored before returning
    private String _decodeUTF8Name(int[] quads, int qlen, int lastQuadBytes)
        throws StreamReadException, StreamConstraintsException
    {
        // Ok: must decode UTF-8 chars. No other validation is needed, since unescaping
        // has been done earlier as necessary (as well as error reporting for unescaped
        // control chars)

        // 4 bytes per quad, except last one maybe less
        final int byteLen = (qlen << 2) - 4 + lastQuadBytes;
        _streamReadConstraints.validateNameLength(byteLen);

        // And last one is not correctly aligned (leading zero bytes instead
        // need to shift a bit, instead of trailing). Only need to shift it
        // for UTF-8 decoding; need revert for storage (since key will not
        // be aligned, to optimize lookup speed)
        int lastQuad;

        if (lastQuadBytes < 4) {
            lastQuad = quads[qlen-1];
            // 8/16/24 bit left shift
            quads[qlen-1] = (lastQuad << ((4 - lastQuadBytes) << 3));
        } else {
            lastQuad = 0;
        }

        // Need some working space, TextBuffer works well:
        char[] cbuf = _textBuffer.emptyAndGetCurrentSegment();
        int cix = 0;

        for (int ix = 0; ix < byteLen; ) {
            int ch = quads[ix >> 2]; // current quad, need to shift+mask
            int byteIx = (ix & 3);
            ch = (ch >> ((3 - byteIx) << 3)) & 0xFF;
            ++ix;

            if (ch > 127) { // multi-byte
                int needed;
                if ((ch & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
                    ch &= 0x1F;
                    needed = 1;
                } else if ((ch & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
                    ch &= 0x0F;
                    needed = 2;
                } else if ((ch & 0xF8) == 0xF0) { // 4 bytes; double-char with surrogates and all...
                    ch &= 0x07;
                    needed = 3;
                } else { // 5- and 6-byte chars not valid json chars
                    _reportInvalidInitial(ch);
                    needed = ch = 1; // never really gets this far
                }
                // 09-Oct-2026, tatu: [core#1748] name bytes are complete, so this is
                //   a truncated sequence, not EOF
                if ((ix + needed) > byteLen) {
                    _reportError("Invalid UTF-8: incomplete multi-byte sequence in property name");
                }

                // Ok, always need at least one more:
                int ch2 = quads[ix >> 2]; // current quad, need to shift+mask
                byteIx = (ix & 3);
                ch2 = (ch2 >> ((3 - byteIx) << 3));
                ++ix;

                if ((ch2 & 0xC0) != 0x080) {
                    _reportInvalidOther(ch2 & 0xFF);
                }
                ch = (ch << 6) | (ch2 & 0x3F);
                if (needed > 1) {
                    ch2 = quads[ix >> 2];
                    byteIx = (ix & 3);
                    ch2 = (ch2 >> ((3 - byteIx) << 3));
                    ++ix;

                    if ((ch2 & 0xC0) != 0x080) {
                        _reportInvalidOther(ch2 & 0xFF);
                    }
                    ch = (ch << 6) | (ch2 & 0x3F);
                    if (needed > 2) { // 4 bytes? (need surrogates on output)
                        ch2 = quads[ix >> 2];
                        byteIx = (ix & 3);
                        ch2 = (ch2 >> ((3 - byteIx) << 3));
                        ++ix;
                        if ((ch2 & 0xC0) != 0x080) {
                            _reportInvalidOther(ch2 & 0xFF);
                        }
                        ch = (ch << 6) | (ch2 & 0x3F);
                    }
                }
                _verifyUTF8NameCodePoint(ch, needed);
                if (needed > 2) { // surrogate pair? once again, let's output one here, one later on
                    ch -= 0x10000; // to normalize it starting with 0x0
                    if (cix >= cbuf.length) {
                        cbuf = _textBuffer.expandCurrentSegment();
                    }
                    cbuf[cix++] = (char) (0xD800 + (ch >> 10));
                    ch = 0xDC00 | (ch & 0x03FF);
                }
            }
            if (cix >= cbuf.length) {
                cbuf = _textBuffer.expandCurrentSegment();
            }
            cbuf[cix++] = (char) ch;
        }

        // And finally, un-align if necessary
        if (lastQuadBytes < 4) {
            quads[qlen-1] = lastQuad;
        }
        return new String(cbuf, 0, cix);
    }

    // Helper method needed to fix [jackson-core#148], masking of 0x00 character
    // @since 3.1.8
    protected final static int _padLastQuad(int q, int bytes) {
        return (bytes == 4) ? q : (q | (-1 << (bytes << 3)));
    }

    // 09-Oct-2026, tatu: [core#1748] Moved from `UTF8StreamJsonParser`,
    //   `UTF8DataInputJsonParser` and `NonBlockingJsonParserBase`
    // @since 3.1.8
    protected <T> T _reportInvalidOther(int mask) throws StreamReadException {
        return _reportError("Invalid UTF-8 middle byte 0x"+Integer.toHexString(mask));
    }

    // @since 3.1.8
    protected <T> T _reportInvalidOther(int mask, int ptr) throws StreamReadException {
        _inputPtr = ptr;
        return _reportInvalidOther(mask);
    }

    // 09-Oct-2026, tatu: [core#1748] rejects overlong encodings, surrogates and code points
    //   beyond U+10FFFF for code point decoded from multi-byte UTF-8 sequence (with `needed`
    //   continuation bytes) in a property name
    private void _verifyUTF8NameCodePoint(int ch, int needed) throws StreamReadException {
        if (needed == 1) {
            if (ch < 0x80) {
                _reportError("Invalid UTF-8: overlong 2-byte encoding of 0x"+Integer.toHexString(ch));
            }
        } else if (needed == 2) {
            if (ch < 0x800) {
                _reportError("Invalid UTF-8: overlong 3-byte encoding of 0x"+Integer.toHexString(ch));
            }
            // [jackson-core#363]: Surrogates (0xD800 - 0xDFFF) are illegal in UTF-8
            if (ch >= 0xD800 && ch <= 0xDFFF) {
                _reportInvalidUTF8Surrogate(ch);
            }
        } else if (ch < 0x10000) {
            _reportError("Invalid UTF-8: overlong 4-byte encoding of 0x"+Integer.toHexString(ch));
        } else if (ch > 0x10FFFF) {
            _reportError("Invalid UTF-8: code point 0x"+Integer.toHexString(ch)+" beyond U+10FFFF");
        }
    }

    // 09-Oct-2026, tatu: [core#1750] Handling of a String value char that is neither
    //   escape nor valid (UTF-8) start char: control chars are only allowed (and returned
    //   from) with ALLOW_UNESCAPED_CONTROL_CHARS; must not be reduced to a plain
    //   "_reportInvalidChar()" call.
    // @since 3.1.8
    protected void _handleInvalidStringChar(int c) throws JacksonException {
        if (c >= INT_SPACE) {
            _reportInvalidChar(c);
            return; // never gets here
        }
        // Throws unless control chars allowed
        _throwUnquotedSpace(c, "string value");
        if (c == INT_LF || c == INT_CR) {
            _handleLinefeedInString(c);
        }
    }

    /**
     * Method called for an unescaped linefeed (allowed by
     * {@link JsonReadFeature#ALLOW_UNESCAPED_CONTROL_CHARS}) within a String value,
     * to update row tracking. Default implementation does nothing.
     *
     * @param c Linefeed character ({@code '\r'} or {@code '\n'})
     *
     * @throws JacksonException for low-level read issues
     *
     * @since 3.1.8
     */
    protected void _handleLinefeedInString(int c) throws JacksonException { }

    // @since 3.1.8 (moved from sub-classes)
    protected <T> T _reportInvalidChar(int c) throws StreamReadException {
        // Either invalid WS or illegal UTF-8 start char
        if (c < INT_SPACE) {
            _reportInvalidSpace(c);
        }
        return _reportInvalidInitial(c);
    }

    // @since 3.1.8 (moved from sub-classes)
    protected <T> T _reportInvalidInitial(int mask) throws StreamReadException {
        return _reportError("Invalid UTF-8 start byte 0x"+Integer.toHexString(mask));
    }
}
