/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package tools.jackson.core.json;

import java.io.*;
import java.util.List;
import java.util.Locale;

import tools.jackson.core.*;
import tools.jackson.core.base.TextualTSFactory;
import tools.jackson.core.io.*;
import tools.jackson.core.json.async.NonBlockingByteArrayJsonParser;
import tools.jackson.core.json.async.NonBlockingByteBufferJsonParser;
import tools.jackson.core.sym.BinaryNameMatcher;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;
import tools.jackson.core.sym.CharsToNameCanonicalizer;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Named;

/**
 * JSON-backed {@link TokenStreamFactory} implementation that will create
 * token readers ("parsers") and writers ("generators") for handling
 * JSON-encoded content.
 *<p>
 * Note that this class used to reside at main {@code tools.jackson.core}
 * in 2.x, but moved here to denote its changed role as implementation,
 * not base class for factories.
 */
public class JsonFactory
    extends TextualTSFactory
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1;

    /*
    /**********************************************************************
    /* Constants
    /**********************************************************************
     */

    /**
     * Name used to identify JSON format
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_JSON = "JSON";

    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_JSON_PARSER_FEATURE_FLAGS = JsonReadFeature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_JSON_GENERATOR_FEATURE_FLAGS = JsonWriteFeature.collectDefaults();

    public final static SerializableString DEFAULT_ROOT_VALUE_SEPARATOR = DefaultPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR;

    public final static char DEFAULT_QUOTE_CHAR = '"';

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Definition of custom character escapes to use for generators created
     * by this factory, if any. If null, standard data format specific
     * escapes are used.
     */
    protected final CharacterEscapes _characterEscapes;

    /**
     * Separator used between root-level values, if any; null indicates
     * "do not add separator".
     * Default separator is a single space character.
     */
    protected final SerializableString _rootValueSeparator;

    /**
     * Optional threshold used for automatically escaping character above certain character
     * code value: either {@code 0} to indicate that no threshold is specified, or value
     * at or above 127 to indicate last character code that is NOT automatically escaped
     * (but depends on other configuration rules for checking).
     */
    protected final int _maximumNonEscapedChar;

    /**
     * Character used for quoting property names (if property name quoting has not
     * been disabled with {@link JsonWriteFeature#QUOTE_PROPERTY_NAMES})
     * and JSON String values.
     */
    protected final char _quoteChar;

    /*
    /**********************************************************************
    /* Symbol table management
    /**********************************************************************
     */

    /**
     * Each factory comes equipped with a shared root symbol table.
     * It should not be linked back to the original blueprint, to
     * avoid contents from leaking between factories.
     */
    protected final transient CharsToNameCanonicalizer _rootCharSymbols;

    /**
     * Alternative to the basic symbol table, some stream-based
     * parsers use different name canonicalization method.
     */
    protected final transient ByteQuadsCanonicalizer _byteSymbolCanonicalizer = ByteQuadsCanonicalizer.createRoot();

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    /**
     * Default constructor used to create factory instances.
     * Creation of a factory instance is a light-weight operation,
     * but it is still a good idea to reuse limited number of
     * factory instances (and quite often just a single instance):
     * factories are used as context for storing some reused
     * processing objects (such as symbol tables parsers use)
     * and this reuse only works within context of a single
     * factory instance.
     */
    public JsonFactory() {
        super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                DEFAULT_JSON_PARSER_FEATURE_FLAGS, DEFAULT_JSON_GENERATOR_FEATURE_FLAGS);
        _rootValueSeparator = DEFAULT_ROOT_VALUE_SEPARATOR;
        _characterEscapes = null;
        _maximumNonEscapedChar = 0; // disabled
        _quoteChar = DEFAULT_QUOTE_CHAR;
        _rootCharSymbols = CharsToNameCanonicalizer.createRoot(this);
    }

    /**
     * Copy constructor.
     *
     * @param src Original factory to copy configuration from
     */
    protected JsonFactory(JsonFactory src)
    {
        super(src);
        _rootValueSeparator = src._rootValueSeparator;
        _characterEscapes = src._characterEscapes;
        _maximumNonEscapedChar = src._maximumNonEscapedChar;
        _quoteChar = src._quoteChar;
        _rootCharSymbols = CharsToNameCanonicalizer.createRoot(this);
    }

    /**
     * Constructors used by {@link JsonFactoryBuilder} for instantiation.
     *
     * @param b Builder that has configuration to use
     *
     * @since 3.0
     */
    protected JsonFactory(JsonFactoryBuilder b)
    {
        super(b);
        _rootValueSeparator = b.rootValueSeparator();
        _characterEscapes = b.characterEscapes();
        _maximumNonEscapedChar = b.highestNonEscapedChar();
        _quoteChar = b.quoteChar();
        _rootCharSymbols = CharsToNameCanonicalizer.createRoot(this);
    }

    @Override
    public JsonFactoryBuilder rebuild() {
        return new JsonFactoryBuilder(this);
    }

    /**
     * Main factory method to use for constructing {@link JsonFactory} instances with
     * different configuration.
     *
     * @return Builder instance to use
     */
    public static JsonFactoryBuilder builder() {
        return new JsonFactoryBuilder();
    }

    /**
     * Method for constructing a new {@link JsonFactory} that has
     * the same settings as this instance, but is otherwise
     * independent (i.e. nothing is actually shared, symbol tables
     * are separate).
     *
     * @return Copy of this factory instance
     */
    @Override
    public JsonFactory copy() {
        return new JsonFactory(this);
    }

    @Override
    public TokenStreamFactory snapshot() {
        return this;
    }

    /*
    /**********************************************************************
    /* Serializable overrides
    /**********************************************************************
     */

    /**
     * Method that we need to override to actually make restoration go
     * through constructors etc; called by JDK serialization system.
     *
     * @return A properly initialized copy of this factory instance
     */
    protected Object readResolve() {
        return new JsonFactory(this);
    }

    /*
    /**********************************************************************
    /* Capability introspection
    /**********************************************************************
     */

    /**
     * Accessor for getting version of the core package, given a parser instance.
     * Left for sub-classes to implement.
     *
     * @return Version of this generator (derived from version declared for
     *   {@code jackson-core} jar that contains the class
     */
    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public boolean canParseAsync() {
        // Jackson 2.9 and later do support async parsing for JSON
        return true;
    }

    /**
     * Checked whether specified parser feature is enabled.
     *
     * @param f Feature to check
     *
     * @return {@code True} if feature is enabled; {@code false} otherwise
     */
    public final boolean isEnabled(JsonReadFeature f) {
        return (_formatReadFeatures & f.getMask()) != 0;
    }

    /**
     * Check whether specified generator feature is enabled.
     *
     * @param f Feature to check
     *
     * @return {@code True} if feature is enabled; {@code false} otherwise
     */
    public final boolean isEnabled(JsonWriteFeature f) {
        return (_formatWriteFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************************
    /* Format support
    /**********************************************************************
     */

    /**
     * Method that can be used to quickly check whether given schema
     * is something that parsers and/or generators constructed by this
     * factory could use. Note that this means possible use, at the level
     * of data format (i.e. schema is for same data format as parsers and
     * generators this factory constructs); individual schema instances
     * may have further usage restrictions.
     */
    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return false; // no FormatSchema for json
    }

    /**
     * Method that returns short textual id identifying format
     * this factory supports.
     *<p>
     * Note: sub-classes should override this method; default
     * implementation will return null for all sub-classes
     */
    @Override
    public String getFormatName() {
        return FORMAT_NAME_JSON;
    }

    @Override
    public Class<? extends FormatFeature> getFormatReadFeatureType() { return JsonReadFeature.class; }

    @Override
    public Class<? extends FormatFeature> getFormatWriteFeatureType() { return JsonWriteFeature.class; }

    /*
    /**********************************************************************
    /* Configuration accessors
    /**********************************************************************
     */

    /**
     * Method for accessing custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     *
     * @return CharacterEscapes configured to be used by parser instances
     */
    public CharacterEscapes getCharacterEscapes() { return _characterEscapes; }

    public String getRootValueSeparator() {
        return (_rootValueSeparator == null) ? null : _rootValueSeparator.getValue();
    }

    /*
    /**********************************************************************
    /* Parser factories, non-blocking (async) sources
    /**********************************************************************
     */

    @Override
    public JsonParser createNonBlockingByteArrayParser(ObjectReadContext readCtxt)
    {
        IOContext ioCtxt = _createNonBlockingContext(null);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new NonBlockingByteArrayJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                can);
    }

    @Override
    public JsonParser createNonBlockingByteBufferParser(ObjectReadContext readCtxt)
    {
        IOContext ioCtxt = _createNonBlockingContext(null);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new NonBlockingByteBufferJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                can);
    }

    protected IOContext _createNonBlockingContext(Object srcRef) {
        return new IOContext(_streamReadConstraints, _streamWriteConstraints,
                _errorReportConfiguration,
                _getBufferRecycler(),
                ContentReference.rawReference(srcRef), false, JsonEncoding.UTF8);
    }

    /*
    /**********************************************************************
    /* Factory methods used by factory for creating parser instances
    /**********************************************************************
     */

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            InputStream in) throws JacksonException
    {
        try {
            return new ByteSourceJsonBootstrapper(ioCtxt, in)
                    .constructParser(readCtxt,
                        readCtxt.getStreamReadFeatures(_streamReadFeatures),
                        readCtxt.getFormatReadFeatures(_formatReadFeatures),
                        _byteSymbolCanonicalizer, _rootCharSymbols, _factoryFeatures);
        } catch (RuntimeException e) {
            // 10-Jun-2022, tatu: For [core#763] may need to close InputStream here
            if (ioCtxt.isResourceManaged()) {
                try {
                    in.close();
                } catch (Exception e2) {
                    e.addSuppressed(e2);
                }
            }
            throw e;
        }
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            Reader r) throws JacksonException
    {
        return new ReaderBasedJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                r,
                _rootCharSymbols.makeChild());
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            char[] data, int offset, int len,
            boolean recyclable) throws JacksonException
    {
        _checkRangeBoundsForCharArray(data, offset, len);
        return new ReaderBasedJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                null,
                _rootCharSymbols.makeChild(),
                data, offset, offset+len, recyclable);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            byte[] data, int offset, int len)
        throws JacksonException
    {
        _checkRangeBoundsForByteArray(data, offset, len);
        return new ByteSourceJsonBootstrapper(ioCtxt, data, offset, len)
                .constructParser(readCtxt,
                        readCtxt.getStreamReadFeatures(_streamReadFeatures),
                        readCtxt.getFormatReadFeatures(_formatReadFeatures),
                       _byteSymbolCanonicalizer, _rootCharSymbols, _factoryFeatures);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            DataInput input)
        throws JacksonException
    {
        // Also: while we can't do full bootstrapping (due to read-ahead limitations), should
        // at least handle possible UTF-8 BOM
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new UTF8DataInputJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                input, can, firstByte);
    }

    /*
    /**********************************************************************
    /* Factory methods used by factory for creating generator instances
    /**********************************************************************
     */

    @Override
    protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, Writer out)
        throws JacksonException
    {
        SerializableString rootSep = writeCtxt.getRootValueSeparator(_rootValueSeparator);
        // May get Character-Escape overrides from context; if not, use factory's own
        // (which default to `null`)
        CharacterEscapes charEsc = writeCtxt.getCharacterEscapes();
        if (charEsc == null) {
            charEsc = _characterEscapes;
        }
        // 14-Jan-2019, tatu: Should we make this configurable via databind layer?
        final int maxNonEscaped = _maximumNonEscapedChar;
        // NOTE: JSON generator does not use schema
        return new WriterBasedJsonGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                writeCtxt.getFormatWriteFeatures(_formatWriteFeatures),
                out,
                rootSep, writeCtxt.getPrettyPrinter(), charEsc, maxNonEscaped, _quoteChar);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, OutputStream out) throws JacksonException
    {
        SerializableString rootSep = writeCtxt.getRootValueSeparator(_rootValueSeparator);
        // May get Character-Escape overrides from context; if not, use factory's own
        // (which default to `null`)
        CharacterEscapes charEsc = writeCtxt.getCharacterEscapes();
        if (charEsc == null) {
            charEsc = _characterEscapes;
        }
        // 14-Jan-2019, tatu: Should we make this configurable via databind layer?
        final int maxNonEscaped = _maximumNonEscapedChar;
        // NOTE: JSON generator does not use schema

        return new UTF8JsonGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                writeCtxt.getFormatWriteFeatures(_formatWriteFeatures),
                out,
                rootSep, charEsc, writeCtxt.getPrettyPrinter(), maxNonEscaped, _quoteChar);
    }

    /*
    /**********************************************************************
    /* Other factory methods
    /**********************************************************************
     */

    @Override
    public PropertyNameMatcher constructNameMatcher(List<Named> matches, boolean alreadyInterned) {
        return BinaryNameMatcher.constructFrom(matches, alreadyInterned);
    }

    @Override
    public PropertyNameMatcher constructCINameMatcher(List<Named> matches, boolean alreadyInterned,
            Locale locale) {
        return BinaryNameMatcher.constructCaseInsensitive(locale, matches, alreadyInterned);
    }
}
