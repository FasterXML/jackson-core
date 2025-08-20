/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package tools.jackson.core;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.sym.SimpleNameMatcher;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JacksonFeature;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.Named;
import tools.jackson.core.util.RecyclerPool;
import tools.jackson.core.util.Snapshottable;

/**
 * The main factory class of Jackson streaming package, used to configure and
 * construct token reader (aka parser, {@link JsonParser})
 * and token writer (aka generator, {@link JsonGenerator})
 * instances.
 *<p>
 * Factory instances are thread-safe and reusable after configuration
 * (if any). Typically applications and services use only a single
 * globally shared factory instance, unless they need differently
 * configured factories. Factory reuse is important if efficiency matters;
 * most recycling of expensive construct is done on per-factory basis.
 *<p>
 * Creation of a factory instance is a light-weight operation.
 *
 * @since 3.0 (refactored out of {@link JsonFactory})
 */
public abstract class TokenStreamFactory
    implements Versioned,
        java.io.Serializable,
        Snapshottable<TokenStreamFactory> // 3.0
{
    private static final long serialVersionUID = 3L;

    /**
     * Shareable stateles "empty" {@link ObjectWriteContext} Object, passed in
     * cases where caller had not passed actual context - "null object" of sort.
     */
    public final static ObjectWriteContext EMPTY_WRITE_CONTEXT = new ObjectWriteContext.Base();

    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    /**
     * Enumeration that defines all on/off features that can only be
     * changed for {@link TokenStreamFactory}.
     */
    public enum Feature
        implements JacksonFeature
    {

        // // // Symbol handling (interning etc)

        /**
         * Feature that determines whether JSON object property names are
         * to be canonicalized using {@link String#intern} or not:
         * if enabled, all property names will be intern()ed (and caller
         * can count on this being true for all such names); if disabled,
         * no intern()ing is done. There may still be basic
         * canonicalization (that is, same String will be used to represent
         * all identical object property names for a single document).
         *<p>
         * Note: this setting only has effect if
         * {@link #CANONICALIZE_PROPERTY_NAMES} is true -- otherwise no
         * canonicalization of any sort is done.
         *<p>
         * This setting is disabled by default since 3.0 (was enabled in 1.x and 2.x)
         */
        INTERN_PROPERTY_NAMES(false),

        /**
         * Feature that determines whether JSON object property names are
         * to be canonicalized (details of how canonicalization is done
         * then further specified by
         * {@link #INTERN_PROPERTY_NAMES}).
         *<p>
         * This setting is enabled by default.
         */
        CANONICALIZE_PROPERTY_NAMES(true),

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
         * Feature to control charset detection for byte-based inputs ({@code byte[]}, {@link InputStream}...). When
         * this feature is enabled (the default), the factory will allow UTF-16 and UTF-32 inputs and try to detect
         * them, as specified by RFC 4627. When this feature is disabled the factory will assume UTF-8, as specified
         * by RFC 8259.
         *<p>
         * NOTE: only applies to some implementations: most notable for {@code JsonFactory}.
         */
        CHARSET_DETECTION(true),        
        ;

        /**
         * Whether feature is enabled or disabled by default.
         */
        private final boolean _defaultState;

        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         *
         * @return Bit mask of all features that are enabled by default
         */
        public static int collectDefaults() {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) { flags |= f.getMask(); }
            }
            return flags;
        }

        private Feature(boolean defaultState) { _defaultState = defaultState; }

        @Override
        public boolean enabledByDefault() { return _defaultState; }
        @Override
        public boolean enabledIn(int flags) { return (flags & getMask()) != 0; }
        @Override
        public int getMask() { return (1 << ordinal()); }
    }

    /*
    /**********************************************************************
    /* Constants
    /**********************************************************************
     */

    /**
     * Bitfield (set of flags) of all factory features that are enabled by default.
     */
    protected final static int DEFAULT_FACTORY_FEATURE_FLAGS = TokenStreamFactory.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    protected final static int DEFAULT_STREAM_READ_FEATURE_FLAGS = StreamReadFeature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    protected final static int DEFAULT_STREAM_WRITE_FEATURE_FLAGS = StreamWriteFeature.collectDefaults();

    /*
    /**********************************************************************
    /* Configuration, simple features
    /**********************************************************************
     */

    /**
     * Currently enabled {@link TokenStreamFactory.Feature}s features as a bitmask.
     */
    protected final int _factoryFeatures;

    /**
     * Currently enabled {@link StreamReadFeature}s as a bitmask.
     */
    protected final int _streamReadFeatures;

    /**
     * Currently enabled {@link StreamWriteFeature}s as a bitmask.
     */
    protected final int _streamWriteFeatures;

    /**
     * Set of format-specific read features enabled, as bitmask.
     */
    protected final int _formatReadFeatures;

    /**
     * Set of format-specific write features enabled, as bitmask.
     */
    protected final int _formatWriteFeatures;

    /*
    /**********************************************************************
    /* Configuration, providers
    /**********************************************************************
     */

    protected final RecyclerPool<BufferRecycler> _recyclerPool;

    /*
    /**********************************************************************
    /* Configuration, constraints
    /**********************************************************************
     */
    
    /**
     * Active StreamReadConstraints to use.
     */
    protected final StreamReadConstraints _streamReadConstraints;

    /**
     * Active StreamWriteConstraints to use.
     */
    protected final StreamWriteConstraints _streamWriteConstraints;

    /**
     * Active ErrorReportConfiguration to use.
     */
    protected final ErrorReportConfiguration _errorReportConfiguration;    

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
     *
     * @param src StreamReadConstraints to use with parsers factory creates
     * @param swc StreamWriteConstraints to use with generators factory creates
     * @param erc ErrorReportConfiguration to use with parsers factory creates
     * @param formatReadFeatures Bitmask of format-specific read features enabled
     * @param formatWriteFeatures Bitmask of format-specific write features enabled
     */
    protected TokenStreamFactory(StreamReadConstraints src, StreamWriteConstraints swc,
            ErrorReportConfiguration erc,
            int formatReadFeatures, int formatWriteFeatures) {
        _streamReadConstraints = Objects.requireNonNull(src);
        _streamWriteConstraints = Objects.requireNonNull(swc);
        _errorReportConfiguration = Objects.requireNonNull(erc);
        _recyclerPool = JsonRecyclerPools.defaultPool();
        _factoryFeatures = DEFAULT_FACTORY_FEATURE_FLAGS;
        _streamReadFeatures = DEFAULT_STREAM_READ_FEATURE_FLAGS;
        _streamWriteFeatures = DEFAULT_STREAM_WRITE_FEATURE_FLAGS;
        _formatReadFeatures = formatReadFeatures;
        _formatWriteFeatures = formatWriteFeatures;
    }

    /**
     * Constructors used by {@link TSFBuilder} for instantiation. Base builder is
     * passed as-is to try to make interface between base types and implementations
     * less likely to change (given that sub-classing is a fragile way to do it):
     * if and when new general-purpose properties are added, implementation classes
     * do not have to use different constructors.
     *
     * @param baseBuilder Builder with configuration to use
     *
     * @since 3.0
     */
    protected TokenStreamFactory(TSFBuilder<?,?> baseBuilder)
    {
        _streamReadConstraints = Objects.requireNonNull(baseBuilder._streamReadConstraints);
        _streamWriteConstraints = Objects.requireNonNull(baseBuilder._streamWriteConstraints);
        _errorReportConfiguration = Objects.requireNonNull(baseBuilder._errorReportConfiguration);

        // 19-Apr-2024, tatu: [core#1269] allow `null` to indicate "just use
        //    default to lazily instantiate
        RecyclerPool<BufferRecycler> rp = baseBuilder._recyclerPool;
        if (rp == null) {
            rp = JsonRecyclerPools.defaultPool();
        }
        _recyclerPool = rp;

        _factoryFeatures = baseBuilder.factoryFeaturesMask();
        _streamReadFeatures = baseBuilder.streamReadFeaturesMask();
        _streamWriteFeatures = baseBuilder.streamWriteFeaturesMask();
        _formatReadFeatures = baseBuilder.formatReadFeaturesMask();
        _formatWriteFeatures = baseBuilder.formatWriteFeaturesMask();
    }

    /**
     * Constructor used if a snapshot is created, or possibly for sub-classing or
     * wrapping (delegating)
     *
     * @param src Source factory with configuration to copy
     */
    protected TokenStreamFactory(TokenStreamFactory src)
    {
        _streamReadConstraints = src._streamReadConstraints;
        _streamWriteConstraints = src._streamWriteConstraints;
        _errorReportConfiguration = src._errorReportConfiguration;
        _recyclerPool = src._recyclerPool;

        _factoryFeatures = src._factoryFeatures;
        _streamReadFeatures = src._streamReadFeatures;
        _streamWriteFeatures = src._streamWriteFeatures;
        _formatReadFeatures = src._formatReadFeatures;
        _formatWriteFeatures = src._formatWriteFeatures;
    }

    /**
     * Method similar to {@link #snapshot()}, but one that forces creation of actual
     * new copy that does NOT share any state, even non-visible to calling code,
     * such as symbol table reuse.
     *<p>
     * Implementation should be functionally equivalent to:
     *<pre>
     *    factoryInstance.rebuild().build();
     *</pre>
     *
     * @return Newly constructed stream factory instance of same type as this one,
     *    with identical configuration settings
     */
    public abstract TokenStreamFactory copy();

    /**
     * Method for constructing a new {@link TokenStreamFactory} that has
     * the same settings as this instance, but is otherwise
     * independent (i.e. nothing is actually shared, symbol tables
     * are separate).
     *<p>
     * Although assumption is that all factories are immutable -- and that we could
     * usually simply return `this` -- method is left unimplemented at this level
     * to make implementors aware of requirement to consider immutability.
     *
     * @return This factory instance to allow call chaining
     *
     * @since 3.0
     */
    @Override
    public abstract TokenStreamFactory snapshot();

    /**
     * Method that can be used to create differently configured stream factories: it will
     * create and return a Builder instance with exact settings of this stream factory.
     *
     * @return Builder instance initialized with configuration this stream factory has
     *
     * @since 3.0
     */
    public abstract TSFBuilder<?,?> rebuild();
//    public abstract <F extends TokenStreamFactory, T extends TSFBuilder<F,T>> TSFBuilder<F,T> rebuild();

    /*
    /**********************************************************************
    /* Capability introspection
    /**********************************************************************
     */

    /**
     * Introspection method that higher-level functionality may call
     * to see whether underlying data format requires a stable ordering
     * of object properties or not.
     * This is usually used for determining
     * whether to force a stable ordering (like alphabetic ordering by name)
     * if no ordering if explicitly specified.
     *
     * @return Whether format supported by this factory
     *   requires Object properties to be ordered.
     */
    public boolean requiresPropertyOrdering() { return false; }

    /**
     * Introspection method that higher-level functionality may call
     * to see whether underlying data format can read and write binary
     * data natively; that is, embedded it as-is without using encodings
     * such as Base64.
     *
     * @return Whether format supported by this factory
     *    supports native binary content
     */
    public abstract boolean canHandleBinaryNatively();

    /**
     * Introspection method that can be used to check whether this
     * factory can create non-blocking parsers: parsers that do not
     * use blocking I/O abstractions but instead use a
     * {@link tools.jackson.core.async.NonBlockingInputFeeder}.
     *
     * @return Whether this factory supports non-blocking ("async") parsing or
     *    not (and consequently whether {@code createNonBlockingXxx()} method(s) work)
     */
    public abstract boolean canParseAsync();

    /**
     * Method for accessing kind of {@link FormatFeature} that a parser
     * {@link JsonParser} produced by this factory would accept, if any;
     * <code>null</code> returned if none.
     *
     * @return Type of format-specific stream read features, if any; {@code null} if none
     */
    public Class<? extends FormatFeature> getFormatReadFeatureType() {
        return null;
    }

    /**
     * Method for accessing kind of {@link FormatFeature} that a parser
     * {@link JsonGenerator} produced by this factory would accept, if any;
     * <code>null</code> returned if none.
     *
     * @return Type of format-specific stream read features, if any; {@code null} if none
     */
    public Class<? extends FormatFeature> getFormatWriteFeatureType() {
        return null;
    }

    /*
    /**********************************************************************
    /* Format detection functionality
    /**********************************************************************
     */

    /**
     * Method that can be used to quickly check whether given schema
     * is something that parsers and/or generators constructed by this
     * factory could use. Note that this means possible use, at the level
     * of data format (i.e. schema is for same data format as parsers and
     * generators this factory constructs); individual schema instances
     * may have further usage restrictions.
     *
     * @param schema Schema instance to check
     *
     * @return Whether parsers and generators constructed by this factory
     *   can use specified format schema instance
     */
    public abstract boolean canUseSchema(FormatSchema schema);

    /**
     * Method that returns short textual id identifying format
     * this factory supports.
     *
     * @return Name of the format handled by parsers, generators this factory creates
     */
    public abstract String getFormatName();

    /*
    /**********************************************************************
    /* Versioned
    /**********************************************************************
     */

    @Override
    public abstract Version version();

    /*
    /**********************************************************************
    /* Configuration, access to features
    /**********************************************************************
     */

    /**
     * Checked whether specified parser feature is enabled.
     *
     * @param f Feature to check
     *
     * @return {@code True} if feature is enabled; {@code false} otherwise
     */
    public final boolean isEnabled(TokenStreamFactory.Feature f) {
        return (_factoryFeatures & f.getMask()) != 0;
    }

    /**
     * Checked whether specified parser feature is enabled.
     *
     * @param f Feature to check
     *
     * @return {@code True} if feature is enabled; {@code false} otherwise
     */
    public final boolean isEnabled(StreamReadFeature f) {
        return (_streamReadFeatures & f.getMask()) != 0;
    }

    /**
     * Checked whether specified parser feature is enabled.
     *
     * @param f Feature to check
     *
     * @return {@code True} if feature is enabled; {@code false} otherwise
     */
    public final boolean isEnabled(StreamWriteFeature f) {
        return (_streamWriteFeatures & f.getMask()) != 0;
    }

    public final int getFactoryFeatures() {
        return _factoryFeatures;
    }

    // @since 3.0
    public final int getStreamReadFeatures() {
        return _streamReadFeatures;
    }

    // @since 3.0
    public final int getStreamWriteFeatures() {
        return _streamWriteFeatures;
    }

    // @since 3.0
    public int getFormatReadFeatures() { return _formatReadFeatures; }

    // @since 3.0
    public int getFormatWriteFeatures() { return _formatWriteFeatures; }

    public StreamReadConstraints streamReadConstraints() { return _streamReadConstraints; }

    public StreamWriteConstraints streamWriteConstraints() { return _streamWriteConstraints; }

    public ErrorReportConfiguration errorReportConfiguration() { return _errorReportConfiguration; }

    /*
    /**********************************************************************
    /* Factory methods for helper objects
    /**********************************************************************
     */

    /**
     * Factory method for constructing case-sensitive {@link PropertyNameMatcher}
     * for given names. It will call {@link String#intern} on names unless specified
     * that this has already been done by caller.
     *
     * @param matches Names to match, including both primary names and possible aliases
     * @param alreadyInterned Whether name Strings are already {@code String.intern()ed} or not
     *
     * @return Case-sensitive {@link PropertyNameMatcher} instance to use
     */
    public PropertyNameMatcher constructNameMatcher(List<Named> matches, boolean alreadyInterned) {
        // 15-Nov-2017, tatu: Base implementation that is likely to work fine for
        //    most if not all implementations as it is more difficult to optimize
        return SimpleNameMatcher.constructFrom(null, matches, alreadyInterned);
    }

    /**
     * Factory method for constructing case-insensitive {@link PropertyNameMatcher}
     * for given names. It will call {@link String#intern} on names unless specified
     * that this has already been done by caller.
     *
     * @param matches Names to match, including both primary names and possible aliases
     * @param alreadyInterned Whether name Strings are already {@code String.intern()ed} or not
     * @param locale Locale to use for case-handling
     *
     * @return Case-insensitive {@link PropertyNameMatcher} instance to use
     */
    public PropertyNameMatcher constructCINameMatcher(List<Named> matches, boolean alreadyInterned,
            Locale locale) {
        return SimpleNameMatcher.constructCaseInsensitive(locale, matches, alreadyInterned);
    }

    /*
    /**********************************************************************
    /* Parser factories, traditional (blocking) I/O sources
    /**********************************************************************
     */

    /**
     * Method for constructing parser instance to decode
     * contents of specified file.
     *<p>
     * Encoding is auto-detected from contents according to JSON
     * specification recommended mechanism. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param readCtxt Object read context to use
     * @param f File that contains content to parse
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            File f) throws JacksonException;

    /**
     * Method for constructing parser instance to decode
     * contents of specified path.
     *<p>
     * Encoding is auto-detected from contents according to JSON
     * specification recommended mechanism. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param readCtxt Object read context to use
     * @param p Path that contains content to parse
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     *
     * @since 3.0
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            Path p) throws JacksonException;

    /**
     * Method for constructing JSON parser instance to parse
     * the contents accessed via specified input stream.
     *<p>
     * The input stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link tools.jackson.core.StreamReadFeature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *<p>
     * Note: no encoding argument is taken since it can always be
     * auto-detected as suggested by JSON RFC. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     * @param readCtxt Object read context to use
     * @param in InputStream to use for reading content to parse
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            InputStream in) throws JacksonException;

    /**
     * Method for constructing parser for parsing
     * the contents accessed via specified Reader.
     <p>
     * The read stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link tools.jackson.core.StreamReadFeature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *
     * @param readCtxt Object read context to use
     * @param r Reader to use for reading content to parse
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            Reader r) throws JacksonException;

    /**
     * Method for constructing parser for parsing
     * the contents of given byte array.
     *
     * @param readCtxt Object read context to use
     * @param data Content to read
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public JsonParser createParser(ObjectReadContext readCtxt, byte[] data) throws JacksonException {
        return createParser(readCtxt, data, 0, data.length);
    }

    /**
     * Method for constructing parser for parsing
     * the contents of given byte array.
     *
     * @param readCtxt Object read context to use
     * @param content Buffer that contains data to parse
     * @param offset Offset of the first data byte within buffer
     * @param len Length of contents to parse within buffer
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            byte[] content, int offset, int len) throws JacksonException;

    /**
     * Method for constructing parser for parsing contents of given String.
     *
     * @param readCtxt Object read context to use
     * @param content Content to read
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            String content) throws JacksonException;

    /**
     * Method for constructing parser for parsing contents of given char array.
     *
     * @param readCtxt Object read context to use
     * @param content Content to read
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public JsonParser createParser(ObjectReadContext readCtxt,
            char[] content) throws JacksonException {
        return createParser(readCtxt, content, 0, content.length);
    }

    /**
     * Method for constructing parser for parsing contents of given char array.
     *
     * @param readCtxt Object read context to use
     * @param content Buffer that contains data to parse
     * @param offset Offset of the first data byte within buffer
     * @param len Length of contents to parse within buffer
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            char[] content, int offset, int len) throws JacksonException;

    /*
    /**********************************************************************
    /* Parser factories, convenience methods that do not specify
    /* `ObjectReadContext`
    /**********************************************************************
     */

    /**
     * @param f File that contains content to parse
     * @return Parser constructed
     * @throws JacksonException If parser construction or initialization fails
     *
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,java.io.File)}
     */
    @Deprecated
    public JsonParser createParser(File f) throws JacksonException {
        return createParser(ObjectReadContext.empty(), f);
    }

    /**
     * @param in InputStream to use for reading content to parse
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     *
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,java.io.InputStream)}
     */
    @Deprecated
    public JsonParser createParser(InputStream in) throws JacksonException {
        return createParser(ObjectReadContext.empty(), in);
    }

    /**
     * @param r Reader to use for reading content to parse
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     *
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,java.io.Reader)}
     */
    @Deprecated
    public JsonParser createParser(Reader r) throws JacksonException {
        return createParser(ObjectReadContext.empty(), r);
    }

    /**
     * @param content Buffer that contains content to parse
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     *
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,byte[])}
     */
    @Deprecated
    public JsonParser createParser(byte[] content) throws JacksonException {
        return createParser(ObjectReadContext.empty(), content, 0, content.length);
    }

    /**
     * @param content Buffer that contains content to parse
     * @param offset Offset of the first data byte within buffer
     * @param len Length of contents to parse within buffer
     *
     * @return Parser constructed
     * @throws JacksonException If parser construction or initialization fails
     *
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,byte[],int,int)}
     */
    @Deprecated
    public JsonParser createParser(byte[] content, int offset, int len) throws JacksonException {
        return createParser(ObjectReadContext.empty(), content, offset, len);
    }

    /**
     * @param content Content to parse
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     *
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,String)}
     */
    @Deprecated
    public JsonParser createParser(String content) throws JacksonException {
        return createParser(ObjectReadContext.empty(), content);
    }

    /**
     * @param content Buffer that contains data to parse
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     *
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,char[])}
     */
    @Deprecated
    public JsonParser createParser(char[] content) throws JacksonException {
        return createParser(ObjectReadContext.empty(), content, 0, content.length);
    }

    /**
     * @param content Buffer that contains data to parse
     * @param offset Offset of the first data byte within buffer
     * @param len Length of contents to parse within buffer
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     *
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,char[],int,int)}
     */
    @Deprecated
    public JsonParser createParser(char[] content, int offset, int len) throws JacksonException {
        return createParser(ObjectReadContext.empty(), content, offset, len);
    }

    /*
    /**********************************************************************
    /* Parser factories, non-stream sources
    /**********************************************************************
     */

    /**
     * Optional method for constructing parser for reading contents from specified {@link DataInput}
     * instance.
     *<p>
     * If this factory does not support {@link DataInput} as source,
     * will throw {@link UnsupportedOperationException}
     *
     * @param readCtxt Object read context to use
     * @param in InputStream to use for reading content to parse
     *
     * @return Parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt, DataInput in)
        throws JacksonException;

    /**
     * Optional method for constructing parser for non-blocking parsing
     * via {@link tools.jackson.core.async.ByteArrayFeeder}
     * interface (accessed using {@link JsonParser#nonBlockingInputFeeder()}
     * from constructed instance).
     *<p>
     * If this factory does not support non-blocking parsing (either at all,
     * or from byte array),
     * will throw {@link UnsupportedOperationException}.
     *<p>
     * Note that JSON-backed factory only supports parsing of UTF-8 encoded JSON content
     * (and US-ASCII since it is proper subset); other encodings are not supported
     * at this point.
     *
     * @param <P> Nominal type of parser constructed and returned
     * @param readCtxt Object read context to use
     *
     * @return Non-blocking parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public <P extends JsonParser & ByteArrayFeeder> P createNonBlockingByteArrayParser(ObjectReadContext readCtxt)
            throws JacksonException {
        return _unsupported("Non-blocking source not (yet?) supported for this format ("+getFormatName()+")");
    }

    /**
     * Optional method for constructing parser for non-blocking parsing
     * via {@link tools.jackson.core.async.ByteBufferFeeder}
     * interface (accessed using {@link JsonParser#nonBlockingInputFeeder()}
     * from constructed instance).
     *<p>
     * If this factory does not support non-blocking parsing (either at all,
     * or from byte array),
     * will throw {@link UnsupportedOperationException}.
     *<p>
     * Note that JSON-backed factory only supports parsing of UTF-8 encoded JSON content
     * (and US-ASCII since it is proper subset); other encodings are not supported
     * at this point.
     *
     * @param <P> Nominal type of parser constructed and returned
     * @param readCtxt Object read context to use
     *
     * @return Non-blocking parser constructed
     *
     * @throws JacksonException If parser construction or initialization fails
     */
    public  <P extends JsonParser & ByteArrayFeeder> P createNonBlockingByteBufferParser(ObjectReadContext readCtxt)
        throws JacksonException {
        return _unsupported("Non-blocking source not (yet?) supported for this format ("+getFormatName()+")");
    }

    /*
    /**********************************************************************
    /* Generator factories with databind context (3.0)
    /**********************************************************************
     */

    /**
     * Method for constructing generator that writes contents
     * using specified {@link OutputStream}.
     * Textual encoding used will be UTF-8, where applicable.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the output stream when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param out OutputStream to use for writing content
     *
     * @return Generator constructed
     *
     * @throws JacksonException If generator construction or initialization fails
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt, OutputStream out)
        throws JacksonException {
        return createGenerator(writeCtxt, out, JsonEncoding.UTF8);
    }

    /**
     * Method for constructing generator that writes contents
     * using specified {@link OutputStream} with specified textual encoding
     * (if applicable).
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the output stream when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param out OutputStream to use for writing content
     * @param enc Character set encoding to use (usually {@link JsonEncoding#UTF8})
     *
     * @return Generator constructed
     *
     * @throws JacksonException If generator construction or initialization fails
     *
     * @since 3.0
     */
    public abstract JsonGenerator createGenerator(ObjectWriteContext writeCtxt,
            OutputStream out, JsonEncoding enc)
        throws JacksonException;

    /**
     * Method for constructing generator that writes contents
     * using specified {@link Writer}.
     * Textual encoding used will be UTF-8, where applicable.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET} is enabled).
     * Using application needs to close it explicitly.
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param w Writer to use for writing content
     *
     * @return Generator constructed
     *
     * @throws JacksonException If generator construction or initialization fails
     *
     * @since 3.0
     */
    public abstract JsonGenerator createGenerator(ObjectWriteContext writeCtxt, Writer w)
        throws JacksonException;

    /**
     * Method for constructing generator that writes contents
     * to specified file, overwriting contents it might have (or creating
     * it if such file does not yet exist).
     *<p>
     * Underlying stream <b>is owned</b> by the generator constructed,
     * i.e. generator will handle closing of file when
     * {@link JsonGenerator#close} is called.
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param f File to write contents to
     * @param enc Character set encoding to use (usually {@link JsonEncoding#UTF8})
     *
     * @return Generator constructed
     *
     * @throws JacksonException If generator construction or initialization fails
     *
     * @since 3.0
     */
    public abstract JsonGenerator createGenerator(ObjectWriteContext writeCtxt, File f,
            JsonEncoding enc)
        throws JacksonException;

    /**
     * Method for constructing generator that writes contents
     * to specified path, overwriting contents it might have (or creating
     * it if such path does not yet exist).
     *<p>
     * Underlying stream <b>is owned</b> by the generator constructed,
     * i.e. generator will handle closing of path when
     * {@link JsonGenerator#close} is called.
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param p Path to write contents to
     * @param enc Character set encoding to use (usually {@link JsonEncoding#UTF8})
     *
     * @return Generator constructed
     *
     * @throws JacksonException If generator construction or initialization fails
     *
     * @since 3.0
     */
    public abstract JsonGenerator createGenerator(ObjectWriteContext writeCtxt, Path p,
            JsonEncoding enc)
        throws JacksonException;

    /**
     * Method for constructing generator that writes content into specified {@link DataOutput},
     * using UTF-8 encoding (with formats where encoding is user-configurable).
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param out {@link DataOutput} used as target of generation
     *
     * @return Generator constructed
     *
     * @throws JacksonException If generator construction or initialization fails
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt, DataOutput out)
            throws JacksonException {
        return createGenerator(writeCtxt, _createDataOutputWrapper(out));
    }

    /*
    /**********************************************************************
    /* Generator factories, convenience methods that do not specify
    /* `ObjectWriteContext`
    /**********************************************************************
     */

    /**
     * Method for constructing JSON generator for writing content
     * using specified output stream.
     * Encoding to use must be specified, and needs to be one of available
     * types (as per JSON specification).
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the output stream when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats)
     * and that ignore passed in encoding.
     *
     * @param out OutputStream to use for writing content
     * @param enc Character encoding to use
     *
     * @return Generator constructed
     *
     * @throws JacksonException If generator construction or initialization fails
     */
    @Deprecated // since 3.0
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc)
        throws JacksonException {
        return createGenerator(ObjectWriteContext.empty(), out, enc);
    }

    /**
     * Convenience method for constructing generator that uses default
     * encoding of the format (UTF-8 for JSON and most other data formats),
     *
     * @param out OutputStream to use for writing content
     *
     * @return Generator constructed
     *
     * @throws JacksonException If generator construction or initialization fails
     */
    @Deprecated // since 3.0
    public JsonGenerator createGenerator(OutputStream out) throws JacksonException {
        return createGenerator(ObjectWriteContext.empty(), out, JsonEncoding.UTF8);
    }

    /**
     * Method for constructing JSON generator for writing content
     * using specified Writer.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET} is enabled).
     * Using application needs to close it explicitly.
     *
     * @param w Writer to use for writing content
     *
     * @return Generator constructed
     *
     * @throws JacksonException If generator construction or initialization fails
     */
    @Deprecated // since 3.0
    public JsonGenerator createGenerator(Writer w) throws JacksonException {
        return createGenerator(ObjectWriteContext.empty(), w);
    }

    /*
    /**********************************************************************
    /* Internal factory methods, other
    /**********************************************************************
     */

    /**
     * Method used by factory to create buffer recycler instances
     * for parsers and generators.
     *<p>
     * Note: only public to give access for {@code ObjectMapper}
     *
     * @return BufferRecycler instance to use
     */
    public BufferRecycler _getBufferRecycler()
    {
        return _getRecyclerPool().acquireAndLinkPooled();
    }

    /**
     * Accessor for getting access to {@link RecyclerPool} for getting
     * {@link BufferRecycler} instance to use.
     *
     * @return RecyclerPool configured for (and used by) this factory.
     */
    public RecyclerPool<BufferRecycler> _getRecyclerPool() {
        return _recyclerPool;
    }

    /**
     * Overridable factory method that actually instantiates desired
     * context object.
     *
     * @param contentRef Source reference to use (relevant to {@code TokenStreamLocation} construction)
     * @param resourceManaged Whether input/output buffers used are managed by this factory
     *
     * @return Context constructed
     */
    protected IOContext _createContext(ContentReference contentRef, boolean resourceManaged) {
        return _createContext(contentRef, resourceManaged, null);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * context object.
     *
     * @param contentRef Source reference to use (relevant to {@code TokenStreamLocation} construction)
     * @param resourceManaged Whether input/output buffers used are managed by this factory
     * @param enc Character encoding defined to be used/expected
     *
     * @return Context constructed
     */
    protected IOContext _createContext(ContentReference contentRef, boolean resourceManaged,
            JsonEncoding enc)
    {
        BufferRecycler br = null;
        Object content = (contentRef == null) ? null : contentRef.getRawContent();
        // 18-Jan-2024, tatu: [core#1195] Let's see if we can reuse already allocated recycler
        //   (is the case when SegmentedStringWriter / ByteArrayBuilder passed)
        if (content instanceof BufferRecycler.Gettable) {
            br = ((BufferRecycler.Gettable) content).bufferRecycler();
        }
        boolean recyclerExternal = (br != null);
        if (!recyclerExternal) {
            br = _getBufferRecycler();
        }
        IOContext ctxt = new IOContext(_streamReadConstraints, _streamWriteConstraints,
                _errorReportConfiguration,
                br, contentRef,
                resourceManaged, enc);
        if (recyclerExternal) {
            ctxt.markBufferRecyclerReleased();
        }
        return ctxt;
    }

    /**
     * Overridable factory method for constructing {@link ContentReference}
     * to pass to parser or generator being created; used in cases where no offset
     * or length is applicable (either irrelevant, or full contents assumed).
     *
     * @param contentRef Underlying input source (parser) or target (generator)
     *
     * @return InputSourceReference instance to use
     */
    protected abstract ContentReference _createContentReference(Object contentRef);

    /**
     * Overridable factory method for constructing {@link ContentReference}
     * to pass to parser or generator being created; used in cases where input
     * comes in a static buffer with relevant offset and length.
     *
     * @param contentRef Underlying input source (parser) or target (generator)
     * @param offset Offset of content in buffer ({@code rawSource})
     * @param length Length of content in buffer ({@code rawSource})
     *
     * @return InputSourceReference instance to use
     */
    protected abstract ContentReference _createContentReference(Object contentRef,
            int offset, int length);

    protected OutputStream _createDataOutputWrapper(DataOutput out) {
        return new DataOutputAsStream(out);
    }

    /**
     * Helper method used for constructing an optimal stream for
     * parsers to use, when input is to be read from an URL.
     * This helps when reading file content via URL.
     *
     * @param url Source to read content to parse from
     *
     * @return InputStream constructed for given {@link URL}
     *
     * @throws JacksonException If there is a problem accessing content from specified {@link URL}
     *
     * @deprecated Since 3.0
     */
    @Deprecated // since 3.0
    protected InputStream _optimizedStreamFromURL(URL url) throws JacksonException {
        if ("file".equals(url.getProtocol())) {
            /* Cannot do this if the path refers
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
                    try {
                        return new FileInputStream(url.getPath());
                    } catch (IOException e) {
                        throw _wrapIOFailure(e);
                    }
                }
                // otherwise, let's fall through and let URL decoder do its magic
            }
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    /**
     * Helper methods used for constructing an {@link InputStream} for
     * parsers to use, when input is to be read from given {@link File}.
     *
     * @param f File to open stream for
     *
     * @return {@link InputStream} constructed
     *
     * @throws JacksonException If there is a problem opening the stream
     */
    protected InputStream _fileInputStream(File f) throws JacksonException
    {
        try {
            return new FileInputStream(f);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    protected InputStream _pathInputStream(Path p) throws JacksonException
    {
        try {
            return Files.newInputStream(p);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    /**
     * Helper methods used for constructing an {@link OutputStream} for
     * generator to use, when target is to be written into given {@link File}.
     *
     * @param f File to open stream for
     *
     * @return {@link OutputStream} constructed
     *
     * @throws JacksonException If there is a problem opening the stream
     */
    protected OutputStream _fileOutputStream(File f) throws JacksonException
    {
        try {
            return new FileOutputStream(f);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    protected OutputStream _pathOutputStream(Path p) throws JacksonException
    {
        try {
            return Files.newOutputStream(p);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    /*
    /**********************************************************************
    /* Range check helper methods
    /**********************************************************************
     */

    protected void _checkRangeBoundsForByteArray(byte[] data, int offset, int len)
        throws JacksonException
    {
        if (data == null) {
            _reportRangeError("Invalid `byte[]` argument: `null`");
        }
        final int dataLen = data.length;
        final int end = offset+len;

        // Note: we are checking that:
        //
        // !(offset < 0)
        // !(len < 0)
        // !((offset + len) < 0) // int overflow!
        // !((offset + len) > dataLen) == !((datalen - (offset+len)) < 0)

        // All can be optimized by OR'ing and checking for negative:
        int anyNegs = offset | len | end | (dataLen - end);
        if (anyNegs < 0) {
            _reportRangeError(String.format(
"Invalid 'offset' (%d) and/or 'len' (%d) arguments for `byte[]` of length %d",
offset, len, dataLen));
        }
    }

    protected void _checkRangeBoundsForCharArray(char[] data, int offset, int len)
        throws JacksonException
    {
        if (data == null) {
            _reportRangeError("Invalid `char[]` argument: `null`");
        }
        final int dataLen = data.length;
        final int end = offset+len;
        // Note: we are checking same things as with other bounds-checks
        int anyNegs = offset | len | end | (dataLen - end);
        if (anyNegs < 0) {
            _reportRangeError(String.format(
"Invalid 'offset' (%d) and/or 'len' (%d) arguments for `char[]` of length %d",
offset, len, dataLen));
        }
    }

    protected <T> T _reportRangeError(String msg) throws JacksonException
    {
        // In 2.x we used `IllegalArgumentException`: in 3.0 upgrade to StreamReadException
        throw new StreamReadException((JsonParser) null, msg);
    }

    /*
    /**********************************************************************
    /* Error reporting methods
    /**********************************************************************
     */

    protected JacksonException _wrapIOFailure(IOException e) {
        return JacksonIOException.construct(e, null);
    }

    protected <T> T _unsupported() {
        return _unsupported("Operation not supported for this format (%s)", getFormatName());
    }

    protected <T> T _unsupported(String str, Object... args) {
        throw new UnsupportedOperationException(String.format(str, args));
    }
}
