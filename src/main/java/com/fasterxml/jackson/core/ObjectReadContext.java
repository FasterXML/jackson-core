package com.fasterxml.jackson.core;

import java.io.IOException;

import com.fasterxml.jackson.core.tree.ArrayTreeNode;
import com.fasterxml.jackson.core.tree.ObjectTreeNode;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Defines API for accessing configuration and state exposed by
 * higher level databind
 * functionality during read (token stream  to Object deserialization) process.
 * Access is mostly needed during construction of
 * {@link JsonParser} instances by {@link TokenStreamFactory}.
 *
 * @since 3.0
 */
public interface ObjectReadContext
{
    public static ObjectReadContext empty() {
        return Base.EMPTY_CONTEXT;
    }

    // // // Configuration

    public FormatSchema getSchema();

    public int getParserFeatures(int defaults);
    public int getFormatReadFeatures(int defaults);

    // // // Databinding callbacks, tree node creation

    /**
     * Method for construct Array nodes for Tree Model instances.
     */
    public ArrayTreeNode createArrayNode();
    
    /**
     * Method for construct Object nodes for Tree Model instances.
     */
    public ObjectTreeNode createObjectNode();
    
    // // // Databinding callbacks, value deserialization

    public <T> T readValue(JsonParser p, Class<T> valueType) throws IOException;

    public <T> T readValue(JsonParser p, TypeReference<?> valueTypeRef) throws IOException;

    public <T> T readValue(JsonParser p, ResolvedType type) throws IOException;

    public <T extends TreeNode> T readTree(JsonParser p) throws IOException;

    /**
     * Default no-op implementation.
     */
    public static class Base implements ObjectReadContext {
        protected static Base EMPTY_CONTEXT = new Base();

        // // // Config access methods
        
        @Override
        public FormatSchema getSchema() { return null; }

        @Override
        public int getParserFeatures(int defaults) {
            return defaults;
        }

        @Override
        public int getFormatReadFeatures(int defaults) {
            return defaults;
        }

        // // // Databind, trees

        @Override
        public ObjectTreeNode createObjectNode() {
            return _reportUnsupportedOperation();
        }

        @Override
        public ArrayTreeNode createArrayNode() {
            return _reportUnsupportedOperation();
        }

        // // // Databind integration

        @Override
        public <T> T readValue(JsonParser p, Class<T> valueType) throws IOException {
            return _reportUnsupportedOperation();
        }

        @Override
        public <T> T readValue(JsonParser p, TypeReference<?> valueTypeRef) throws IOException {
            return _reportUnsupportedOperation();
        }

        @Override
        public <T> T readValue(JsonParser p, ResolvedType type) throws IOException {
            return _reportUnsupportedOperation();
        }

        @Override
        public <T extends TreeNode> T readTree(JsonParser p) throws IOException {
            return _reportUnsupportedOperation();
        }

        // // // Helper methods
        
        protected <T> T _reportUnsupportedOperation() {
            throw new UnsupportedOperationException("Operation not supported by `ObjectReadContext` of type "+getClass().getName());
        }
    }
}
