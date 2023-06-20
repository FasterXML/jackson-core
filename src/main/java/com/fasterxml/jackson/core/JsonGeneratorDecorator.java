package com.fasterxml.jackson.core;

public interface JsonGeneratorDecorator {
    /**
     * Allow to decorate {@link JsonGenerator} instances returned by {@link JsonFactory}.
     * 
     * @since 2.16
     * @param factory The factory which was used to build the original generator
     * @param generator The generator to decorate. This might already be a decorated instance, not the original.
     * @return decorated generator
     */
    JsonGenerator decorate(JsonFactory factory, JsonGenerator generator);
}
