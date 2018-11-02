/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package com.fasterxml.jackson.core;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.core.io.*;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.sym.FieldNameMatcher;
import com.fasterxml.jackson.core.sym.SimpleNameMatcher;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.BufferRecyclers;
import com.fasterxml.jackson.core.util.Named;
import com.fasterxml.jackson.core.util.Snapshottable;

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
     * cases where caller had not passed actual context -- "null object" of sort.
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
         * This setting is disabled by default since 3.0 (was enabled in 1.x and 2.x)
         */
        INTERN_FIELD_NAMES(false),

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

    /**
     * Since factory instances are immutable, a Builder class is needed for creating
     * configurations for differently configured factory instances.
     *
     * @since 3.0
     */
    public abstract static class TSFBuilder<F extends TokenStreamFactory,
        B extends TSFBuilder<F,B>>
    {
        /**
         * Set of {@link TokenStreamFactory.Feature}s enabled, as bitmask.
         */
        protected int _factoryFeatures;

        /**
         * Set of {@link StreamReadFeature}s enabled, as bitmask.
         */
        protected int _streamReadFeatures;

        /**
         * Set of {@link StreamWriteFeature}s enabled, as bitmask.
         */
        protected int _streamWriteFeatures;

        /**
         * Set of format-specific read {@link FormatFeature}s enabled, as bitmask.
         */
        protected int _formatReadFeatures;

        /**
         * Set of format-specific write {@link FormatFeature}s enabled, as bitmask.
         */
        protected int _formatWriteFeatures;

        // // // Construction

        protected TSFBuilder(int formatReadF, int formatWriteF) {
            _factoryFeatures = DEFAULT_FACTORY_FEATURE_FLAGS;
            _streamReadFeatures = DEFAULT_STREAM_READ_FEATURE_FLAGS;
            _streamWriteFeatures = DEFAULT_STREAM_WRITE_FEATURE_FLAGS;
            _formatReadFeatures = formatReadF;
            _formatWriteFeatures = formatWriteF;
        }

        protected TSFBuilder(TokenStreamFactory base)
        {
            this(base._factoryFeatures,
                    base._streamReadFeatures, base._streamWriteFeatures,
                    base._formatReadFeatures, base._formatWriteFeatures);
        }

        protected TSFBuilder(int factoryFeatures,
                int streamReadFeatures, int streamWriteFeatures,
                int formatReadFeatures, int formatWriteFeatures)
        {
            _factoryFeatures = factoryFeatures;
            _streamReadFeatures = streamReadFeatures;
            _streamWriteFeatures = streamWriteFeatures;
            _formatReadFeatures = formatReadFeatures;
            _formatWriteFeatures = formatWriteFeatures;
        }

        // // // Accessors

        public int factoryFeaturesMask() { return _factoryFeatures; }
        public int streamReadFeaturesMask() { return _streamReadFeatures; }
        public int streamWriteFeaturesMask() { return _streamWriteFeatures; }

        public int formatReadFeaturesMask() { return _formatReadFeatures; }
        public int formatWriteFeaturesMask() { return _formatWriteFeatures; }

        // // // Factory features

        public B enable(TokenStreamFactory.Feature f) {
            _factoryFeatures |= f.getMask();
            return _this();
        }

        public B disable(TokenStreamFactory.Feature f) {
            _factoryFeatures &= ~f.getMask();
            return _this();
        }

        public B configure(TokenStreamFactory.Feature f, boolean state) {
            return state ? enable(f) : disable(f);
        }

        // // // Parser features

        public B enable(StreamReadFeature f) {
            _streamReadFeatures |= f.getMask();
            return _this();
        }

        public B enable(StreamReadFeature first, StreamReadFeature... other) {
            _streamReadFeatures |= first.getMask();
            for (StreamReadFeature f : other) {
                _streamReadFeatures |= f.getMask();
            }
            return _this();
        }

        public B disable(StreamReadFeature f) {
            _streamReadFeatures &= ~f.getMask();
            return _this();
        }

        public B disable(StreamReadFeature first, StreamReadFeature... other) {
            _streamReadFeatures &= ~first.getMask();
            for (StreamReadFeature f : other) {
                _streamReadFeatures &= ~f.getMask();
            }
            return _this();
        }

        public B configure(StreamReadFeature f, boolean state) {
            return state ? enable(f) : disable(f);
        }

        // // // Generator features

        public B enable(StreamWriteFeature f) {
            _streamWriteFeatures |= f.getMask();
            return _this();
        }

        public B enable(StreamWriteFeature first, StreamWriteFeature... other) {
            _streamWriteFeatures |= first.getMask();
            for (StreamWriteFeature f : other) {
                _streamWriteFeatures |= f.getMask();
            }
            return _this();
        }

        public B disable(StreamWriteFeature f) {
            _streamWriteFeatures &= ~f.getMask();
            return _this();
        }
        
        public B disable(StreamWriteFeature first, StreamWriteFeature... other) {
            _streamWriteFeatures &= ~first.getMask();
            for (StreamWriteFeature f : other) {
                _streamWriteFeatures &= ~f.getMask();
            }
            return _this();
        }

        public B configure(StreamWriteFeature f, boolean state) {
            return state ? enable(f) : disable(f);
        }

        // // // Other methods

        /**
         * Method for constructing actual {@link TokenStreamFactory} instance, given
         * configuration.
         */
        public abstract F build();

        // silly convenience cast method we need
        @SuppressWarnings("unchecked")
        protected final B _this() { return (B) this; }
    }

    /*
    /**********************************************************
    /* Constants
    /**********************************************************
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
    /**********************************************************
    /* Configuration
    /**********************************************************
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
    protected TokenStreamFactory(int formatReadFeatures, int formatWriteFeatures) {
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
     * @since 3.0
     */
    protected TokenStreamFactory(TSFBuilder<?,?> baseBuilder)
    {
        _factoryFeatures = baseBuilder.factoryFeaturesMask();
        _streamReadFeatures = baseBuilder.streamReadFeaturesMask();
        _streamWriteFeatures = baseBuilder.streamWriteFeaturesMask();
        _formatReadFeatures = baseBuilder.formatReadFeaturesMask();
        _formatWriteFeatures = baseBuilder.formatWriteFeaturesMask();
    }

    /**
     * Constructor used if a snapshot is created, or possibly for sub-classing or
     * wrapping (delegating)
     */
    protected TokenStreamFactory(TokenStreamFactory src)
    {
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
     * Implementation should be functionally equivalent to
     *<pre>
     *    factoryInstance.rebuild().build();
     *</pre>
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
     * @since 3.0
     */
    @Override
    public abstract TokenStreamFactory snapshot();

    /**
     * Method that can be used to create differently configured stream factories.
     *
     * @since 3.0
     */
    public abstract TSFBuilder<?,?> rebuild();
//    public abstract <F extends TokenStreamFactory, T extends TSFBuilder<F,T>> TSFBuilder<F,T> rebuild();

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
     */
    public boolean requiresPropertyOrdering() { return false; }

    /**
     * Introspection method that higher-level functionality may call
     * to see whether underlying data format can read and write binary
     * data natively; that is, embedded it as-is without using encodings
     * such as Base64.
     */
    public abstract boolean canHandleBinaryNatively();

    /**
     * Introspection method that can be used to check whether this
     * factory can create non-blocking parsers: parsers that do not
     * use blocking I/O abstractions but instead use a
     * {@link com.fasterxml.jackson.core.async.NonBlockingInputFeeder}.
     */
    public abstract boolean canParseAsync();

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
    public abstract boolean canUseSchema(FormatSchema schema);

    /**
     * Method that returns short textual id identifying format
     * this factory supports.
     *<p>
     * Note: sub-classes should override this method; default
     * implementation will return null for all sub-classes
     */
    public abstract String getFormatName();

    /*
    /**********************************************************
    /* Versioned
    /**********************************************************
     */

    @Override
    public abstract Version version();

    /*
    /**********************************************************
    /* Configuration, access to features
    /**********************************************************
     */

    public final boolean isEnabled(TokenStreamFactory.Feature f) {
        return (_factoryFeatures & f.getMask()) != 0;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(StreamReadFeature f) {
        return (_streamReadFeatures & f.getMask()) != 0;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(StreamWriteFeature f) {
        return (_streamWriteFeatures & f.getMask()) != 0;
    }

    /**
     * @since 3.0
     */
    public final int getStreamReadFeatures() {
        return _streamReadFeatures;
    }

    /**
     * @since 3.0
     */
    public final int getStreamWriteFeatures() {
        return _streamWriteFeatures;
    }

    /**
     * @since 3.0
     */
    public int getFormatReadFeatures() { return _formatReadFeatures; }

    /**
     * @since 3.0
     */
    public int getFormatWriteFeatures() { return _formatWriteFeatures; }

    /*
    /**********************************************************
    /* Factory methods for helper objects
    /**********************************************************
     */

    /**
     * Factory method for constructing case-sensitive {@link FieldNameMatcher}
     * for given names. It will call {@link String#intern} on names unless specified
     * that this has already been done by caller.
     *
     * @param matches Names to match, including both primary names and possible aliases
     */
    public FieldNameMatcher constructFieldNameMatcher(List<Named> matches, boolean alreadyInterned) {
        // 15-Nov-2017, tatu: Base implementation that is likely to work fine for
        //    most if not all implementations as it is more difficult to optimize
        return SimpleNameMatcher.constructFrom(matches, alreadyInterned);
    }

    /**
     * Factory method for constructing case-insensitive {@link FieldNameMatcher}
     * for given names. It will call {@link String#intern} on names unless specified
     * that this has already been done by caller.
     *
     * @param matches Names to match, including both primary names and possible aliases
     */
    public FieldNameMatcher constructCIFieldNameMatcher(List<Named> matches, boolean alreadyInterned) {
        return SimpleNameMatcher.constructCaseInsensitive(matches, alreadyInterned);
    }

    /*
    /**********************************************************
    /* Parser factories, traditional (blocking) I/O sources
    /**********************************************************
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
     *
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param f File that contains JSON content to parse
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            File f) throws IOException;

    /**
     * Method for constructing JSON parser instance to decode
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
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            URL url) throws IOException;

    /**
     * Method for constructing JSON parser instance to parse
     * the contents accessed via specified input stream.
     *<p>
     * The input stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link com.fasterxml.jackson.core.StreamReadFeature#AUTO_CLOSE_SOURCE}
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
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            InputStream in) throws IOException;

    /**
     * Method for constructing parser for parsing
     * the contents accessed via specified Reader.
     <p>
     * The read stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link com.fasterxml.jackson.core.StreamReadFeature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *
     * @param r Reader to use for reading JSON content to parse
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            Reader r) throws IOException;

    /**
     * Method for constructing parser for parsing
     * the contents of given byte array.
     */
    public JsonParser createParser(ObjectReadContext readCtxt, byte[] data) throws IOException {
        return createParser(readCtxt, data, 0, data.length);
    }

    /**
     * Method for constructing parser for parsing
     * the contents of given byte array.
     * 
     * @param data Buffer that contains data to parse
     * @param offset Offset of the first data byte within buffer
     * @param len Length of contents to parse within buffer
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            byte[] data, int offset, int len) throws IOException;

    /**
     * Method for constructing parser for parsing contents of given String.
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            String content) throws IOException;

    /**
     * Method for constructing parser for parsing contents of given char array.
     */
    public JsonParser createParser(ObjectReadContext readCtxt,
            char[] content) throws IOException {
        return createParser(readCtxt, content, 0, content.length);
    }

    /**
     * Method for constructing parser for parsing contents of given char array.
     */
    public abstract JsonParser createParser(ObjectReadContext readCtxt,
            char[] content, int offset, int len) throws IOException;

    /*
    /**********************************************************************
    /* Parser factories, convenience methods that do not specify
    /* `ObjectReadContext` 
    /**********************************************************************
     */

    /**
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,java.io.File)}
     */
    @Deprecated
    public JsonParser createParser(File src) throws IOException {
        return createParser(ObjectReadContext.empty(), src);
    }

    /**
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,java.net.URL)}
     */
    @Deprecated
    public JsonParser createParser(URL src) throws IOException {
        return createParser(ObjectReadContext.empty(), src);
    }

    /**
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,java.io.InputStream)}
     */
    @Deprecated
    public JsonParser createParser(InputStream in) throws IOException {
        return createParser(ObjectReadContext.empty(), in);
    }

    /**
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,java.io.Reader)}
     */
    @Deprecated
    public JsonParser createParser(Reader r) throws IOException {
        return createParser(ObjectReadContext.empty(), r);
    }

    /**
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,byte[])}
     */
    @Deprecated
    public JsonParser createParser(byte[] data) throws IOException {
        return createParser(ObjectReadContext.empty(), data, 0, data.length);
    }

    /**
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,byte[],int,int)}
     */
    @Deprecated
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException {
        return createParser(ObjectReadContext.empty(), data, offset, len);
    }

    /**
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,String)}
     */
    @Deprecated
    public JsonParser createParser(String content) throws IOException {
        return createParser(ObjectReadContext.empty(), content);
    }

    /**
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,char[])}
     */
    @Deprecated
    public JsonParser createParser(char[] content) throws IOException {
        return createParser(ObjectReadContext.empty(), content, 0, content.length);
    }

    /**
     * @deprecated Since 3.0 use {@link #createParser(ObjectReadContext,char[],int,int)}
     */
    @Deprecated
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
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
     */
    public abstract JsonParser createParser(ObjectReadContext readCtx,
            DataInput in) throws IOException;

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
    public JsonParser createNonBlockingByteArrayParser(ObjectReadContext readCtxt) throws IOException {
        return _unsupported("Non-blocking source not (yet?) support for this format ("+getFormatName()+")");        
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
     * {@link com.fasterxml.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param out OutputStream to use for writing JSON content
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt, OutputStream out)
        throws IOException {
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
     * {@link com.fasterxml.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param out OutputStream to use for writing JSON content
     *
     * @since 3.0
     */
    public abstract JsonGenerator createGenerator(ObjectWriteContext writeCtxt,
            OutputStream out, JsonEncoding enc)
        throws IOException;

    /**
     * Method for constructing generator that writes contents
     * using specified {@link Writer}.
     * Textual encoding used will be UTF-8, where applicable.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link com.fasterxml.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET} is enabled).
     * Using application needs to close it explicitly.
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param w Writer to use for writing JSON content 
     *
     * @since 3.0
     */
    public abstract JsonGenerator createGenerator(ObjectWriteContext writeCtxt, Writer w)
        throws IOException;

    /**
     * Method for constructing generator that writes contents
     * to specified file, overwriting contents it might have (or creating
     * it if such file does not yet exist).
     *<p>
     * Underlying stream <b>is owned</b> by the generator constructed,
     * i.e. generator will handle closing of file when
     * {@link JsonGenerator#close} is called.
     *
     * @since 3.0
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     * @param f File to write contents to
     */
    public abstract JsonGenerator createGenerator(ObjectWriteContext writeCtxt, File f,
            JsonEncoding enc)
        throws IOException;

    /**
     * Method for constructing generator that writes content into specified {@link DataOutput},
     * using UTF-8 encoding (with formats where encoding is user-configurable).
     *
     * @param writeCtxt Object-binding context where applicable; used for providing contextual
     *    configuration
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt, DataOutput out) throws IOException {
        return createGenerator(writeCtxt, _createDataOutputWrapper(out));
    }

    /*
    /**********************************************************************
    /* Generator factories, convenience methods that do not specify
    /* `ObjectWriteContext` 
    /**********************************************************************
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
     * {@link com.fasterxml.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats)
     * and that ignore passed in encoding.
     *
     * @param out OutputStream to use for writing JSON content 
     * @param enc Character encoding to use
     */
    @Deprecated
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc)
        throws IOException {
        return createGenerator(ObjectWriteContext.empty(), out, enc);
    }

    /**
     * Convenience method for constructing generator that uses default
     * encoding of the format (UTF-8 for JSON and most other data formats).
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats).
     */
    @Deprecated
    public JsonGenerator createGenerator(OutputStream out) throws IOException {
        return createGenerator(ObjectWriteContext.empty(), out, JsonEncoding.UTF8);
    }
    
    /**
     * Method for constructing JSON generator for writing JSON content
     * using specified Writer.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link com.fasterxml.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET} is enabled).
     * Using application needs to close it explicitly.
     *
     * @param w Writer to use for writing JSON content 
     */
    @Deprecated
    public JsonGenerator createGenerator(Writer w) throws IOException {
        return createGenerator(ObjectWriteContext.empty(), w);
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
    @Deprecated
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException {
        return createGenerator(ObjectWriteContext.empty(), f, enc);
    }

    /**
     * Method for constructing generator for writing content using specified
     * {@link DataOutput} instance.
     */
    @Deprecated
    public JsonGenerator createGenerator(DataOutput out, JsonEncoding enc) throws IOException {
        return createGenerator(ObjectWriteContext.empty(),
                _createDataOutputWrapper(out), enc);
    }

    /**
     * Convenience method for constructing generator that uses default
     * encoding of the format (UTF-8 for JSON and most other data formats).
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats).
     */
    @Deprecated
    public JsonGenerator createGenerator(DataOutput out) throws IOException {
        return createGenerator(ObjectWriteContext.empty(),
                _createDataOutputWrapper(out), JsonEncoding.UTF8);
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
            return BufferRecyclers.getBufferRecycler();
        }
        return new BufferRecycler();
    }

    /**
     * Overridable factory method that actually instantiates desired
     * context object.
     */
    protected IOContext _createContext(Object srcRef, boolean resourceManaged) {
        return new IOContext(_getBufferRecycler(), srcRef, resourceManaged, null);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * context object.
     */
    protected IOContext _createContext(Object srcRef, boolean resourceManaged,
            JsonEncoding enc) {
        return new IOContext(_getBufferRecycler(), srcRef, resourceManaged, enc);
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

    protected <T> T _unsupported() {
        return _unsupported("Operation not supported for this format (%s)", getFormatName());
    }
    
    protected <T> T _unsupported(String str, Object... args) {
        throw new UnsupportedOperationException(String.format(str, args));
    }
}
