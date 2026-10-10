package com.fasterxml.jackson.core.json;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberTypeFP;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.IOContext;
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
