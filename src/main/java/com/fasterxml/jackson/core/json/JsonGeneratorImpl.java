package com.fasterxml.jackson.core.json;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.VersionUtil;

/**
 * Intermediate base class shared by JSON-backed generators
 * like {@link UTF8JsonGenerator} and {@link WriterBasedJsonGenerator}.
 * 
 * @since 2.1
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
    /* Configuration, basic I/O
    /**********************************************************
     */

    final protected IOContext _ioContext;

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
     */
    protected CharacterEscapes _characterEscapes;
    
    /*
    /**********************************************************
    /* Configuration, other
    /**********************************************************
     */

    /**
     * Separator to use, if any, between root-level values.
     * 
     * @since 2.1
     */
    protected SerializableString _rootValueSeparator
        = DefaultPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR;

    /**
     * Flag that is set if quoting is not to be added around
     * JSON Object property names.
     *
     * @since 2.7
     */
    protected boolean _cfgUnqNames;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public JsonGeneratorImpl(IOContext ctxt, int features, ObjectCodec codec)
    {
        super(features, codec);
        _ioContext = ctxt;
        if (Feature.ESCAPE_NON_ASCII.enabledIn(features)) {
            // inlined `setHighestNonEscapedChar()`
            _maximumNonEscapedChar = 127;
        }
        _cfgUnqNames = !Feature.QUOTE_FIELD_NAMES.enabledIn(features);
    }

    /*
    /**********************************************************
    /* Overridden configuration methods
    /**********************************************************
     */

    @Override
    public JsonGenerator enable(Feature f) {
        super.enable(f);
        if (f == Feature.QUOTE_FIELD_NAMES) {
            _cfgUnqNames = false;
        }
        return this;
    }

    @Override
    public JsonGenerator disable(Feature f) {
        super.disable(f);
        if (f == Feature.QUOTE_FIELD_NAMES) {
            _cfgUnqNames = true;
        }
        return this;
    }

    @Override
    protected void _checkStdFeatureChanges(int newFeatureFlags, int changedFeatures) {
        super._checkStdFeatureChanges(newFeatureFlags, changedFeatures);
        _cfgUnqNames = !Feature.QUOTE_FIELD_NAMES.enabledIn(newFeatureFlags);
    }

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
    public JsonGenerator setRootValueSeparator(SerializableString sep) {
        _rootValueSeparator = sep;
        return this;
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
}
