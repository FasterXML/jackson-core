package com.fasterxml.jackson.core.json;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.IOContext;

/**
 * Intermediate base class shared by JSON-backed generators
 * like {@link UTF8JsonGenerator} and {@link WriterBasedJsonGenerator}.
 */
public abstract class JsonGeneratorBase extends GeneratorBase
{
    /*
    /**********************************************************************
    /* Constants
    /**********************************************************************
     */

    /**
     * This is the default set of escape codes, over 7-bit ASCII range
     * (first 128 character codes), used for single-byte UTF-8 characters.
     */
    protected final static int[] DEFAULT_OUTPUT_ESCAPES = CharTypes.get7BitOutputEscapes();

    /*
    /**********************************************************************
    /* Configuration, basic I/O, features
    /**********************************************************************
     */

    protected final IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link com.fasterxml.jackson.core.json.JsonWriteFeature}s
     * are enabled.
     */
    protected int _formatWriteFeatures;

    /*
    /**********************************************************************
    /* Configuration, output escaping
    /**********************************************************************
     */

    /**
     * Currently active set of output escape code definitions (whether
     * and how to escape or not) for 7-bit ASCII range (first 128
     * character codes). Defined separately to make potentially
     * customizable
     */
    protected int[] _outputEscapes = DEFAULT_OUTPUT_ESCAPES;

    /**
     * Definition of custom character escapes to use for generators created
     * by this factory, if any. If null, standard data format specific
     * escapes are used.
     *<p>
     * NOTE: although typically set during construction (in constructor),
     * can not be made final in 3.0 due to some edge use cases (JSONP support).
     */
    protected CharacterEscapes _characterEscapes;

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

    /*
    /**********************************************************************
    /* Configuration, other
    /**********************************************************************
     */

    /**
     * Object that handles pretty-printing (usually additional
     * white space to make results more human-readable) during
     * output. If null, no pretty-printing is done.
     */
    protected PrettyPrinter _cfgPrettyPrinter;

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
     * Flag set to indicate that implicit conversion from number
     * to JSON String is needed (as per
     * {@link com.fasterxml.jackson.core.StreamWriteFeature#WRITE_NUMBERS_AS_STRINGS}).
     */
    protected boolean _cfgNumbersAsStrings;

    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */

    /**
     * Object that keeps track of the current contextual state of the generator.
     */
    protected JsonWriteContext _tokenWriteContext;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public JsonGeneratorBase(ObjectWriteContext writeCtxt, IOContext ctxt,
            int streamWriteFeatures, int formatWriteFeatures,
            SerializableString rootValueSeparator, PrettyPrinter pp,
            CharacterEscapes charEsc, int maxNonEscaped)
    {
        super(writeCtxt, streamWriteFeatures);
        _ioContext = ctxt;
        _formatWriteFeatures = formatWriteFeatures;
        // By default we use this feature to determine additional quoting
        if (JsonWriteFeature.ESCAPE_NON_ASCII.enabledIn(formatWriteFeatures)) {
            // note! Lowest effective value is 127 (0 is used as marker, but values
            // from 1 through 126 have no effect different from 127), so:
            maxNonEscaped = 127;
        }
        _maximumNonEscapedChar = maxNonEscaped;
        _cfgUnqNames = !JsonWriteFeature.QUOTE_FIELD_NAMES.enabledIn(formatWriteFeatures);
        _cfgNumbersAsStrings = JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.enabledIn(formatWriteFeatures);
        _rootValueSeparator = rootValueSeparator;

        _cfgPrettyPrinter = pp;

        final DupDetector dups = StreamWriteFeature.STRICT_DUPLICATE_DETECTION.enabledIn(streamWriteFeatures)
                ? DupDetector.rootDetector(this) : null;
        _tokenWriteContext = JsonWriteContext.createRootContext(dups);

        // 03-Oct-2017, tatu: Not clean (shouldn't call non-static methods from ctor),
        //    but for now best way to avoid code duplication
        setCharacterEscapes(charEsc);
    }

    /*
    /**********************************************************************
    /* Versioned, accessors
    /**********************************************************************
     */

    @Override public Version version() { return PackageVersion.VERSION; }

    public boolean isEnabled(JsonWriteFeature f) { return f.enabledIn(_formatWriteFeatures); }
    
    /*
    /**********************************************************************
    /* Overridden configuration methods
    /**********************************************************************
     */
    
    @Override
    public int formatWriteFeatures() {
        return _formatWriteFeatures;
    }
    
    @Override
    public JsonGenerator setHighestNonEscapedChar(int charCode) {
        _maximumNonEscapedChar = (charCode < 0) ? 0 : charCode;
        return this;
    }

    @Override
    public int getHighestNonEscapedChar() {
        return _maximumNonEscapedChar;
    }

    @Override
    public abstract JsonGenerator setCharacterEscapes(CharacterEscapes esc);

    /**
     * Method for accessing custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     */
    @Override
    public CharacterEscapes getCharacterEscapes() {
        return _characterEscapes;
    }

    /*
    /**********************************************************************
    /* Overridden output state handling methods
    /**********************************************************************
     */
    
    @Override
    public final TokenStreamContext getOutputContext() { return _tokenWriteContext; }

    @Override
    public final Object getCurrentValue() {
        return _tokenWriteContext.getCurrentValue();
    }

    @Override
    public final void setCurrentValue(Object v) {
        _tokenWriteContext.setCurrentValue(v);
    }

    /*
    /**********************************************************************
    /* Partial API, structural
    /**********************************************************************
     */

    @Override
    public void writeStartArray(Object currentValue, int size) throws IOException {
        writeStartArray(currentValue);
    }

    @Override
    public void writeStartObject(Object currentValue, int size) throws IOException {
        writeStartObject(currentValue);
    }

    /*
    /**********************************************************************
    /* Partial API, field names, name+value
    /**********************************************************************
     */

   @Override
    public void writeFieldId(long id) throws IOException {
        writeFieldName(Long.toString(id));
    }

    /*
    /**********************************************************************
    /* Shared helper methods
    /**********************************************************************
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
            if (_tokenWriteContext.inArray()) {
                _cfgPrettyPrinter.beforeArrayValues(this);
            } else if (_tokenWriteContext.inObject()) {
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
                typeMsg, _tokenWriteContext.typeDesc()));
    }
}
