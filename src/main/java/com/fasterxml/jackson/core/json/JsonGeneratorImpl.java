package com.fasterxml.jackson.core.json;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.VersionUtil;

/**
 * Intermediate base class shared by JSON-backed generators
 * like {@link UTF8JsonGenerator} and {@link WriterBasedJsonGenerator}.
 */
public abstract class JsonGeneratorImpl extends GeneratorBase
{
    /*
    /**********************************************************
    /* Constants
    /**********************************************************
     */

    /**
     * This is the default set of escape codes, over 7-bit ASCII range
     * (first 128 character codes), used for single-byte UTF-8 characters.
     */
    protected final static int[] sOutputEscapes = CharTypes.get7BitOutputEscapes();

    /*
    /**********************************************************
    /* Configuration, basic I/O, features
    /**********************************************************
     */

    protected final IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link com.fasterxml.jackson.core.json.JsonWriteFeature}s
     * are enabled.
     */
    protected int _formatWriteFeatures;

    /*
    /**********************************************************
    /* Configuration, output escaping
    /**********************************************************
     */

    /**
     * Currently active set of output escape code definitions (whether
     * and how to escape or not) for 7-bit ASCII range (first 128
     * character codes). Defined separately to make potentially
     * customizable
     */
    protected int[] _outputEscapes = sOutputEscapes;

    /**
     * Value between 128 (0x80) and 65535 (0xFFFF) that indicates highest
     * Unicode code point that will not need escaping; or 0 to indicate
     * that all characters can be represented without escaping.
     * Typically used to force escaping of some portion of character set;
     * for example to always escape non-ASCII characters (if value was 127).
     *<p>
     * NOTE: not all sub-classes make use of this setting.
     */
    protected int _maximumNonEscapedChar;

    /**
     * Definition of custom character escapes to use for generators created
     * by this factory, if any. If null, standard data format specific
     * escapes are used.
     *<p>
     * NOTE: although typically set during construction (in constructor),
     * can not be made final in 3.0 due to some edge use cases (JSONP support).
     */
    protected CharacterEscapes _characterEscapes;
    
    /*
    /**********************************************************
    /* Configuration, other
    /**********************************************************
     */

    /**
     * Separator to use, if any, between root-level values.
     */
    protected final SerializableString _rootValueSeparator;

    /**
     * Flag that is set if quoting is not to be added around
     * JSON Object property names.
     */
    protected boolean _cfgUnqNames;

    /**
     * Object that handles pretty-printing (usually additional
     * white space to make results more human-readable) during
     * output. If null, no pretty-printing is done.
     */
    protected PrettyPrinter _cfgPrettyPrinter;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public JsonGeneratorImpl(ObjectWriteContext writeCtxt, IOContext ctxt,
            int streamWriteFeatures, int formatWriteFeatures,
            SerializableString rvs, CharacterEscapes charEsc, PrettyPrinter pp)
    {
        super(writeCtxt, streamWriteFeatures);
        _ioContext = ctxt;
        _formatWriteFeatures = formatWriteFeatures;
        // By default we use this feature to determine additional quoting
        if (JsonWriteFeature.ESCAPE_NON_ASCII.enabledIn(formatWriteFeatures)) {
            // inlined `setHighestNonEscapedChar()`
            _maximumNonEscapedChar = 127;
        }
        _cfgUnqNames = !JsonWriteFeature.QUOTE_FIELD_NAMES.enabledIn(formatWriteFeatures);
        _rootValueSeparator = rvs;

        _cfgPrettyPrinter = pp;
        // 03-Oct-2017, tatu: Not clean (shouldn't call non-static methods from ctor),
        //    but for now best way to avoid code duplication
        setCharacterEscapes(charEsc);
    }

    /*
    /**********************************************************
    /* Versioned
    /**********************************************************
     */

    @Override
    public Version version() {
        return VersionUtil.versionFor(getClass());
    }

    /*
    /**********************************************************
    /* Overridden configuration methods
    /**********************************************************
     */

    @Override
    public JsonGenerator setHighestNonEscapedChar(int charCode) {
        _maximumNonEscapedChar = (charCode < 0) ? 0 : charCode;
        return this;
    }

    @Override
    public int getHighestEscapedChar() {
        return _maximumNonEscapedChar;
    }

    @Override
    public JsonGenerator setCharacterEscapes(CharacterEscapes esc)
    {
        _characterEscapes = esc;
        if (esc == null) { // revert to standard escapes
            _outputEscapes = sOutputEscapes;
        } else {
            _outputEscapes = esc.getEscapeCodesForAscii();
        }
        return this;
    }

    /**
     * Method for accessing custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     */
    @Override
    public CharacterEscapes getCharacterEscapes() {
        return _characterEscapes;
    }

    @Override
    public JsonGenerator setPrettyPrinter(PrettyPrinter pp) {
        _cfgPrettyPrinter = pp;
        return this;
    }

    @Override
    public PrettyPrinter getPrettyPrinter() {
        return _cfgPrettyPrinter;
    }
    
    /*
    /**********************************************************
    /* Partial API
    /**********************************************************
     */

    // // Overrides just to make things final, to possibly help with inlining
    
    @Override
    public final void writeStringField(String fieldName, String value) throws IOException
    {
        writeFieldName(fieldName);
        writeString(value);
    }

    /*
    /**********************************************************
    /* Shared helper methods
    /**********************************************************
     */

    protected void _verifyPrettyValueWrite(String typeMsg, int status) throws IOException
    {
        // If we have a pretty printer, it knows what to do:
        switch (status) {
        case JsonWriteContext.STATUS_OK_AFTER_COMMA: // array
            _cfgPrettyPrinter.writeArrayValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AFTER_COLON:
            _cfgPrettyPrinter.writeObjectFieldValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AFTER_SPACE:
            _cfgPrettyPrinter.writeRootValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AS_IS:
            // First entry, but of which context?
            if (_outputContext.inArray()) {
                _cfgPrettyPrinter.beforeArrayValues(this);
            } else if (_outputContext.inObject()) {
                _cfgPrettyPrinter.beforeObjectEntries(this);
            }
            break;
        case JsonWriteContext.STATUS_EXPECT_NAME:
            _reportCantWriteValueExpectName(typeMsg);
            break;
        default:
            _throwInternal();
            break;
        }
    }

    protected void _reportCantWriteValueExpectName(String typeMsg) throws IOException
    {
        _reportError(String.format("Can not %s, expecting field name (context: %s)",
                typeMsg, _outputContext.typeDesc()));
    }
}
