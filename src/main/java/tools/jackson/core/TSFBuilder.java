package tools.jackson.core;

/**
 * Since factory instances are immutable, a Builder class is needed for creating
 * configurations for differently configured factory instances.
 */
public abstract class TSFBuilder<F extends TokenStreamFactory,
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

    /**
     * StreamReadConstraints to use.
     */
    protected StreamReadConstraints _streamReadConstraints;

    // // // Construction

    protected TSFBuilder(StreamReadConstraints src,
            int formatReadF, int formatWriteF) {
        _streamReadConstraints = src;
        _factoryFeatures = TokenStreamFactory.DEFAULT_FACTORY_FEATURE_FLAGS;
        _streamReadFeatures = TokenStreamFactory.DEFAULT_STREAM_READ_FEATURE_FLAGS;
        _streamWriteFeatures = TokenStreamFactory.DEFAULT_STREAM_WRITE_FEATURE_FLAGS;
        _formatReadFeatures = formatReadF;
        _formatWriteFeatures = formatWriteF;
    }

    protected TSFBuilder(TokenStreamFactory base)
    {
        this(base._streamReadConstraints,
                base._factoryFeatures,
                base._streamReadFeatures, base._streamWriteFeatures,
                base._formatReadFeatures, base._formatWriteFeatures);
        _streamReadConstraints = base._streamReadConstraints;
    }

    protected TSFBuilder(StreamReadConstraints src,
            int factoryFeatures,
            int streamReadFeatures, int streamWriteFeatures,
            int formatReadFeatures, int formatWriteFeatures)
    {
        _streamReadConstraints = src;
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

    // // // Other configuration

    /**
     * Sets the constraints for streaming reads.
     *
     * @param streamReadConstraints constraints for streaming reads
     * @return this builder
     */
    public B streamReadConstraints(StreamReadConstraints streamReadConstraints) {
        _streamReadConstraints = streamReadConstraints;
        return _this();
    }

    // // // Other methods

    /**
     * Method for constructing actual {@link TokenStreamFactory} instance, given
     * configuration.
     *
     * @return {@link TokenStreamFactory} build using builder configuration settings
     */
    public abstract F build();

    // silly convenience cast method we need
    @SuppressWarnings("unchecked")
    protected final B _this() { return (B) this; }
}
