package tools.jackson.core.util;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.TokenStreamFactory;

/**
 * Simple interface to allow adding decorators around {@link JsonGenerator}s.
 *
 * @since 2.16
 */
public interface JsonGeneratorDecorator
{
    /**
     * Allow to decorate {@link JsonGenerator} instances returned by {@link TokenStreamFactory}.
     * 
     * @since 2.16
     * @param factory The factory which was used to build the original generator
     * @param generator The generator to decorate. This might already be a decorated instance, not the original.
     * @return decorated generator
     */
    JsonGenerator decorate(TokenStreamFactory factory, JsonGenerator generator);
}
