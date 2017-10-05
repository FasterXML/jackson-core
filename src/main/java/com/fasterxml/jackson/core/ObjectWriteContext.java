package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.CharacterEscapes;

/**
 * Defines API for accessing configuration and state exposed by
 * higher level databind
 * functionality during write (Object to token stream serialization) process.
 * Access is mostly needed during construction of
 * {@link JsonGenerator} instances by {@link TokenStreamFactory}.
 *
 * @since 3.0
 */
public interface ObjectWriteContext
{
    public static ObjectWriteContext empty() {
        return Base.EMPTY_CONTEXT;
    }

    public FormatSchema getSchema();

    public CharacterEscapes getCharacterEscapes();
    public PrettyPrinter getPrettyPrinter();
    public SerializableString getRootValueSeparator(SerializableString defaultSeparator);

    public int getGeneratorFeatures(int defaults);
    public int getFormatWriteFeatures(int defaults);

    /**
     * Default no-op implementation.
     */
    public static class Base implements ObjectWriteContext {
        protected static ObjectWriteContext EMPTY_CONTEXT = new Base();

        @Override
        public FormatSchema getSchema() { return null; }

        @Override
        public CharacterEscapes getCharacterEscapes() { return null; }

        @Override
        public PrettyPrinter getPrettyPrinter() { return null; }

        @Override
        public SerializableString getRootValueSeparator(SerializableString defaultSeparator) {
            return defaultSeparator;
        }

        @Override
        public int getGeneratorFeatures(int defaults) {
            return defaults;
        }

        @Override
        public int getFormatWriteFeatures(int defaults) {
            return defaults;
        }
    }
}
