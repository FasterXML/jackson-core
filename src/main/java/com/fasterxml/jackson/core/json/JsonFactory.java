/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package com.fasterxml.jackson.core.json;

import java.io.*;
import java.util.List;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.TextualTSFactory;
import com.fasterxml.jackson.core.io.*;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import com.fasterxml.jackson.core.sym.BinaryNameMatcher;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.core.sym.FieldNameMatcher;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Named;

/**
 * JSON-backed {@link TokenStreamFactory} implementation that will create
 * token readers ("parsers") and writers ("generators") for handling
 * JSON-encoded content.
 *<p>
 * Note that this class used to reside at main <code>com.fasterxml.jackson.core</code>
 * in 2.x, but moved here to denote its changed role as implementation, not base
 * class for factories.
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
    protected final transient CharsToNameCanonicalizer _rootCharSymbols = CharsToNameCanonicalizer.createRoot();

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
        super(DEFAULT_JSON_PARSER_FEATURE_FLAGS, DEFAULT_JSON_GENERATOR_FEATURE_FLAGS);
        _rootValueSeparator = DEFAULT_ROOT_VALUE_SEPARATOR;
        _characterEscapes = null;
    }

    /**
     * Copy constructor.
     */
    protected JsonFactory(JsonFactory src)
    {
        super(src);
        _rootValueSeparator = src._rootValueSeparator;
        _characterEscapes = src._characterEscapes;
        _inputDecorator = src._inputDecorator;
        _outputDecorator = src._outputDecorator;
    }

    /**
     * Constructors used by {@link JsonFactoryBuilder} for instantiation.
     *
     * @since 3.0
     */
    protected JsonFactory(JsonFactoryBuilder b)
    {
        super(b);
        _rootValueSeparator = b.rootValueSeparator();
        _characterEscapes = b.characterEscapes();
    }

    @Override
    public JsonFactoryBuilder rebuild() {
        return new JsonFactoryBuilder(this);
    }

    /**
     * Main factory method to use for constructing {@link JsonFactory} instances with
     * different configuration.
     */
    public static JsonFactoryBuilder builder() {
        return new JsonFactoryBuilder();
    }

    /**
     * Method for constructing a new {@link JsonFactory} that has
     * the same settings as this instance, but is otherwise
     * independent (i.e. nothing is actually shared, symbol tables
     * are separate).
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
     * through constructors etc.
     * Also: must be overridden by sub-classes as well.
     */
    protected Object readResolve() {
        return new JsonFactory(this);
    }

    /*
    /**********************************************************************
    /* Capability introspection
    /**********************************************************************
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
     */
    public final boolean isEnabled(JsonReadFeature f) {
        return (_formatReadFeatures & f.getMask()) != 0;
    }

    /**
     * Check whether specified generator feature is enabled.
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
    public JsonParser createNonBlockingByteArrayParser(ObjectReadContext readCtxt) throws IOException
    {
        IOContext ioCtxt = _createNonBlockingContext(null);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChild(_factoryFeatures);
        return new NonBlockingJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                can);
    }

    protected IOContext _createNonBlockingContext(Object srcRef) {
        // [jackson-core#476]: disable buffer recycling for 2.9 to avoid concurrency issues;
        // easiest done by just constructing private "recycler":
        BufferRecycler recycler = new BufferRecycler();
        return new IOContext(recycler, srcRef, false);
    }    
    
    /*
    /**********************************************************************
    /* Factory methods used by factory for creating parser instances
    /**********************************************************************
     */

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            InputStream in) throws IOException {
        return new ByteSourceJsonBootstrapper(ioCtxt, in)
                .constructParser(readCtxt,
                        readCtxt.getStreamReadFeatures(_streamReadFeatures),
                        readCtxt.getFormatReadFeatures(_formatReadFeatures),
                        _byteSymbolCanonicalizer, _rootCharSymbols, _factoryFeatures);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            Reader r) throws IOException {
        return new ReaderBasedJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                r,
                _rootCharSymbols.makeChild(_factoryFeatures));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            char[] data, int offset, int len,
            boolean recyclable) throws IOException {
        return new ReaderBasedJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                null,
                _rootCharSymbols.makeChild(_factoryFeatures),
                data, offset, offset+len, recyclable);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            byte[] data, int offset, int len) throws IOException
    {
        return new ByteSourceJsonBootstrapper(ioCtxt, data, offset, len)
                .constructParser(readCtxt,
                        readCtxt.getStreamReadFeatures(_streamReadFeatures),
                        readCtxt.getFormatReadFeatures(_formatReadFeatures),
                       _byteSymbolCanonicalizer, _rootCharSymbols, _factoryFeatures);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            DataInput input) throws IOException
    {
        // Also: while we can't do full bootstrapping (due to read-ahead limitations), should
        // at least handle possible UTF-8 BOM
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChild(_factoryFeatures);
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
            IOContext ioCtxt, Writer out) throws IOException
    {
        SerializableString rootSep = writeCtxt.getRootValueSeparator(_rootValueSeparator);
        CharacterEscapes charEsc = writeCtxt.getCharacterEscapes();
        if (charEsc == null) {
            charEsc = _characterEscapes;
        }
        // NOTE: JSON generator does not use schema; has no format-specific features
        return new WriterBasedJsonGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                writeCtxt.getFormatWriteFeatures(_formatWriteFeatures),
                out,
                rootSep, charEsc, writeCtxt.getPrettyPrinter());
    }

    @Override
    protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, OutputStream out) throws IOException
    {
        SerializableString rootSep = writeCtxt.getRootValueSeparator(_rootValueSeparator);
        CharacterEscapes charEsc = writeCtxt.getCharacterEscapes();
        if (charEsc == null) {
            charEsc = _characterEscapes;
        }
        // NOTE: JSON generator does not use schema; has no format-specific features

        return new UTF8JsonGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                writeCtxt.getFormatWriteFeatures(_formatWriteFeatures),
                out,
                rootSep, charEsc, writeCtxt.getPrettyPrinter());
    }

    /*
    /**********************************************************************
    /* Other factory methods
    /**********************************************************************
     */

    @Override
    public FieldNameMatcher constructFieldNameMatcher(List<Named> matches, boolean alreadyInterned) {
        return BinaryNameMatcher.constructFrom(matches, alreadyInterned);
    }

    @Override
    public FieldNameMatcher constructCIFieldNameMatcher(List<Named> matches, boolean alreadyInterned) {
        return BinaryNameMatcher.constructCaseInsensitive(matches, alreadyInterned);
    }
}
