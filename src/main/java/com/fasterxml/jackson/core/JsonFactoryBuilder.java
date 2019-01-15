package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

/**
 * {@link com.fasterxml.jackson.core.TSFBuilder}
 * implementation for constructing vanilla {@link JsonFactory}
 * instances for reading/writing JSON encoded content.
 *
 * @since 2.10
 */
public class JsonFactoryBuilder extends TSFBuilder<JsonFactory, JsonFactoryBuilder>
{
    protected CharacterEscapes _characterEscapes;

    protected SerializableString _rootValueSeparator;

    protected int _maximumNonEscapedChar;

    public JsonFactoryBuilder() {
        super();
        _rootValueSeparator = JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR;
        _maximumNonEscapedChar = 0;
    }

    public JsonFactoryBuilder(JsonFactory base) {
        super(base);
        _characterEscapes = base.getCharacterEscapes();
        _rootValueSeparator = base._rootValueSeparator;
        _maximumNonEscapedChar = base._maximumNonEscapedChar;
    }

    /*
    /**********************************************************
    /* Mutators
    /**********************************************************
     */

    // // // JSON-parsing features

    @Override
    public JsonFactoryBuilder enable(JsonReadFeature f) {
        _legacyEnable(f.mappedFeature());
        return this;
    }

    @Override
    public JsonFactoryBuilder enable(JsonReadFeature first, JsonReadFeature... other) {
        _legacyEnable(first.mappedFeature());
        enable(first);
        for (JsonReadFeature f : other) {
            _legacyEnable(f.mappedFeature());
        }
        return this;
    }

    @Override
    public JsonFactoryBuilder disable(JsonReadFeature f) {
        _legacyDisable(f.mappedFeature());
        return this;
    }

    @Override
    public JsonFactoryBuilder disable(JsonReadFeature first, JsonReadFeature... other) {
        _legacyDisable(first.mappedFeature());
        for (JsonReadFeature f : other) {
            _legacyEnable(f.mappedFeature());
        }
        return this;
    }

    @Override
    public JsonFactoryBuilder configure(JsonReadFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    // // // JSON-generating features

    @Override
    public JsonFactoryBuilder enable(JsonWriteFeature f) {
        JsonGenerator.Feature old = f.mappedFeature();
        if (old != null) {
            _legacyEnable(old);
        }
        return this;
    }

    @Override
    public JsonFactoryBuilder enable(JsonWriteFeature first, JsonWriteFeature... other) {
        _legacyEnable(first.mappedFeature());
        for (JsonWriteFeature f : other) {
            _legacyEnable(f.mappedFeature());
        }
        return this;
    }

    @Override
    public JsonFactoryBuilder disable(JsonWriteFeature f) {
        _legacyDisable(f.mappedFeature());
        return this;
    }

    @Override
    public JsonFactoryBuilder disable(JsonWriteFeature first, JsonWriteFeature... other) {
        _legacyDisable(first.mappedFeature());
        for (JsonWriteFeature f : other) {
            _legacyDisable(f.mappedFeature());
        }
        return this;
    }

    @Override
    public JsonFactoryBuilder configure(JsonWriteFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    // // // JSON-specific helper objects, settings
    
    /**
     * Method for defining custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     */
    public JsonFactoryBuilder characterEscapes(CharacterEscapes esc) {
        _characterEscapes = esc;
        return this;
    }

    /**
     * Method that allows overriding String used for separating root-level
     * JSON values (default is single space character)
     * 
     * @param sep Separator to use, if any; null means that no separator is
     *   automatically added
     */
    public JsonFactoryBuilder rootValueSeparator(String sep) {
        _rootValueSeparator = (sep == null) ? null : new SerializedString(sep);
        return this;
    }

    /**
     * Method that allows overriding String used for separating root-level
     * JSON values (default is single space character)
     * 
     * @param sep Separator to use, if any; null means that no separator is
     *   automatically added
     */
    public JsonFactoryBuilder rootValueSeparator(SerializableString sep) {
        _rootValueSeparator = sep;
        return this;
    }

    /**
     * Method that allows specifying threshold beyond which all characters are
     * automatically escaped (without checking possible custom escaping settings
     * a la {@link #characterEscapes}: for example, to force escaping of all non-ASCII
     * characters (set to 127), or all non-Latin-1 character (set to 255).
     * Default setting is "disabled", specified by passing value of {@code 0} (or
     * negative numbers).
     *<p>
     * NOTE! Lowest value (aside from marker 0) is 127: for ASCII range, other checks apply
     * and this threshold is ignored.
     * 
     * @param maxNonEscaped Highest character code that is NOT automatically escaped; if
     *    positive value above 0, or 0 to indicate that no automatic escaping is applied
     *    beside from what JSON specification requires (and possible custom escape settings).
     *    Values between 1 and 127 are all taken to behave as if 127 is specified: that is,
     *    no automatic escaping is applied in ASCII range.
     */
    public JsonFactoryBuilder highestNonEscapedChar(int maxNonEscaped) {
        _maximumNonEscapedChar = (maxNonEscaped <= 0) ? 0 : Math.max(127, maxNonEscaped);
        return this;
    }

    // // // Accessors for JSON-specific settings
    
    public CharacterEscapes characterEscapes() { return _characterEscapes; }
    public SerializableString rootValueSeparator() { return _rootValueSeparator; }

    public int highestNonEscapedChar() { return _maximumNonEscapedChar; }

    @Override
    public JsonFactory build() {
        // 28-Dec-2017, tatu: No special settings beyond base class ones, so:
        return new JsonFactory(this);
    }
}
