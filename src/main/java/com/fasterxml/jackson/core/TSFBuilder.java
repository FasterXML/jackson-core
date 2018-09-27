package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.InputDecorator;
import com.fasterxml.jackson.core.io.OutputDecorator;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

/**
 * Since 2.10, Builder class is offered for creating token stream factories
 * with difference configurations: with 3.x they will be fully immutable.
 *
 * @since 2.10
 */
public abstract class TSFBuilder<F extends JsonFactory,
    B extends TSFBuilder<F,B>>
{
    /*
    /**********************************************************************
    /* Constants
    /**********************************************************************
     */

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

    /*
    /**********************************************************************
    /* Configured features
    /**********************************************************************
     */
    
    /**
     * Set of {@link TokenStreamFactory.Feature}s enabled, as bitmask.
     */
    protected int _factoryFeatures;

    /**
     * Set of {@link JsonParser.Feature}s enabled, as bitmask.
     */
    protected int _parserFeatures;

    /**
     * Set of {@link JsonGenerator.Feature}s enabled, as bitmask.
     */
    protected int _generatorFeatures;

    /*
    /**********************************************************************
    /* Other configuration
    /**********************************************************************
     */

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
    
    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    protected TSFBuilder() {
        _factoryFeatures = DEFAULT_FACTORY_FEATURE_FLAGS;
        _parserFeatures = DEFAULT_PARSER_FEATURE_FLAGS;
        _generatorFeatures = DEFAULT_GENERATOR_FEATURE_FLAGS;
        _inputDecorator = null;
        _outputDecorator = null;
    }

    protected TSFBuilder(JsonFactory base)
    {
        this(base._factoryFeatures,
                base._parserFeatures, base._generatorFeatures);
    }

    protected TSFBuilder(int factoryFeatures,
            int parserFeatures, int generatorFeatures)
    {
        _factoryFeatures = factoryFeatures;
        _parserFeatures = parserFeatures;
        _generatorFeatures = generatorFeatures;
    }

    // // // Accessors

    public int factoryFeaturesMask() { return _factoryFeatures; }
    public int parserFeaturesMask() { return _parserFeatures; }
    public int generatorFeaturesMask() { return _generatorFeatures; }

    public InputDecorator inputDecorator() { return _inputDecorator; }
    public OutputDecorator outputDecorator() { return _outputDecorator; }

    // // // Factory features

    public B enable(JsonFactory.Feature f) {
        _factoryFeatures |= f.getMask();
        return _this();
    }

    public B disable(JsonFactory.Feature f) {
        _factoryFeatures &= ~f.getMask();
        return _this();
    }

    public B configure(JsonFactory.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    // // // Parser features

    public B enable(JsonParser.Feature f) {
        _parserFeatures |= f.getMask();
        return _this();
    }

    public B enable(JsonParser.Feature first, JsonParser.Feature... other) {
        _parserFeatures |= first.getMask();
        for (JsonParser.Feature f : other) {
            _parserFeatures |= f.getMask();
        }
        return _this();
    }

    public B disable(JsonParser.Feature f) {
        _parserFeatures &= ~f.getMask();
        return _this();
    }

    public B disable(JsonParser.Feature first, JsonParser.Feature... other) {
        _parserFeatures &= ~first.getMask();
        for (JsonParser.Feature f : other) {
            _parserFeatures &= ~f.getMask();
        }
        return _this();
    }

    public B configure(JsonParser.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    // // // Generator features

    public B enable(JsonGenerator.Feature f) {
        _generatorFeatures |= f.getMask();
        return _this();
    }

    public B enable(JsonGenerator.Feature first, JsonGenerator.Feature... other) {
        _generatorFeatures |= first.getMask();
        for (JsonGenerator.Feature f : other) {
            _generatorFeatures |= f.getMask();
        }
        return _this();
    }

    public B disable(JsonGenerator.Feature f) {
        _generatorFeatures &= ~f.getMask();
        return _this();
    }
    
    public B disable(JsonGenerator.Feature first, JsonGenerator.Feature... other) {
        _generatorFeatures &= ~first.getMask();
        for (JsonGenerator.Feature f : other) {
            _generatorFeatures &= ~f.getMask();
        }
        return _this();
    }

    public B configure(JsonGenerator.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    /* 26-Jun-2018, tatu: This should not be needed here, but due to 2.x limitations,
     *   we do need to include it or require casting. So let's select lesser evil(s)
     */

    // // // JSON-specific, reads

    public B enable(JsonReadFeature f) {
        return _failNonJSON(f);
    }

    public B enable(JsonReadFeature first, JsonReadFeature... other) {
        return _failNonJSON(first);
    }

    public B disable(JsonReadFeature f) {
        return _failNonJSON(f);
    }

    public B disable(JsonReadFeature first, JsonReadFeature... other) {
        return _failNonJSON(first);
    }

    public B configure(JsonReadFeature f, boolean state) {
        return _failNonJSON(f);
    }
    
    private B _failNonJSON(Object feature) {
        throw new IllegalArgumentException("Feature "+feature.getClass().getName()
                +"#"+feature.toString()+" not supported for non-JSON backend");
    }

    // // // JSON-specific, writes

    public B enable(JsonWriteFeature f) {
        return _failNonJSON(f);
    }

    public B enable(JsonWriteFeature first, JsonWriteFeature... other) {
        return _failNonJSON(first);
    }

    public B disable(JsonWriteFeature f) {
        return _failNonJSON(f);
    }

    public B disable(JsonWriteFeature first, JsonWriteFeature... other) {
        return _failNonJSON(first);
    }

    public B configure(JsonWriteFeature f, boolean state) {
        return _failNonJSON(f);
    }

    // // // Other configuration

    public B inputDecorator(InputDecorator dec) {
        _inputDecorator = dec;
        return _this();
    }

    public B outputDecorator(OutputDecorator dec) {
        _outputDecorator = dec;
        return _this();
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
