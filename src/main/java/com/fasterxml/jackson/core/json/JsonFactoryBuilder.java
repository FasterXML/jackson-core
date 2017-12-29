package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

/**
 * {@link com.fasterxml.jackson.core.TokenStreamFactory.TSFBuilder}
 * implementation for constructing {@link JsonFactory}
 * instances.
 *
 * @since 3.0
 */
public class JsonFactoryBuilder extends DecorableTSFBuilder<JsonFactory, JsonFactoryBuilder>
{
    public JsonFactoryBuilder() {
        super();
    }

    public JsonFactoryBuilder(JsonFactory base) {
        super(base);
    }

    @Override
    public JsonFactory build() {
        // 28-Dec-2017, tatu: No special settings beyond base class ones, so:
        return new JsonFactory(this);
    }

}
