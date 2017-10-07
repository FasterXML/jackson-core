package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.tree.ArrayTreeNode;
import com.fasterxml.jackson.core.tree.ObjectTreeNode;

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

        // // // Databind integration

        @Override
        public ObjectTreeNode createObjectNode() {
            return _reportUnsupportedOperation();
        }

        @Override
        public ArrayTreeNode createArrayNode() {
            return _reportUnsupportedOperation();
        }

        protected <T> T _reportUnsupportedOperation() {
            throw new UnsupportedOperationException("Operation not supported by `ObjectReadContext` of type "+getClass().getName());
        }
    }
}
