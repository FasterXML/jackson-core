package tools.jackson.core.json;

import tools.jackson.core.*;
import tools.jackson.core.base.GeneratorBase;
import tools.jackson.core.io.CharTypes;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.JacksonFeatureSet;

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

    /**
     * Low-level I/O context used mostly for buffer recycling.
     */
    protected final IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link tools.jackson.core.json.JsonWriteFeature}s
     * are enabled.
     */
    protected final int _formatWriteFeatures;

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
    protected final int _maximumNonEscapedChar;

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
    protected final PrettyPrinter _cfgPrettyPrinter;

    /**
     * Separator to use, if any, between root-level values.
     */
    protected final SerializableString _rootValueSeparator;

    /**
     * Flag that is set if quoting is not to be added around
     * JSON Object property names.
     */
    protected final boolean _cfgUnqNames;

    /**
     * Flag set to indicate that implicit conversion from number
     * to JSON String is needed (as per
     * {@link tools.jackson.core.json.JsonWriteFeature#WRITE_NUMBERS_AS_STRINGS}).
     */
    protected final boolean _cfgNumbersAsStrings;

    /**
     * Whether to write Hex values with upper-case letters (true)
     * or lower-case (false)
     */
    protected boolean _cfgWriteHexUppercase;

    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */

    /**
     * Object that keeps track of the current contextual state of the generator.
     */
    protected JsonWriteContext _streamWriteContext;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected JsonGeneratorBase(ObjectWriteContext writeCtxt, IOContext ctxt,
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
        _cfgUnqNames = !JsonWriteFeature.QUOTE_PROPERTY_NAMES.enabledIn(formatWriteFeatures);
        _cfgNumbersAsStrings = JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.enabledIn(formatWriteFeatures);
        _cfgWriteHexUppercase = JsonWriteFeature.WRITE_HEX_UPPER_CASE.enabledIn(formatWriteFeatures);
        _rootValueSeparator = rootValueSeparator;

        _cfgPrettyPrinter = pp;

        final DupDetector dups = StreamWriteFeature.STRICT_DUPLICATE_DETECTION.enabledIn(streamWriteFeatures)
                ? DupDetector.rootDetector(this) : null;
        _streamWriteContext = JsonWriteContext.createRootContext(dups);

        // 03-Oct-2017, tatu: Not clean (shouldn't call non-static methods from ctor),
        //    but for now best way to avoid code duplication
        setCharacterEscapes(charEsc);
    }

    /*
    /**********************************************************************
    /* Versioned, accessors, capabilities
    /**********************************************************************
     */

    @Override public Version version() { return PackageVersion.VERSION; }

    /*
    /**********************************************************************
    /* Constraints violation checking
    /**********************************************************************
     */

    @Override
    public StreamWriteConstraints streamWriteConstraints() {
        return _ioContext.streamWriteConstraints();
    }

    public boolean isEnabled(JsonWriteFeature f) { return f.enabledIn(_formatWriteFeatures); }

    @Override
    public JacksonFeatureSet<StreamWriteCapability> streamWriteCapabilities() {
        return DEFAULT_WRITE_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* Overridden configuration methods
    /**********************************************************************
     */

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
    public final TokenStreamContext streamWriteContext() { return _streamWriteContext; }

    @Override
    public final Object currentValue() {
        return _streamWriteContext.currentValue();
    }

    @Override
    public final void assignCurrentValue(Object v) {
        _streamWriteContext.assignCurrentValue(v);
    }

    /*
    /**********************************************************************
    /* Partial API, structural
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeStartArray(Object currentValue, int size) throws JacksonException {
        writeStartArray(currentValue);
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(Object currentValue, int size) throws JacksonException {
        writeStartObject(currentValue);
        return this;
    }

    /*
    /**********************************************************************
    /* Partial API, Object property names/ids
    /**********************************************************************
     */

   @Override
    public JsonGenerator writePropertyId(long id) throws JacksonException {
        writeName(Long.toString(id));
        return this;
    }

    /*
    /**********************************************************************
    /* Shared helper methods
    /**********************************************************************
     */

    protected void _verifyPrettyValueWrite(String typeMsg, int status) throws JacksonException
    {
        // If we have a pretty printer, it knows what to do:
        switch (status) {
        case JsonWriteContext.STATUS_OK_AFTER_COMMA: // array
            _cfgPrettyPrinter.writeArrayValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AFTER_COLON:
            _cfgPrettyPrinter.writeObjectNameValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AFTER_SPACE:
            _cfgPrettyPrinter.writeRootValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AS_IS:
            // First entry, but of which context?
            if (_streamWriteContext.inArray()) {
                _cfgPrettyPrinter.beforeArrayValues(this);
            } else if (_streamWriteContext.inObject()) {
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

    protected void _reportCantWriteValueExpectName(String typeMsg) throws JacksonException
    {
        throw _constructWriteException("Cannot %s, expecting a property name (context: %s)",
                typeMsg, _streamWriteContext.typeDesc());
    }
}
