package com.fasterxml.jackson.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.tree.ArrayTreeNode;
import com.fasterxml.jackson.core.tree.ObjectTreeNode;

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

    // // // Configuration access

    public FormatSchema getSchema();

    public CharacterEscapes getCharacterEscapes();
    public PrettyPrinter getPrettyPrinter();
    public SerializableString getRootValueSeparator(SerializableString defaultSeparator);

    public int getStreamWriteFeatures(int defaults);
    public int getFormatWriteFeatures(int defaults);

    public TokenStreamFactory getGeneratorFactory();

    // // // Generator construction: limited to targets that make sense for embedding
    // // // purposes (like "JSON in JSON" etc)

    default JsonGenerator createGenerator(OutputStream out) throws IOException {
        return getGeneratorFactory().createGenerator(this, out);
    }

    default JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        return getGeneratorFactory().createGenerator(this, out, enc);
    }

    default JsonGenerator createGenerator(Writer w) throws IOException {
        return getGeneratorFactory().createGenerator(this, w);
    }

    // // // Databinding callbacks, tree node creation

    /**
     * Method for construct Array nodes for Tree Model instances.
     */
    public ArrayTreeNode createArrayNode();
    
    /**
     * Method for construct Object nodes for Tree Model instances.
     */
    public ObjectTreeNode createObjectNode();
    
    // // // Databinding callbacks, value serialization

    /**
     * Method that may be called to serialize given value, using specified
     * token stream generator.
     */
    public void writeValue(JsonGenerator g, Object value) throws IOException;

    public void writeTree(JsonGenerator g, TreeNode value) throws IOException;

    /**
     * Default no-op implementation.
     */
    public static class Base implements ObjectWriteContext {
        protected static Base EMPTY_CONTEXT = new Base();

        // // // Config access methods
        
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
        public int getStreamWriteFeatures(int defaults) {
            return defaults;
        }

        @Override
        public TokenStreamFactory getGeneratorFactory() {
            return _reportUnsupportedOperation();
        }
        
        @Override
        public int getFormatWriteFeatures(int defaults) {
            return defaults;
        }

        // // // Databind integration

        @Override
        public ObjectTreeNode createObjectNode() {
            return _reportUnsupportedOperation();
        }

        @Override
        public ArrayTreeNode createArrayNode() {
            return _reportUnsupportedOperation();
        }
        
        @Override
        public void writeValue(JsonGenerator g, Object value) throws IOException {
            _reportUnsupportedOperation();
        }

        @Override
        public void writeTree(JsonGenerator g, TreeNode value) throws IOException {
            _reportUnsupportedOperation();
        }
        
        protected <T> T _reportUnsupportedOperation() {
            throw new UnsupportedOperationException("Operation not supported by `ObjectWriteContext` of type "+getClass().getName());
        }
    }
}
