package com.fasterxml.jackson.core.json;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberTypeFP;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;

/**
 * Another intermediate base class, only used by actual JSON-backed parser
 * implementations.
 *
 * @since 2.17
 */
public abstract class JsonParserBase
    extends ParserBase
{
    @SuppressWarnings("deprecation")
    protected final static int FEAT_MASK_TRAILING_COMMA = Feature.ALLOW_TRAILING_COMMA.getMask();
    @SuppressWarnings("deprecation")
    protected final static int FEAT_MASK_LEADING_ZEROS = Feature.ALLOW_NUMERIC_LEADING_ZEROS.getMask();
    @SuppressWarnings("deprecation")
    protected final static int FEAT_MASK_NON_NUM_NUMBERS = Feature.ALLOW_NON_NUMERIC_NUMBERS.getMask();
    @SuppressWarnings("deprecation")
    protected final static int FEAT_MASK_ALLOW_MISSING = Feature.ALLOW_MISSING_VALUES.getMask();
    @SuppressWarnings("deprecation")
    protected final static int FEAT_MASK_ALLOW_CTRL_RS = Feature.ALLOW_RS_CONTROL_CHAR.getMask();
    
    protected final static int FEAT_MASK_ALLOW_SINGLE_QUOTES = Feature.ALLOW_SINGLE_QUOTES.getMask();
    protected final static int FEAT_MASK_ALLOW_UNQUOTED_NAMES = Feature.ALLOW_UNQUOTED_FIELD_NAMES.getMask();
    protected final static int FEAT_MASK_ALLOW_JAVA_COMMENTS = Feature.ALLOW_COMMENTS.getMask();
    protected final static int FEAT_MASK_ALLOW_YAML_COMMENTS = Feature.ALLOW_YAML_COMMENTS.getMask();

    // Latin1 encoding is not supported, but we do use 8-bit subset for
    // pre-processing task, to simplify first pass, keep it fast.
    protected final static int[] INPUT_CODES_LATIN1 = CharTypes.getInputCodeLatin1();

    // This is the main input-code lookup table, fetched eagerly
    protected final static int[] INPUT_CODES_UTF8 = CharTypes.getInputCodeUtf8();

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Codec used for data binding when (if) requested; typically full
     * <code>ObjectMapper</code>, but that abstract is not part of core
     * package.
     */
    protected ObjectCodec _objectCodec;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected JsonParserBase(IOContext ioCtxt, int features, ObjectCodec codec) {
        super(ioCtxt, features);
        _objectCodec = codec;
    }

    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

    /*
    /**********************************************************************
    /* Capability introspection
    /**********************************************************************
     */

    @Override
    public final JacksonFeatureSet<StreamReadCapability> getReadCapabilities() {
        return JSON_READ_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* Overrides
    /**********************************************************************
     */

    /**
     * JSON format does not have native information on "correct" floating-point
     * type to use, unlike some formats (most binary formats), so it needs to
     * indicate this as {@link NumberTypeFP#UNKNOWN}.
     *
     * @return Natural floating-point type if known; {@link NumberTypeFP#UNKNOWN} for
     *    all JSON-backed parsers.
     */
    @Override // added in 2.17
    public NumberTypeFP getNumberTypeFP() throws IOException {
        return NumberTypeFP.UNKNOWN;
    }

    /*
    /**********************************************************************
    /* Location handling
    /**********************************************************************
     */

    // First: override some methods as abstract to force definition by subclasses
    
    @Override
    public abstract JsonLocation currentLocation();

    @Override
    public abstract JsonLocation currentTokenLocation();

    @Override
    protected abstract JsonLocation _currentLocationMinusOne();

    @Deprecated // since 2.17
    @Override
    public final JsonLocation getCurrentLocation() {
        return currentLocation();
    }
    
    @Deprecated // since 2.17
    @Override
    public final JsonLocation getTokenLocation() {
        return currentTokenLocation();
    }

    /*
    /**********************************************************************
    /* Other helper methods
    /**********************************************************************
     */

    // @since 2.19
    protected boolean _isAllowedCtrlCharRS(int i) {
        return (i == INT_RS)  && (_features & FEAT_MASK_ALLOW_CTRL_RS) != 0;
    }

    // 09-Oct-2026, tatu: [core#1746] Decimal point must be followed by a digit,
    //   unless trailing decimal point is allowed AND there is an integer part
    // @since 2.21.8
    protected boolean _missingFractionDigits(int fractLen, int intLen) {
        return (fractLen == 0)
                && ((intLen == 0) || !isEnabled(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS.mappedFeature()));
    }

    // @since 2.21.8
    protected void _verifyFractionDigits(int fractLen, int intLen, int ch) throws JsonParseException {
        if (_missingFractionDigits(fractLen, intLen)) {
            _reportUnexpectedNumberChar(ch, "Decimal point not followed by a digit");
        }
    }

    // @since 2.21.8
    protected void _reportLeadingPlusSignNotAllowed() throws JsonParseException {
        _reportUnexpectedNumberChar('+', "JSON spec does not allow numbers to have plus signs: enable `JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS` to allow");
    }

    // 09-Oct-2026, tatu: [core#1748] Byte-based parsers accept all multi-byte UTF-8
    //   characters when scanning unquoted names, so decoded name must be verified to
    //   only contain chars `ReaderBasedJsonParser` accepts (Java identifier parts)
    private void _verifyUnquotedName(String name) throws JsonParseException {
        for (int i = 0, len = name.length(); i < len; ++i) {
            final char c = name.charAt(i);
            if ((c > 0x7F) && !Character.isJavaIdentifierPart(c)) {
                _reportUnexpectedChar(c, (i == 0)
                        ? "was expecting either valid name character (for unquoted name) or double-quote (for quoted) to start field name"
                        : "was expecting a colon to separate field name and value");
            }
        }
    }

    // 09-Oct-2026, tatu: [core#1750] Handling of a String value char that is neither
    //   escape nor valid (UTF-8) start char: control chars are only allowed (and returned
    //   from) with ALLOW_UNESCAPED_CONTROL_CHARS; must not be reduced to a plain
    //   "_reportInvalidChar()" call.
    // @since 2.21.8
    protected void _handleInvalidStringChar(int c) throws IOException {
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
     * @since 2.23
     */
    // 09-Oct-2026, tatu: [core#1748] Moved from `UTF8StreamJsonParser`,
    //   `UTF8DataInputJsonParser` and `NonBlockingJsonParserBase`
    protected final String _decodeAndAddUTF8Name(ByteQuadsCanonicalizer symbols,
            int[] quads, int qlen, int lastQuadBytes)
        throws JsonParseException, StreamConstraintsException
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
     * @since 2.23
     */
    protected final String _findOrAddUnquotedUTF8Name(ByteQuadsCanonicalizer symbols,
            int[] quads, int qlen, int lastQuadBytes)
        throws JsonParseException, StreamConstraintsException
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
        throws JsonParseException, StreamConstraintsException
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
                    _reportError("Invalid UTF-8: incomplete multi-byte sequence in field name");
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
    // @since 2.23
    protected final static int _padLastQuad(int q, int bytes) {
        return (bytes == 4) ? q : (q | (-1 << (bytes << 3)));
    }

    // 09-Oct-2026, tatu: [core#1748] Moved from `UTF8StreamJsonParser`,
    //   `UTF8DataInputJsonParser` and `NonBlockingJsonParserBase`
    // @since 2.23
    protected void _reportInvalidOther(int mask) throws JsonParseException {
        _reportError("Invalid UTF-8 middle byte 0x"+Integer.toHexString(mask));
    }

    // @since 2.23
    protected void _reportInvalidOther(int mask, int ptr) throws JsonParseException {
        _inputPtr = ptr;
        _reportInvalidOther(mask);
    }

    // 09-Oct-2026, tatu: [core#1748] rejects overlong encodings, surrogates and code points
    //   beyond U+10FFFF for code point decoded from multi-byte UTF-8 sequence (with `needed`
    //   continuation bytes) in a property name
    private void _verifyUTF8NameCodePoint(int ch, int needed) throws JsonParseException {
        _verifyUTF8NotOverlong(ch, needed);
        if (needed == 2) {
            // [jackson-core#363]: Surrogates (0xD800 - 0xDFFF) are illegal in UTF-8
            if (ch >= 0xD800 && ch <= 0xDFFF) {
                _reportInvalidUTF8Surrogate(ch);
            }
        } else if (ch > 0x10FFFF) {
            _reportError("Invalid UTF-8: code point 0x"+Integer.toHexString(ch)+" beyond U+10FFFF");
        }
    }

    private void _verifyUTF8NotOverlong(int ch, int needed) throws JsonParseException {
        final int min = (needed == 1) ? 0x80 : ((needed == 2) ? 0x800 : 0x10000);
        if (ch < min) {
            _reportError("Invalid UTF-8: overlong "+(needed+1)+"-byte encoding of 0x"+Integer.toHexString(ch));
        }
    }

    // 10-Oct-2026, tatu: [core#1756] Number of continuation bytes for multi-byte
    //   UTF-8 lead byte; or -1 if not a valid lead byte (including 0xC0/0xC1 that
    //   only start overlong encodings, and 0xF5 - 0xF7 that start ones beyond U+10FFFF)
    // @since 2.23
    protected final static int _utf8ContinuationCount(int lead) {
        if (lead < 0xC2) {
            return -1;
        }
        if (lead < 0xE0) {
            return 1;
        }
        if (lead < 0xF0) {
            return 2;
        }
        return (lead < 0xF5) ? 3 : -1;
    }

    // 10-Oct-2026, tatu: [core#1744], [core#1756] Handles multi-byte UTF-8 character
    //   after backslash (with `needed` continuation bytes) decoded as `cp`: overlong
    //   encodings are rejected as invalid UTF-8; supplementary characters (cannot be
    //   returned as `char`) and surrogates (invalid in UTF-8) as unrecognized escapes,
    //   instead of being truncated. Others are passed to
    //   `_handleUnrecognizedCharacterEscape()`, like ASCII characters.
    // @since 2.23
    protected char _handleEscapedUTF8Char(int cp, int needed) throws IOException {
        _verifyUTF8NotOverlong(cp, needed);
        if (cp > 0xFFFF || (cp >= 0xD800 && cp <= 0xDFFF)) {
            // point to lead byte of character, not its last byte
            throw _constructReadException("Unrecognized character escape "+_getCharDesc(cp),
                    _currentLocationMinus(needed + 1));
        }
        return _handleUnrecognizedCharacterEscape((char) cp);
    }

    /**
     * Variant of {@link #_currentLocationMinusOne()} for location of {@code count}
     * bytes (or chars) before current input position, used to point to start of a
     * multi-byte character just decoded. Default implementation simply delegates to
     * {@link #_currentLocationMinusOne()}.
     *
     * @param count Number of bytes (or chars) to go back
     *
     * @return Location {@code count} bytes (or chars) before current position
     *
     * @since 2.23
     */
    protected JsonLocation _currentLocationMinus(int count) {
        return _currentLocationMinusOne();
    }

    /**
     * Method called for an unescaped linefeed (allowed by
     * {@link JsonReadFeature#ALLOW_UNESCAPED_CONTROL_CHARS}) within a String value,
     * to update row tracking. Default implementation does nothing.
     *
     * @param c Linefeed character ({@code '\r'} or {@code '\n'})
     *
     * @throws IOException for low-level read issues
     *
     * @since 2.21.8
     */
    protected void _handleLinefeedInString(int c) throws IOException { }

    // @since 2.21.8 (moved from sub-classes)
    protected void _reportInvalidChar(int c) throws JsonParseException {
        // Either invalid WS or illegal UTF-8 start char
        if (c < INT_SPACE) {
            _throwInvalidSpace(c);
        }
        _reportInvalidInitial(c);
    }

    // @since 2.21.8 (moved from sub-classes)
    protected void _reportInvalidInitial(int mask) throws JsonParseException {
        _reportError("Invalid UTF-8 start byte 0x"+Integer.toHexString(mask));
    }
}
