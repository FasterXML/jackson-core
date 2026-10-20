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
    // 19-Oct-2026, pjfanning: [core#1784] padding uses 0xFF bytes, so a last quad
    //   starting with 0xFF could match a shorter name in the symbol table, skipping
    //   UTF-8 validation of the name; 0xFF is never valid in UTF-8, so reject it here
    // @since 2.23
    protected final int _padLastQuad(int q, int bytes) throws JsonParseException {
        if (((q >>> ((bytes - 1) << 3)) & 0xFF) == 0xFF) {
            _reportInvalidInitial(0xFF);
        }
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
        if (needed == 1) {
            if (ch < 0x80) {
                _reportInvalidUTF8Overlong(ch, 2, _inputPtr);
            }
        } else if (needed == 2) {
            _verifyUTF8CodePoint3(ch, _inputPtr);
        } else {
            _verifyUTF8CodePoint4(ch, _inputPtr);
        }
    }

    /**
     * Method for verifying code point decoded from a 3-byte UTF-8 sequence:
     * overlong (non-shortest form) encodings and surrogates are invalid (RFC 3629).
     *<p>
     * No such check is needed for 2-byte sequences whose length is determined using
     * {@code CharTypes.getInputCodeUtf8()} (or tables derived from it), since lead bytes
     * 0xC0 and 0xC1 of overlong ones are marked invalid there; other callers
     * (like property name decoding) need to check those separately.
     *
     * @param ch Decoded code point
     * @param ptr Input pointer to set (for error location) if code point is invalid
     *
     * @throws JsonParseException If code point is invalid
     *
     * @since 2.23
     */
    protected final void _verifyUTF8CodePoint3(int ch, int ptr) throws JsonParseException {
        if (ch < 0x800) {
            _reportInvalidUTF8Overlong(ch, 3, ptr);
        }
        // [jackson-core#363]: Surrogates (0xD800 - 0xDFFF) are illegal in UTF-8
        if ((ch & 0xF800) == 0xD800) {
            _inputPtr = ptr;
            _reportInvalidUTF8Surrogate(ch);
        }
    }

    /**
     * Method for verifying code point decoded from a 4-byte UTF-8 sequence:
     * overlong (non-shortest form) encodings and code points beyond U+10FFFF
     * are invalid (RFC 3629).
     *
     * @param ch Decoded code point (not yet offset by -0x10000)
     * @param ptr Input pointer to set (for error location) if code point is invalid
     *
     * @throws JsonParseException If code point is invalid
     *
     * @since 2.23
     */
    protected final void _verifyUTF8CodePoint4(int ch, int ptr) throws JsonParseException {
        if (ch < 0x10000) {
            _reportInvalidUTF8Overlong(ch, 4, ptr);
        }
        if (ch > 0x10FFFF) {
            _inputPtr = ptr;
            _reportError("Invalid UTF-8: code point 0x"+Integer.toHexString(ch)+" beyond U+10FFFF");
        }
    }

    /**
     * Method for verifying lead byte and first continuation byte of a 3- or 4-byte
     * UTF-8 sequence that is skipped instead of decoded: together they determine
     * whether sequence is overlong, a surrogate or beyond U+10FFFF (Unicode Table 3-7).
     *
     * @param lead Lead byte (unsigned)
     * @param second First continuation byte (unsigned)
     *
     * @throws JsonParseException If sequence is invalid
     *
     * @since 2.23
     */
    protected final void _verifyUTF8SecondByte(int lead, int second) throws JsonParseException {
        // 19-Oct-2026, tatu: [core#1782] only these lead bytes restrict second byte range
        switch (lead) {
        case 0xE0:
        case 0xF0:
            _verifyUTF8NotOverlong(lead, second);
            break;
        case 0xED:
            if (second >= 0xA0) {
                _reportInvalidUTF8SecondByte("surrogate", lead, second);
            }
            break;
        case 0xF4:
            if (second >= 0x90) {
                _reportInvalidUTF8SecondByte("code point beyond U+10FFFF", lead, second);
            }
            break;
        }
    }

    // 19-Oct-2026, tatu: [core#1782] Off-lined to keep `_verifyUTF8CodePoint3()` and
    //   `_verifyUTF8CodePoint4()` small (inlinable)
    private void _reportInvalidUTF8Overlong(int ch, int byteCount, int ptr) throws JsonParseException {
        _inputPtr = ptr;
        _reportError("Invalid UTF-8: overlong "+byteCount+"-byte encoding of 0x"+Integer.toHexString(ch));
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

    // 19-Oct-2026, tatu: [core#1787] Best-effort decoding of (possibly multi-byte)
    //   UTF-8 char starting at `buf[ptr]`, for error messages only: returns lead
    //   byte as-is if sequence is invalid (incl. overlong, surrogate, beyond U+10FFFF)
    //   or not complete before `end`
    // @since 2.23
    protected final static int _decodeUTF8ForError(byte[] buf, int ptr, int end) {
        final int c = buf[ptr] & 0xFF;
        final int needed = _utf8ContinuationCount(c);
        if ((needed < 0) || ((ptr + needed) >= end)) {
            return c;
        }
        int value = c & (0x3F >> needed); // 0x1F, 0x0F or 0x07
        for (int i = 1; i <= needed; ++i) {
            final int d = buf[ptr + i];
            if ((d & 0xC0) != 0x080) {
                return c;
            }
            value = (value << 6) | (d & 0x3F);
        }
        final int min = (needed == 1) ? 0x80 : ((needed == 2) ? 0x800 : 0x10000);
        if ((value < min) || (value > 0x10FFFF) || ((value >= 0xD800) && (value <= 0xDFFF))) {
            return c;
        }
        return value;
    }

    // 10-Oct-2026, tatu: [core#1756] Verifies that multi-byte UTF-8 character with
    //   lead byte `lead` and first continuation byte `second` is not overlong: shortest
    //   form of 3-byte (lead 0xE0) and 4-byte (lead 0xF0) characters needs `second` of
    //   at least 0xA0 and 0x90, respectively (Unicode Table 3-7). Overlong 2-byte ones
    //   are excluded by `_utf8ContinuationCount()` (lead 0xC0/0xC1).
    // @since 2.23
    protected void _verifyUTF8NotOverlong(int lead, int second) throws JsonParseException {
        if ((lead == 0xE0 && second < 0xA0) || (lead == 0xF0 && second < 0x90)) {
            _reportInvalidUTF8SecondByte("overlong encoding", lead, second);
        }
    }

    private void _reportInvalidUTF8SecondByte(String desc, int lead, int second) throws JsonParseException {
        _reportError("Invalid UTF-8: "+desc+" (lead byte 0x"+Integer.toHexString(lead)
                +", second byte 0x"+Integer.toHexString(second)+")");
    }

    // 10-Oct-2026, tatu: [core#1744], [core#1756] Handles (non-overlong) multi-byte UTF-8
    //   character after backslash, decoded as `cp`: supplementary characters (cannot be
    //   returned as `char`) and surrogates (invalid in UTF-8) are rejected, instead of being
    //   truncated. Others are passed to `_handleUnrecognizedCharacterEscape()`, like ASCII
    //   characters.
    // @since 2.23
    protected char _handleEscapedUTF8Char(int cp) throws IOException {
        if (cp > 0xFFFF || (cp >= 0xD800 && cp <= 0xDFFF)) {
            throw _constructReadException("Unrecognized character escape "+_getCharDesc(cp),
                    _currentLocationMinusOne());
        }
        return _handleUnrecognizedCharacterEscape((char) cp);
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
