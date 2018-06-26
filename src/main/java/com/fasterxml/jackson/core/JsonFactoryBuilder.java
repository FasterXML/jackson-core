package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonReadFeature;

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

    protected boolean _handleBSONWrappers;

    public JsonFactoryBuilder() {
        super();
        _rootValueSeparator = JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR;
        _handleBSONWrappers = JsonReadFeature.HANDLE_BSON_WRAPPERS.enabledByDefault();
    }

    public JsonFactoryBuilder(JsonFactory base) {
        super(base);
        _characterEscapes = base.getCharacterEscapes();
        _rootValueSeparator = base._rootValueSeparator;
//        _handleBSONWrappers = base._handleBSONWrappers;
    }

    /*
    /**********************************************************
    /* Mutators
    /**********************************************************
     */

    // // // JSON-parsing features

    public JsonFactoryBuilder enable(JsonReadFeature f) {
        JsonParser.Feature old = f.mappedFeature();
        if (old != null) {
            enable(old);
        } else if (f == JsonReadFeature.HANDLE_BSON_WRAPPERS) {
            _handleBSONWrappers = true;
        }
        return _this();
    }

    public JsonFactoryBuilder enable(JsonReadFeature first, JsonReadFeature... other) {
        enable(first);
        for (JsonReadFeature f : other) {
            enable(f);
        }
        return _this();
    }

    public JsonFactoryBuilder disable(JsonReadFeature f) {
        JsonParser.Feature old = f.mappedFeature();
        if (old != null) {
            disable(old);
        } else if (f == JsonReadFeature.HANDLE_BSON_WRAPPERS) {
            _handleBSONWrappers = false;
        }
        return _this();
    }

    public JsonFactoryBuilder disable(JsonReadFeature first, JsonReadFeature... other) {
        disable(first);
        for (JsonReadFeature f : other) {
            disable(f);
        }
        return _this();
    }

    public JsonFactoryBuilder configure(JsonReadFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

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

    public CharacterEscapes characterEscapes() { return _characterEscapes; }
    public SerializableString rootValueSeparator() { return _rootValueSeparator; }

    @Override
    public JsonFactory build() {
        // 28-Dec-2017, tatu: No special settings beyond base class ones, so:
        return new JsonFactory(this);
    }
}
