/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package com.fasterxml.jackson.core;

import java.io.*;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.io.*;
import com.fasterxml.jackson.core.json.*;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
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
        java.io.Serializable
{
    private static final long serialVersionUID = 1;

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
         *<p>
         * This setting is enabled by default.
         */
        USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING(true),

        /**
         * Feature that determines whether we register {@link SoftReference}-d {@link BufferRecycler}s in
         *  {@link ThreadLocal}s, to be able to release them with a shutDown method.
         *  Note that this feature works on class level and can only be set once, that is,
         *  once enabled by one JsonFactory instance, it will always be enabled for all instances.
         *<p>
         * This setting is disabled by default.
         */
        USE_RELEASABLE_THREAD_LOCAL_BUFFERS(false)
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

    /*
     * For issue #400: We optionally keep references to all SoftReferences to BufferRecyclers which we put in all ThreadLocals
     * We do this to be able to release them (dereference) in a newly introduced shutdown method.
     * It can be enabled by Feature.USE_RELEASABLE_THREAD_LOCAL_BUFFERS on a JsonFactory instance or by
     * enableUseReleasableThreadLocalBuffers on JsonFactory class.
     */
    private static volatile ThreadLocalBufferManager _bufferMgr = null;

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final protected static ThreadLocal<SoftReference<BufferRecycler>> _recyclerRef = new ThreadLocal<SoftReference<BufferRecycler>>();

    /**
     * Each factory comes equipped with a shared root symbol table.
     * It should not be linked back to the original blueprint, to
     * avoid contents from leaking between factories.
     */
    protected final transient CharsToNameCanonicalizer _rootCharSymbols = CharsToNameCanonicalizer.createRoot();

    /**
     * Alternative to the basic symbol table, some stream-based
     * parsers use different name canonicalization method.
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
     */
    public JsonFactory copy()
    {
        _checkInvalidCopy(JsonFactory.class);
        // as per above, do clear ObjectCodec
        return new JsonFactory(this, null);
    }

    protected void _checkInvalidCopy(Class<?> exp)
    {
        if (getClass() != exp) {
            throw new IllegalStateException("Failed copy(): "+getClass().getName()
                    +" (version: "+version()+") does not override copy(); it has to");
        }
    }

    /*
    /**********************************************************
    /* Shutdown, releasing resources
    /**********************************************************
    */

    /**
     * Releases the buffers retained in ThreadLocals. To be called on shutdown event of applications which make use of
     * an environment like an appserver which stays alive and uses a thread pool that causes ThreadLocals created by the
     * application to survive much longer than the application itself.
     * Note: {@link JsonFactory.Feature}.USE_RELEASABLE_THREAD_LOCAL_BUFFERS needs to be enabled or
     * enableUseReleasableThreadLocalBuffers needs to have be called on startup
     */
    public static void shutdown() {
        System.out.println("JsonFactory shutdown called"); // for debugging, @TODO remove after test
        if (_bufferMgr != null) {
            _bufferMgr.shutdown();
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
     */
    public boolean canUseCharArrays() { return true; }

    /**
     * Introspection method that can be used to check whether this
     * factory can create non-blocking parsers: parsers that do not
     * use blocking I/O abstractions but instead use a
     * {@link com.fasterxml.jackson.core.async.NonBlockingInputFeeder}.
     */
    public boolean canParseAsync() {
        // 31-May-2017, tatu: Jackson 2.9 does support async parsing for JSON,
        //   but not all other formats, so need to do this:
        return _isJSONFactory();
    }

    /**
     * Method for accessing kind of {@link FormatFeature} that a parser
     * {@link JsonParser} produced by this factory would accept, if any;
     * <code>null</code> returned if none.
     */
    public Class<? extends FormatFeature> getFormatReadFeatureType() {
        return null;
    }

    /**
     * Method for accessing kind of {@link FormatFeature} that a parser
     * {@link JsonGenerator} produced by this factory would accept, if any;
     * <code>null</code> returned if none.
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
     * Method that can be called to determine if a custom
     * {@link ObjectCodec} is needed for binding data parsed
     * using {@link JsonParser} constructed by this factory
     * (which typically also implies the same for serialization
     * with {@link JsonGenerator}).
     * 
     * @return True if custom codec is needed with parsers and
     *   generators created by this factory; false if a general
     *   {@link ObjectCodec} is enough
     */
    public boolean requiresCustomCodec() {
        return false;
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
     * Method for enabling or disabling specified factory feature
     * (check {@link JsonFactory.Feature} for list of features)
     */
    public final JsonFactory configure(JsonFactory.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    /**
     * Method for enabling specified factory feature
     * (check {@link JsonFactory.Feature} for list of features)
     */
    public JsonFactory enable(JsonFactory.Feature f) {
        _factoryFeatures |= f.getMask();
        if (f == Feature.USE_RELEASABLE_THREAD_LOCAL_BUFFERS) {
            enableUseReleasableThreadLocalBuffers(); // set to true for all instances
        }
        return this;
    }

    /**
     * Method for disabling specified factory features
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

    /**
     * Enables use of releasable ThreadLocalBuffers, released in shutDown method
     */
    public static void enableUseReleasableThreadLocalBuffers() {
        _bufferMgr = ThreadLocalBufferManager.instance();
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
     */
    public JsonFactory setRootValueSeparator(String sep) {
        _rootValueSeparator = (sep == null) ? null : new SerializedString(sep);
        return this;
    }

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
    /* Parser factories, traditional (blocking) I/O sources
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
     */
    public JsonParser createParser(Reader r) throws IOException, JsonParseException {
        // false -> we do NOT own Reader (did not create it)
        IOContext ctxt = _createContext(r, false);
        return _createParser(_decorate(r, ctxt), ctxt);
    }

    /**
     * Method for constructing parser for parsing
     * the contents of given byte array.
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
     * Method for constructing parser for parsing contents of given String.
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
     * Method for constructing parser for parsing contents of given char array.
     */
    public JsonParser createParser(char[] content) throws IOException {
        return createParser(content, 0, content.length);
    }
    
    /**
     * Method for constructing parser for parsing contents of given char array.
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
     * Optional method for constructing parser for reading contents from specified {@link DataInput}
     * instance.
     *<p>
     * If this factory does not support {@link DataInput} as source,
     * will throw {@link UnsupportedOperationException}
     */
    public JsonParser createParser(DataInput in) throws IOException {
        IOContext ctxt = _createContext(in, false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    /*
    /**********************************************************
    /* Parser factories, non-blocking (async) sources
    /**********************************************************
     */

    /**
     * Optional method for constructing parser for non-blocking parsing
     * via {@link com.fasterxml.jackson.core.async.ByteArrayFeeder}
     * interface (accessed using {@link JsonParser#getNonBlockingInputFeeder()}
     * from constructed instance).
     *<p>
     * If this factory does not support non-blocking parsing (either at all,
     * or from byte array),
     * will throw {@link UnsupportedOperationException}
     */
    public JsonParser createNonBlockingByteArrayParser() throws IOException
    {
        // 17-May-2017, tatu: Need to take care not to accidentally create JSON parser
        //   for non-JSON input:
        _requireJSONFactory("Non-blocking source not (yet?) support for this format (%s)");
        IOContext ctxt = _createContext(null, false);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChild(_factoryFeatures);
        return new NonBlockingJsonParser(ctxt, _parserFeatures, can);
    }

    /*
    /**********************************************************
    /* Generator factories
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
     */
    public JsonGenerator createGenerator(DataOutput out, JsonEncoding enc) throws IOException {
        return createGenerator(_createDataOutputWrapper(out), enc);
    }

    /**
     * Convenience method for constructing generator that uses default
     * encoding of the format (UTF-8 for JSON and most other data formats).
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats).
     */
    public JsonGenerator createGenerator(DataOutput out) throws IOException {
        return createGenerator(_createDataOutputWrapper(out), JsonEncoding.UTF8);
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
     */
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return new ReaderBasedJsonParser(ctxt, _parserFeatures, r, _objectCodec,
                _rootCharSymbols.makeChild(_factoryFeatures));
    }

    /**
     * Overridable factory method that actually instantiates parser
     * using given <code>char[]</code> object for accessing content.
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
     * Optional factory method, expected to be overridden
     */
    protected JsonParser _createParser(DataInput input, IOContext ctxt) throws IOException
    {
        // 13-May-2016, tatu: Need to take care not to accidentally create JSON parser for
        //   non-JSON input.
        _requireJSONFactory("InputData source not (yet?) support for this format (%s)");
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

    protected final InputStream _decorate(InputStream in, IOContext ctxt) throws IOException {
        if (_inputDecorator != null) {
            InputStream in2 = _inputDecorator.decorate(ctxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    protected final Reader _decorate(Reader in, IOContext ctxt) throws IOException {
        if (_inputDecorator != null) {
            Reader in2 = _inputDecorator.decorate(ctxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    protected final DataInput _decorate(DataInput in, IOContext ctxt) throws IOException {
        if (_inputDecorator != null) {
            DataInput in2 = _inputDecorator.decorate(ctxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    protected final OutputStream _decorate(OutputStream out, IOContext ctxt) throws IOException {
        if (_outputDecorator != null) {
            OutputStream out2 = _outputDecorator.decorate(ctxt, out);
            if (out2 != null) {
                return out2;
            }
        }
        return out;
    }

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
        if (Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING.enabledIn(_factoryFeatures)) {
            SoftReference<BufferRecycler> ref = _recyclerRef.get();
            BufferRecycler br = (ref == null) ? null : ref.get();
    
            if (br == null) {
                br = new BufferRecycler();
                SoftReference newRef;
                if (_bufferMgr != null) {
                    newRef = _bufferMgr.getReleasableSoftBufRecycler(br);
                }
                else {
                    newRef = new SoftReference<BufferRecycler>(br);
                }
                _recyclerRef.set(newRef);
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

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    /**
     * Helper method called to work around the problem of this class both defining
     * general API for constructing parsers+generators AND implementing the API
     * for JSON handling. Problem here is that when adding new functionality
     * via factory methods, it is not possible to leave these methods abstract
     * (because we are implementing them for JSON); but there is risk that
     * sub-classes do not override them all (plus older version can not implement).
     * So a work-around is to add a check to ensure that factory is still one
     * used for JSON; and if not, make base implementation of a factory method fail.
     */
    private final void _requireJSONFactory(String msg) {
        if (!_isJSONFactory()) {
            throw new UnsupportedOperationException(String.format(msg, getFormatName()));
        }
    }

    private final boolean _isJSONFactory() {
        // NOTE: since we only really care about whether this is standard JSON-backed factory,
        // or its sub-class / delegated to one, no need to check for equality, identity is enough
        return getFormatName() == FORMAT_NAME_JSON;
    }

    /*
     * For issue #400: We keep a separate Set of all SoftReferences to BufferRecyclers which we put in all ThreadLocals
     * We do this to be able to release them (dereference) in a newly introduced shutdown method.
     * When gc clears a SoftReference, it puts it on a newly introduced referenceQueue. We use this queue to release the inactive SoftReferences from the Set.
     */
    private static final class ThreadLocalBufferManager {

        /**
         * A lock to make sure shutdown is only be executed by one thread at a time since it iterates over and modifies the allSoftBufRecyclers.
         */
        private final Object SHUTDOWN_LOCK = new Object();

        /**
         * A set of all SoftReferences to all BufferRecyclers to be able to release them on shutdown.
         * 'All' means the ones created by this class, in this classloader. There may be more from other classloaders.
         * We use a HashSet to have quick O(1) add and remove operations.
         * The Set is backed by a ConcurrentHashMap to mitigate locking overhead.
         */
        private final Set<SoftReference<BufferRecycler>> allSoftBufRecyclers = Collections.newSetFromMap(new ConcurrentHashMap<SoftReference<BufferRecycler>, Boolean>());

        /**
         * Queue where gc will put just-cleared SoftReferences, previously referencing BufferRecyclers.
         * We use it to remove the cleared softRefs from the above set.
         */
        private final ReferenceQueue<BufferRecycler> refQueue = new ReferenceQueue<BufferRecycler>();

        /**
         * Returns the lazily intialized singleton instance
         * @return the singleton instance
         */
        private static ThreadLocalBufferManager instance() {
            return ThreadLocalBufferManagerHolder.manager;
        }
        /**
         * Remove cleared (inactive) SoftRefs from our set. Gc may have cleared one or more, and made them inactive.
         * We minimize contention by keeping synchronized sections short: the poll/remove methods
         */
        private void removeSoftRefsClearedByGc() {
            SoftReference clearedSoftRef = null;
            do {
                clearedSoftRef = (SoftReference) refQueue.poll();
                if (clearedSoftRef != null) {
                    allSoftBufRecyclers.remove(clearedSoftRef); // uses reference-equality, quick, and O(1) removal by HashSet
                }
            } while(clearedSoftRef != null);
        }

        private SoftReference getReleasableSoftBufRecycler(BufferRecycler br) {
            SoftReference newRef;
            newRef = new SoftReference<BufferRecycler>(br, refQueue);
            // also retain softRef to br in a set to be able to release it on shutdown
            allSoftBufRecyclers.add(newRef);
            // gc may have cleared one or more SoftRefs, clean them up to avoid a memleak
            removeSoftRefsClearedByGc();
            return newRef;
        }

        /**
         * Releases the buffers retained in ThreadLocals. To be called on shutdown event of applications which make use of
         * an environment like an appserver which stays alive and uses a thread pool that causes ThreadLocals created by the
         * application to survive much longer than the application itself.
         * It will clear all bufRecyclers from the SoftRefs and release all SoftRefs itself from our set.
         */
        private void shutdown() {
            System.out.println("ThreadLocalBufferManager shutdown called"); // for debugging, @TODO remove after test
            synchronized(SHUTDOWN_LOCK) {
                removeSoftRefsClearedByGc(); // make sure the refQueue is empty
                for (SoftReference ref : allSoftBufRecyclers) {
                    ref.clear(); // possibly already cleared by gc, nothing happens in that case
                }
                allSoftBufRecyclers.clear(); //release cleared SoftRefs
            }
        }
    }

    /**
     * ThreadLocalBufferManagerHolder uses the thread-safe initialize-on-demand, holder class idiom that implicitly
     * incorporates lazy initialization by declaring a static variable within a static Holder inner class
     */
    private static final class ThreadLocalBufferManagerHolder {
        private static final ThreadLocalBufferManager manager = new ThreadLocalBufferManager();
    }
}
