/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package com.fasterxml.jackson.core;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;

import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.*;
import com.fasterxml.jackson.core.json.*;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * The main factory class of Jackson package, used to configure and
 * construct reader (aka parser, {@link JsonParser})
 * and writer (aka generator, {@link JsonGenerator})
 * instances.
 *<p>
 * Factory instances are thread-safe and reusable after configuration
 * (if any). Typically applications and services use only a single
 * globally shared factory instance, unless they need differently
 * configured factories. Factory reuse is important if efficiency matters;
 * most recycling of expensive construct is done on per-factory basis.
 *<p>
 * Creation of a factory instance is a light-weight operation,
 * and since there is no need for pluggable alternative implementations
 * (as there is no "standard" JSON processor API to implement),
 * the default constructor is used for constructing factory
 * instances.
 *
 * @author Tatu Saloranta
 */
@SuppressWarnings("resource")
public class JsonFactory
    implements Versioned,
        java.io.Serializable // since 2.1 (for Android, mostly)
{
    private static final long serialVersionUID = 1; // since 2.6.0

    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    /**
     * Enumeration that defines all on/off features that can only be
     * changed for {@link JsonFactory}.
     */
    public enum Feature {
        
        // // // Symbol handling (interning etc)
        
        /**
         * Feature that determines whether JSON object field names are
         * to be canonicalized using {@link String#intern} or not:
         * if enabled, all field names will be intern()ed (and caller
         * can count on this being true for all such names); if disabled,
         * no intern()ing is done. There may still be basic
         * canonicalization (that is, same String will be used to represent
         * all identical object property names for a single document).
         *<p>
         * Note: this setting only has effect if
         * {@link #CANONICALIZE_FIELD_NAMES} is true -- otherwise no
         * canonicalization of any sort is done.
         *<p>
         * This setting is enabled by default.
         */
        INTERN_FIELD_NAMES(true),

        /**
         * Feature that determines whether JSON object field names are
         * to be canonicalized (details of how canonicalization is done
         * then further specified by
         * {@link #INTERN_FIELD_NAMES}).
         *<p>
         * This setting is enabled by default.
         */
        CANONICALIZE_FIELD_NAMES(true),

        /**
         * Feature that determines what happens if we encounter a case in symbol
         * handling where number of hash collisions exceeds a safety threshold
         * -- which almost certainly means a denial-of-service attack via generated
         * duplicate hash codes.
         * If feature is enabled, an {@link IllegalStateException} is
         * thrown to indicate the suspected denial-of-service attack; if disabled, processing continues but
         * canonicalization (and thereby <code>intern()</code>ing) is disabled) as protective
         * measure.
         *<p>
         * This setting is enabled by default.
         * 
         * @since 2.4
         */
        FAIL_ON_SYMBOL_HASH_OVERFLOW(true),

        /**
         * Feature that determines whether we will use {@link BufferRecycler} with
         * {@link ThreadLocal} and {@link SoftReference}, for efficient reuse of
         * underlying input/output buffers.
         * This usually makes sense on normal J2SE/J2EE server-side processing;
         * but may not make sense on platforms where {@link SoftReference} handling
         * is broken (like Android), or if there are retention issues due to
         * {@link ThreadLocal} (see
         * <a href="https://github.com/FasterXML/jackson-core/issues/189">Issue #189</a>
         * for a possible case)
         *
         * @since 2.6
         */
        USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING(true)
        
        ;

        /**
         * Whether feature is enabled or disabled by default.
         */
        private final boolean _defaultState;
        
        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults() {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) { flags |= f.getMask(); }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) { _defaultState = defaultState; }
        
        public boolean enabledByDefault() { return _defaultState; }
        public boolean enabledIn(int flags) { return (flags & getMask()) != 0; }
        public int getMask() { return (1 << ordinal()); }
    }

    /*
    /**********************************************************
    /* Constants
    /**********************************************************
     */
    
    /**
     * Name used to identify JSON format
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_JSON = "JSON";
    
    /**
     * Bitfield (set of flags) of all factory features that are enabled by default.
     */
    protected final static int DEFAULT_FACTORY_FEATURE_FLAGS = JsonFactory.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    protected final static int DEFAULT_PARSER_FEATURE_FLAGS = JsonParser.Feature.collectDefaults();
    
    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    protected final static int DEFAULT_GENERATOR_FEATURE_FLAGS = JsonGenerator.Feature.collectDefaults();

    private final static SerializableString DEFAULT_ROOT_VALUE_SEPARATOR = DefaultPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR;
    
    /*
    /**********************************************************
    /* Buffer, symbol table management
    /**********************************************************
     */

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final protected static ThreadLocal<SoftReference<BufferRecycler>> _recyclerRef
        = new ThreadLocal<SoftReference<BufferRecycler>>();

    /**
     * Each factory comes equipped with a shared root symbol table.
     * It should not be linked back to the original blueprint, to
     * avoid contents from leaking between factories.
     */
    protected final transient CharsToNameCanonicalizer _rootCharSymbols = CharsToNameCanonicalizer.createRoot();

    /**
     * Alternative to the basic symbol table, some stream-based
     * parsers use different name canonicalization method.
     *<p>
     * TODO: should clean up this; looks messy having 2 alternatives
     * with not very clear differences.
     * 
     * @since 2.6.0
     */
    protected final transient ByteQuadsCanonicalizer _byteSymbolCanonicalizer = ByteQuadsCanonicalizer.createRoot();

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Object that implements conversion functionality between
     * Java objects and JSON content. For base JsonFactory implementation
     * usually not set by default, but can be explicitly set.
     * Sub-classes (like @link org.codehaus.jackson.map.MappingJsonFactory}
     * usually provide an implementation.
     */
    protected ObjectCodec _objectCodec;

    /**
     * Currently enabled factory features.
     */
    protected int _factoryFeatures = DEFAULT_FACTORY_FEATURE_FLAGS;
    
    /**
     * Currently enabled parser features.
     */
    protected int _parserFeatures = DEFAULT_PARSER_FEATURE_FLAGS;

    /**
     * Currently enabled generator features.
     */
    protected int _generatorFeatures = DEFAULT_GENERATOR_FEATURE_FLAGS;

    /**
     * Definition of custom character escapes to use for generators created
     * by this factory, if any. If null, standard data format specific
     * escapes are used.
     */
    protected CharacterEscapes _characterEscapes;

    /**
     * Optional helper object that may decorate input sources, to do
     * additional processing on input during parsing.
     */
    protected InputDecorator _inputDecorator;

    /**
     * Optional helper object that may decorate output object, to do
     * additional processing on output during content generation.
     */
    protected OutputDecorator _outputDecorator;

    /**
     * Separator used between root-level values, if any; null indicates
     * "do not add separator".
     * Default separator is a single space character.
     * 
     * @since 2.1
     */
    protected SerializableString _rootValueSeparator = DEFAULT_ROOT_VALUE_SEPARATOR;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
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
    public JsonFactory() { this(null); }

    public JsonFactory(ObjectCodec oc) { _objectCodec = oc; }

    /**
     * Constructor used when copy()ing a factory instance.
     * 
     * @since 2.2.1
     */
    protected JsonFactory(JsonFactory src, ObjectCodec codec)
    {
        _objectCodec = null;
        _factoryFeatures = src._factoryFeatures;
        _parserFeatures = src._parserFeatures;
        _generatorFeatures = src._generatorFeatures;
        _characterEscapes = src._characterEscapes;
        _inputDecorator = src._inputDecorator;
        _outputDecorator = src._outputDecorator;
        _rootValueSeparator = src._rootValueSeparator;
        
        /* 27-Apr-2013, tatu: How about symbol table; should we try to
         *   reuse shared symbol tables? Could be more efficient that way;
         *   although can slightly add to concurrency overhead.
         */
    }
    
    /**
     * Method for constructing a new {@link JsonFactory} that has
     * the same settings as this instance, but is otherwise
     * independent (i.e. nothing is actually shared, symbol tables
     * are separate).
     * Note that {@link ObjectCodec} reference is not copied but is
     * set to null; caller typically needs to set it after calling
     * this method. Reason for this is that the codec is used for
     * callbacks, and assumption is that there is strict 1-to-1
     * mapping between codec, factory. Caller has to, then, explicitly
     * set codec after making the copy.
     * 
     * @since 2.1
     */
    public JsonFactory copy()
    {
        _checkInvalidCopy(JsonFactory.class);
        // as per above, do clear ObjectCodec
        return new JsonFactory(this, null);
    }
    
    /**
     * @since 2.1
     * @param exp
     */
    protected void _checkInvalidCopy(Class<?> exp)
    {
        if (getClass() != exp) {
            throw new IllegalStateException("Failed copy(): "+getClass().getName()
                    +" (version: "+version()+") does not override copy(); it has to");
        }
    }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Method that we need to override to actually make restoration go
     * through constructors etc.
     * Also: must be overridden by sub-classes as well.
     */
    protected Object readResolve() {
        return new JsonFactory(this, _objectCodec);
    }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */
    
    /**
     * Introspection method that higher-level functionality may call
     * to see whether underlying data format requires a stable ordering
     * of object properties or not.
     * This is usually used for determining
     * whether to force a stable ordering (like alphabetic ordering by name)
     * if no ordering if explicitly specified.
     *<p>
     * Default implementation returns <code>false</code> as JSON does NOT
     * require stable ordering. Formats that require ordering include positional
     * textual formats like <code>CSV</code>, and schema-based binary formats
     * like <code>Avro</code>.
     * 
     * @since 2.3
     */
    public boolean requiresPropertyOrdering() { return false; }

    /**
     * Introspection method that higher-level functionality may call
     * to see whether underlying data format can read and write binary
     * data natively; that is, embeded it as-is without using encodings
     * such as Base64.
     *<p>
     * Default implementation returns <code>false</code> as JSON does not
     * support native access: all binary content must use Base64 encoding.
     * Most binary formats (like Smile and Avro) support native binary content.
     * 
     * @since 2.3
     */
    public boolean canHandleBinaryNatively() { return false; }

    /**
     * Introspection method that can be used by base factory to check
     * whether access using <code>char[]</code> is something that actual
     * parser implementations can take advantage of, over having to
     * use {@link java.io.Reader}. Sub-types are expected to override
     * definition; default implementation (suitable for JSON) alleges
     * that optimization are possible; and thereby is likely to try
     * to access {@link java.lang.String} content by first copying it into
     * recyclable intermediate buffer.
     * 
     * @since 2.4
     */
    public boolean canUseCharArrays() { return true; }

    /**
     * Method for accessing kind of {@link FormatFeature} that a parser
     * {@link JsonParser} produced by this factory would accept, if any;
     * <code>null</code> returned if none.
     *
     * @since 2.6
     */
    public Class<? extends FormatFeature> getFormatReadFeatureType() {
        return null;
    }

    /**
     * Method for accessing kind of {@link FormatFeature} that a parser
     * {@link JsonGenerator} produced by this factory would accept, if any;
     * <code>null</code> returned if none.
     *
     * @since 2.6
     */
    public Class<? extends FormatFeature> getFormatWriteFeatureType() {
        return null;
    }
    /*
    /**********************************************************
    /* Format detection functionality
    /**********************************************************
     */

    /**
     * Method that can be used to quickly check whether given schema
     * is something that parsers and/or generators constructed by this
     * factory could use. Note that this means possible use, at the level
     * of data format (i.e. schema is for same data format as parsers and
     * generators this factory constructs); individual schema instances
     * may have further usage restrictions.
     * 
     * @since 2.1
     */
    public boolean canUseSchema(FormatSchema schema) {
        if (schema == null){
            return false;
        }
        String ourFormat = getFormatName();
        return (ourFormat != null) && ourFormat.equals(schema.getSchemaType());
    }

    /**
     * Method that returns short textual id identifying format
     * this factory supports.
     *<p>
     * Note: sub-classes should override this method; default
     * implementation will return null for all sub-classes
     */
    public String getFormatName()
    {
        /* Somewhat nasty check: since we can't make this abstract
         * (due to backwards compatibility concerns), need to prevent
         * format name "leakage"
         */
        if (getClass() == JsonFactory.class) {
            return FORMAT_NAME_JSON;
        }
        return null;
    }

    /**
     * Convenience method for trying to determine whether input via given accessor
     * is of format type supported by this factory.
     */
    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        // since we can't keep this abstract, only implement for "vanilla" instance
        if (getClass() == JsonFactory.class) {
            return hasJSONFormat(acc);
        }
        return null;
    }

    /**
     * Method that can be called to determine if a custom
     * {@link ObjectCodec} is needed for binding data parsed
     * using {@link JsonParser} constructed by this factory
     * (which typically also implies the same for serialization
     * with {@link JsonGenerator}).
     * 
     * @return True if custom codec is needed with parsers and
     *   generators created by this factory; false if a general
     *   {@link ObjectCodec} is enough
     * 
     * @since 2.1
     */
    public boolean requiresCustomCodec() {
        return false;
    }
    
    /**
     * Helper method that can be called to determine if content accessed
     * using given accessor seems to be JSON content.
     */
    protected MatchStrength hasJSONFormat(InputAccessor acc) throws IOException
    {
        return ByteSourceJsonBootstrapper.hasJSONFormat(acc);
    }

    /*
    /**********************************************************
    /* Versioned
    /**********************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Configuration, factory features
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link JsonParser.Feature} for list of features)
     */
    public final JsonFactory configure(JsonFactory.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    /**
     * Method for enabling specified parser feature
     * (check {@link JsonFactory.Feature} for list of features)
     */
    public JsonFactory enable(JsonFactory.Feature f) {
        _factoryFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link JsonFactory.Feature} for list of features)
     */
    public JsonFactory disable(JsonFactory.Feature f) {
        _factoryFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(JsonFactory.Feature f) {
        return (_factoryFeatures & f.getMask()) != 0;
    }
    
    /*
    /**********************************************************
    /* Configuration, parser configuration
    /**********************************************************
     */
    
    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link JsonParser.Feature} for list of features)
     */
    public final JsonFactory configure(JsonParser.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    /**
     * Method for enabling specified parser feature
     * (check {@link JsonParser.Feature} for list of features)
     */
    public JsonFactory enable(JsonParser.Feature f) {
        _parserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link JsonParser.Feature} for list of features)
     */
    public JsonFactory disable(JsonParser.Feature f) {
        _parserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(JsonParser.Feature f) {
        return (_parserFeatures & f.getMask()) != 0;
    }

    /**
     * Method for getting currently configured input decorator (if any;
     * there is no default decorator).
     */
    public InputDecorator getInputDecorator() {
        return _inputDecorator;
    }

    /**
     * Method for overriding currently configured input decorator
     */
    public JsonFactory setInputDecorator(InputDecorator d) {
        _inputDecorator = d;
        return this;
    }
    
    /*
    /**********************************************************
    /* Configuration, generator settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link JsonGenerator.Feature} for list of features)
     */
    public final JsonFactory configure(JsonGenerator.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }


    /**
     * Method for enabling specified generator features
     * (check {@link JsonGenerator.Feature} for list of features)
     */
    public JsonFactory enable(JsonGenerator.Feature f) {
        _generatorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link JsonGenerator.Feature} for list of features)
     */
    public JsonFactory disable(JsonGenerator.Feature f) {
        _generatorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(JsonGenerator.Feature f) {
        return (_generatorFeatures & f.getMask()) != 0;
    }

    /**
     * Method for accessing custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     */
    public CharacterEscapes getCharacterEscapes() { return _characterEscapes; }

    /**
     * Method for defining custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     */
    public JsonFactory setCharacterEscapes(CharacterEscapes esc) {
        _characterEscapes = esc;
        return this;
    }

    /**
     * Method for getting currently configured output decorator (if any;
     * there is no default decorator).
     */
    public OutputDecorator getOutputDecorator() {
        return _outputDecorator;
    }

    /**
     * Method for overriding currently configured output decorator
     */
    public JsonFactory setOutputDecorator(OutputDecorator d) {
        _outputDecorator = d;
        return this;
    }

    /**
     * Method that allows overriding String used for separating root-level
     * JSON values (default is single space character)
     * 
     * @param sep Separator to use, if any; null means that no separator is
     *   automatically added
     * 
     * @since 2.1
     */
    public JsonFactory setRootValueSeparator(String sep) {
        _rootValueSeparator = (sep == null) ? null : new SerializedString(sep);
        return this;
    }

    /**
     * @since 2.1
     */
    public String getRootValueSeparator() {
        return (_rootValueSeparator == null) ? null : _rootValueSeparator.getValue();
    }
    
    /*
    /**********************************************************
    /* Configuration, other
    /**********************************************************
     */

    /**
     * Method for associating a {@link ObjectCodec} (typically
     * a <code>com.fasterxml.jackson.databind.ObjectMapper</code>)
     * with this factory (and more importantly, parsers and generators
     * it constructs). This is needed to use data-binding methods
     * of {@link JsonParser} and {@link JsonGenerator} instances.
     */
    public JsonFactory setCodec(ObjectCodec oc) {
        _objectCodec = oc;
        return this;
    }

    public ObjectCodec getCodec() { return _objectCodec; }

    /*
    /**********************************************************
    /* Parser factories (new ones, as per [Issue-25])
    /**********************************************************
     */

    /**
     * Method for constructing JSON parser instance to parse
     * contents of specified file.
     *
     *<p>
     * Encoding is auto-detected from contents according to JSON
     * specification recommended mechanism. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param f File that contains JSON content to parse
     * 
     * @since 2.1
     */
    public JsonParser createParser(File f) throws IOException, JsonParseException {
        // true, since we create InputStream from File
        IOContext ctxt = _createContext(f, true);
        InputStream in = new FileInputStream(f);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    /**
     * Method for constructing JSON parser instance to parse
     * contents of resource reference by given URL.
     *
     *<p>
     * Encoding is auto-detected from contents according to JSON
     * specification recommended mechanism. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param url URL pointing to resource that contains JSON content to parse
     * 
     * @since 2.1
     */
    public JsonParser createParser(URL url) throws IOException, JsonParseException {
        // true, since we create InputStream from URL
        IOContext ctxt = _createContext(url, true);
        InputStream in = _optimizedStreamFromURL(url);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    /**
     * Method for constructing JSON parser instance to parse
     * the contents accessed via specified input stream.
     *<p>
     * The input stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link com.fasterxml.jackson.core.JsonParser.Feature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *<p>
     *
     * Note: no encoding argument is taken since it can always be
     * auto-detected as suggested by JSON RFC. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     * @param in InputStream to use for reading JSON content to parse
     * 
     * @since 2.1
     */
    public JsonParser createParser(InputStream in) throws IOException, JsonParseException {
        IOContext ctxt = _createContext(in, false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }
    
    /**
     * Method for constructing parser for parsing
     * the contents accessed via specified Reader.
     <p>
     * The read stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link com.fasterxml.jackson.core.JsonParser.Feature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *
     * @param r Reader to use for reading JSON content to parse
     * 
     * @since 2.1
     */
    public JsonParser createParser(Reader r) throws IOException, JsonParseException {
        // false -> we do NOT own Reader (did not create it)
        IOContext ctxt = _createContext(r, false);
        return _createParser(_decorate(r, ctxt), ctxt);
    }

    /**
     * Method for constructing parser for parsing
     * the contents of given byte array.
     * 
     * @since 2.1
     */
    public JsonParser createParser(byte[] data) throws IOException, JsonParseException {
        IOContext ctxt = _createContext(data, true);
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, 0, data.length);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, 0, data.length, ctxt);
    }

    /**
     * Method for constructing parser for parsing
     * the contents of given byte array.
     * 
     * @param data Buffer that contains data to parse
     * @param offset Offset of the first data byte within buffer
     * @param len Length of contents to parse within buffer
     * 
     * @since 2.1
     */
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException, JsonParseException {
        IOContext ctxt = _createContext(data, true);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, offset, len);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, offset, len, ctxt);
    }

    /**
     * Method for constructing parser for parsing
     * contents of given String.
     * 
     * @since 2.1
     */
    public JsonParser createParser(String content) throws IOException, JsonParseException {
        final int strLen = content.length();
        // Actually, let's use this for medium-sized content, up to 64kB chunk (32kb char)
        if ((_inputDecorator != null) || (strLen > 0x8000) || !canUseCharArrays()) {
            // easier to just wrap in a Reader than extend InputDecorator; or, if content
            // is too long for us to copy it over
            return createParser(new StringReader(content));
        }
        IOContext ctxt = _createContext(content, true);
        char[] buf = ctxt.allocTokenBuffer(strLen);
        content.getChars(0, strLen, buf, 0);
        return _createParser(buf, 0, strLen, ctxt, true);
    }

    /**
     * Method for constructing parser for parsing
     * contents of given char array.
     * 
     * @since 2.4
     */
    public JsonParser createParser(char[] content) throws IOException {
        return createParser(content, 0, content.length);
    }
    
    /**
     * Method for constructing parser for parsing contents of given char array.
     * 
     * @since 2.4
     */
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
        if (_inputDecorator != null) { // easier to just wrap in a Reader than extend InputDecorator
            return createParser(new CharArrayReader(content, offset, len));
        }
        return _createParser(content, offset, len, _createContext(content, true),
                // important: buffer is NOT recyclable, as it's from caller
                false);
    }

    /**
     * @since 2.8
     */
    public JsonParser createParser(DataInput in) throws IOException {
        IOContext ctxt = _createContext(in, false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    /*
    /**********************************************************
    /* Parser factories (old ones, pre-2.2)
    /**********************************************************
     */

    /**
     * Method for constructing JSON parser instance to parse
     * contents of specified file.
     *<p>
     * Encoding is auto-detected from contents according to JSON
     * specification recommended mechanism. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param f File that contains JSON content to parse
     * 
     * @deprecated Since 2.2, use {@link #createParser(File)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(File f) throws IOException, JsonParseException {
        return createParser(f);
    }

    /**
     * Method for constructing JSON parser instance to parse
     * contents of resource reference by given URL.
     *
     *<p>
     * Encoding is auto-detected from contents according to JSON
     * specification recommended mechanism. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param url URL pointing to resource that contains JSON content to parse
     * 
     * @deprecated Since 2.2, use {@link #createParser(URL)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(URL url) throws IOException, JsonParseException {
        return createParser(url);
    }

    /**
     * Method for constructing JSON parser instance to parse
     * the contents accessed via specified input stream.
     *<p>
     * The input stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link com.fasterxml.jackson.core.JsonParser.Feature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *<p>
     *
     * Note: no encoding argument is taken since it can always be
     * auto-detected as suggested by JSON RFC. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     * @param in InputStream to use for reading JSON content to parse
     * 
     * @deprecated Since 2.2, use {@link #createParser(InputStream)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(InputStream in) throws IOException, JsonParseException {
        return createParser(in);
    }

    /**
     * Method for constructing parser for parsing
     * the contents accessed via specified Reader.
     <p>
     * The read stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link com.fasterxml.jackson.core.JsonParser.Feature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *
     * @param r Reader to use for reading JSON content to parse
     * 
     * @deprecated Since 2.2, use {@link #createParser(Reader)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(Reader r) throws IOException, JsonParseException {
        return createParser(r);
    }

    /**
     * Method for constructing parser for parsing the contents of given byte array.
     * 
     * @deprecated Since 2.2, use {@link #createParser(byte[])} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(byte[] data) throws IOException, JsonParseException {
        return createParser(data);
    }

    /**
     * Method for constructing parser for parsing
     * the contents of given byte array.
     * 
     * @param data Buffer that contains data to parse
     * @param offset Offset of the first data byte within buffer
     * @param len Length of contents to parse within buffer
     * 
     * @deprecated Since 2.2, use {@link #createParser(byte[],int,int)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(byte[] data, int offset, int len) throws IOException, JsonParseException {
        return createParser(data, offset, len);
    }

    /**
     * Method for constructing parser for parsing
     * contents of given String.
     * 
     * @deprecated Since 2.2, use {@link #createParser(String)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(String content) throws IOException, JsonParseException {
        return createParser(content);
    }

    /*
    /**********************************************************
    /* Generator factories, new (as per [Issue-25]
    /**********************************************************
     */

    /**
     * Method for constructing JSON generator for writing JSON content
     * using specified output stream.
     * Encoding to use must be specified, and needs to be one of available
     * types (as per JSON specification).
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the output stream when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link com.fasterxml.jackson.core.JsonGenerator.Feature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats)
     * and that ignore passed in encoding.
     *
     * @param out OutputStream to use for writing JSON content 
     * @param enc Character encoding to use
     * 
     * @since 2.1
     */
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        ctxt.setEncoding(enc);
        if (enc == JsonEncoding.UTF8) {
            return _createUTF8Generator(_decorate(out, ctxt), ctxt);
        }
        Writer w = _createWriter(out, enc, ctxt);
        return _createGenerator(_decorate(w, ctxt), ctxt);
    }

    /**
     * Convenience method for constructing generator that uses default
     * encoding of the format (UTF-8 for JSON and most other data formats).
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats).
     * 
     * @since 2.1
     */
    public JsonGenerator createGenerator(OutputStream out) throws IOException {
        return createGenerator(out, JsonEncoding.UTF8);
    }
    
    /**
     * Method for constructing JSON generator for writing JSON content
     * using specified Writer.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link com.fasterxml.jackson.core.JsonGenerator.Feature#AUTO_CLOSE_TARGET} is enabled).
     * Using application needs to close it explicitly.
     * 
     * @since 2.1
     *
     * @param w Writer to use for writing JSON content 
     */
    public JsonGenerator createGenerator(Writer w) throws IOException {
        IOContext ctxt = _createContext(w, false);
        return _createGenerator(_decorate(w, ctxt), ctxt);
    }
    
    /**
     * Method for constructing JSON generator for writing JSON content
     * to specified file, overwriting contents it might have (or creating
     * it if such file does not yet exist).
     * Encoding to use must be specified, and needs to be one of available
     * types (as per JSON specification).
     *<p>
     * Underlying stream <b>is owned</b> by the generator constructed,
     * i.e. generator will handle closing of file when
     * {@link JsonGenerator#close} is called.
     *
     * @param f File to write contents to
     * @param enc Character encoding to use
     * 
     * @since 2.1
     */
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ctxt = _createContext(out, true);
        ctxt.setEncoding(enc);
        if (enc == JsonEncoding.UTF8) {
            return _createUTF8Generator(_decorate(out, ctxt), ctxt);
        }
        Writer w = _createWriter(out, enc, ctxt);
        return _createGenerator(_decorate(w, ctxt), ctxt);
    }    

    /**
     * Method for constructing generator for writing content using specified
     * {@link DataOutput} instance.
     * 
     * @since 2.8
     */
    public JsonGenerator createGenerator(DataOutput out, JsonEncoding enc) throws IOException {
        return createGenerator(_createDataOutputWrapper(out), enc);
    }

    /**
     * Convenience method for constructing generator that uses default
     * encoding of the format (UTF-8 for JSON and most other data formats).
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats).
     * 
     * @since 2.8
     */
    public JsonGenerator createGenerator(DataOutput out) throws IOException {
        return createGenerator(_createDataOutputWrapper(out), JsonEncoding.UTF8);
    }

    /*
    /**********************************************************
    /* Generator factories, old (pre-2.2)
    /**********************************************************
     */

    /**
     * Method for constructing JSON generator for writing JSON content
     * using specified output stream.
     * Encoding to use must be specified, and needs to be one of available
     * types (as per JSON specification).
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the output stream when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link com.fasterxml.jackson.core.JsonGenerator.Feature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats)
     * and that ignore passed in encoding.
     *
     * @param out OutputStream to use for writing JSON content 
     * @param enc Character encoding to use
     *
     * @deprecated Since 2.2, use {@link #createGenerator(OutputStream, JsonEncoding)} instead.
     */
    @Deprecated
    public JsonGenerator createJsonGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        return createGenerator(out, enc);
    }

    /**
     * Method for constructing JSON generator for writing JSON content
     * using specified Writer.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link com.fasterxml.jackson.core.JsonGenerator.Feature#AUTO_CLOSE_TARGET} is enabled).
     * Using application needs to close it explicitly.
     *
     * @param out Writer to use for writing JSON content 
     * 
     * @deprecated Since 2.2, use {@link #createGenerator(Writer)} instead.
     */
    @Deprecated
    public JsonGenerator createJsonGenerator(Writer out) throws IOException {
        return createGenerator(out);
    }

    /**
     * Convenience method for constructing generator that uses default
     * encoding of the format (UTF-8 for JSON and most other data formats).
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats).
     * 
     * @deprecated Since 2.2, use {@link #createGenerator(OutputStream)} instead.
     */
    @Deprecated
    public JsonGenerator createJsonGenerator(OutputStream out) throws IOException {
        return createGenerator(out, JsonEncoding.UTF8);
    }

    /*
    /**********************************************************
    /* Factory methods used by factory for creating parser instances,
    /* overridable by sub-classes
    /**********************************************************
     */

    /**
     * Overridable factory method that actually instantiates desired parser
     * given {@link InputStream} and context object.
     *<p>
     * This method is specifically designed to remain
     * compatible between minor versions so that sub-classes can count
     * on it being called as expected. That is, it is part of official
     * interface from sub-class perspective, although not a public
     * method available to users of factory implementations.
     * 
     * @since 2.1
     */
    protected JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        // As per [JACKSON-259], may want to fully disable canonicalization:
        return new ByteSourceJsonBootstrapper(ctxt, in).constructParser(_parserFeatures,
                _objectCodec, _byteSymbolCanonicalizer, _rootCharSymbols, _factoryFeatures);
    }

    /**
     * Overridable factory method that actually instantiates parser
     * using given {@link Reader} object for reading content.
     *<p>
     * This method is specifically designed to remain
     * compatible between minor versions so that sub-classes can count
     * on it being called as expected. That is, it is part of official
     * interface from sub-class perspective, although not a public
     * method available to users of factory implementations.
     * 
     * @since 2.1
     */
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return new ReaderBasedJsonParser(ctxt, _parserFeatures, r, _objectCodec,
                _rootCharSymbols.makeChild(_factoryFeatures));
    }

    /**
     * Overridable factory method that actually instantiates parser
     * using given <code>char[]</code> object for accessing content.
     * 
     * @since 2.4
     */
    protected JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt,
            boolean recyclable) throws IOException {
        return new ReaderBasedJsonParser(ctxt, _parserFeatures, null, _objectCodec,
                _rootCharSymbols.makeChild(_factoryFeatures),
                        data, offset, offset+len, recyclable);
    }

    /**
     * Overridable factory method that actually instantiates parser
     * using given {@link Reader} object for reading content
     * passed as raw byte array.
     *<p>
     * This method is specifically designed to remain
     * compatible between minor versions so that sub-classes can count
     * on it being called as expected. That is, it is part of official
     * interface from sub-class perspective, although not a public
     * method available to users of factory implementations.
     */
    protected JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException
    {
        return new ByteSourceJsonBootstrapper(ctxt, data, offset, len).constructParser(_parserFeatures,
                _objectCodec, _byteSymbolCanonicalizer, _rootCharSymbols, _factoryFeatures);
    }

    /**
     * @since 2.8
     */
    protected JsonParser _createParser(DataInput input, IOContext ctxt) throws IOException
    {
        // 13-May-2016, tatu: Need to take care not to accidentally create JSON parser for
        //   non-JSON input. So, bit unclean but...
        String format = getFormatName();
        if (format != FORMAT_NAME_JSON) { // NOTE: only ensure override; full equality NOT needed
            throw new UnsupportedOperationException(String.format(
                    "InputData source not (yet?) support for this format (%s)", format));
        }
        // Also: while we can't do full bootstrapping (due to read-ahead limitations), should
        // at least handle possible UTF-8 BOM
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChild(_factoryFeatures);
        return new UTF8DataInputJsonParser(ctxt, _parserFeatures, input,
                _objectCodec, can, firstByte);
    }

    /*
    /**********************************************************
    /* Factory methods used by factory for creating generator instances,
    /* overridable by sub-classes
    /**********************************************************
     */
    
    /**
     * Overridable factory method that actually instantiates generator for
     * given {@link Writer} and context object.
     *<p>
     * This method is specifically designed to remain
     * compatible between minor versions so that sub-classes can count
     * on it being called as expected. That is, it is part of official
     * interface from sub-class perspective, although not a public
     * method available to users of factory implementations.
     */
    protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException
    {
        WriterBasedJsonGenerator gen = new WriterBasedJsonGenerator(ctxt,
                _generatorFeatures, _objectCodec, out);
        if (_characterEscapes != null) {
            gen.setCharacterEscapes(_characterEscapes);
        }
        SerializableString rootSep = _rootValueSeparator;
        if (rootSep != DEFAULT_ROOT_VALUE_SEPARATOR) {
            gen.setRootValueSeparator(rootSep);
        }
        return gen;
    }

    /**
     * Overridable factory method that actually instantiates generator for
     * given {@link OutputStream} and context object, using UTF-8 encoding.
     *<p>
     * This method is specifically designed to remain
     * compatible between minor versions so that sub-classes can count
     * on it being called as expected. That is, it is part of official
     * interface from sub-class perspective, although not a public
     * method available to users of factory implementations.
     */
    protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        UTF8JsonGenerator gen = new UTF8JsonGenerator(ctxt,
                _generatorFeatures, _objectCodec, out);
        if (_characterEscapes != null) {
            gen.setCharacterEscapes(_characterEscapes);
        }
        SerializableString rootSep = _rootValueSeparator;
        if (rootSep != DEFAULT_ROOT_VALUE_SEPARATOR) {
            gen.setRootValueSeparator(rootSep);
        }
        return gen;
    }

    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        // note: this should not get called any more (caller checks, dispatches)
        if (enc == JsonEncoding.UTF8) { // We have optimized writer for UTF-8
            return new UTF8Writer(ctxt, out);
        }
        // not optimal, but should do unless we really care about UTF-16/32 encoding speed
        return new OutputStreamWriter(out, enc.getJavaName());
    }

    /*
    /**********************************************************
    /* Internal factory methods, decorator handling
    /**********************************************************
     */

    /**
     * @since 2.4
     */
    protected final InputStream _decorate(InputStream in, IOContext ctxt) throws IOException {
        if (_inputDecorator != null) {
            InputStream in2 = _inputDecorator.decorate(ctxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    /**
     * @since 2.4
     */
    protected final Reader _decorate(Reader in, IOContext ctxt) throws IOException {
        if (_inputDecorator != null) {
            Reader in2 = _inputDecorator.decorate(ctxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    /**
     * @since 2.8
     */
    protected final DataInput _decorate(DataInput in, IOContext ctxt) throws IOException {
        if (_inputDecorator != null) {
            DataInput in2 = _inputDecorator.decorate(ctxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }
    
    /**
     * @since 2.4
     */
    protected final OutputStream _decorate(OutputStream out, IOContext ctxt) throws IOException {
        if (_outputDecorator != null) {
            OutputStream out2 = _outputDecorator.decorate(ctxt, out);
            if (out2 != null) {
                return out2;
            }
        }
        return out;
    }

    /**
     * @since 2.4
     */
    protected final Writer _decorate(Writer out, IOContext ctxt) throws IOException {
        if (_outputDecorator != null) {
            Writer out2 = _outputDecorator.decorate(ctxt, out);
            if (out2 != null) {
                return out2;
            }
        }
        return out;
    }
    
    /*
    /**********************************************************
    /* Internal factory methods, other
    /**********************************************************
     */

    /**
     * Method used by factory to create buffer recycler instances
     * for parsers and generators.
     *<p>
     * Note: only public to give access for <code>ObjectMapper</code>
     */
    public BufferRecycler _getBufferRecycler()
    {
        /* 23-Apr-2015, tatu: Let's allow disabling of buffer recycling
         *   scheme, for cases where it is considered harmful (possibly
         *   on Android, for example)
         */
        if (isEnabled(Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING)) {
            SoftReference<BufferRecycler> ref = _recyclerRef.get();
            BufferRecycler br = (ref == null) ? null : ref.get();
    
            if (br == null) {
                br = new BufferRecycler();
                _recyclerRef.set(new SoftReference<BufferRecycler>(br));
            }
            return br;
        }
        return new BufferRecycler();
    }

    /**
     * Overridable factory method that actually instantiates desired
     * context object.
     */
    protected IOContext _createContext(Object srcRef, boolean resourceManaged) {
        return new IOContext(_getBufferRecycler(), srcRef, resourceManaged);
    }

    /**
     * @since 2.8
     */
    protected OutputStream _createDataOutputWrapper(DataOutput out) {
        return new DataOutputAsStream(out);
    }

    /**
     * Helper methods used for constructing an optimal stream for
     * parsers to use, when input is to be read from an URL.
     * This helps when reading file content via URL.
     */
    protected InputStream _optimizedStreamFromURL(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            /* Can not do this if the path refers
             * to a network drive on windows. This fixes the problem;
             * might not be needed on all platforms (NFS?), but should not
             * matter a lot: performance penalty of extra wrapping is more
             * relevant when accessing local file system.
             */
            String host = url.getHost();
            if (host == null || host.length() == 0) {
                // [core#48]: Let's try to avoid probs with URL encoded stuff
                String path = url.getPath();
                if (path.indexOf('%') < 0) {
                    return new FileInputStream(url.getPath());

                }
                // otherwise, let's fall through and let URL decoder do its magic
            }
        }
        return url.openStream();
    }
}
