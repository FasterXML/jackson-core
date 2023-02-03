package tools.jackson.core;

import java.io.InputStream;
import java.io.Reader;

import tools.jackson.core.tree.ArrayTreeNode;
import tools.jackson.core.tree.ObjectTreeNode;
import tools.jackson.core.type.ResolvedType;
import tools.jackson.core.type.TypeReference;

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

    // // // Configuration access

    public FormatSchema getSchema();

    public int getStreamReadFeatures(int defaults);
    public int getFormatReadFeatures(int defaults);

    public TokenStreamFactory tokenStreamFactory();

    public StreamReadConstraints streamReadConstraints();

    // // // Parser construction

    default JsonParser createParser(InputStream in) throws JacksonException {
        return tokenStreamFactory().createParser(this, in);
    }

    default JsonParser createParser(Reader r) throws JacksonException {
        return tokenStreamFactory().createParser(this, r);
    }

    default JsonParser createParser(String content) throws JacksonException {
        return tokenStreamFactory().createParser(this, content);
    }

    default JsonParser createParser(byte[] content) throws JacksonException {
        return tokenStreamFactory().createParser(this, content);
    }

    default JsonParser createParser(byte[] content, int offset, int length) throws JacksonException {
        return tokenStreamFactory().createParser(this, content, offset, length);
    }

    // // // Databinding callbacks, tree node creation

    /**
     * Method for construct Array nodes for Tree Model instances.
     *
     * @return Array node created
     */
    public ArrayTreeNode createArrayNode();

    /**
     * Method for construct Object nodes for Tree Model instances.
     *
     * @return Object node created
     */
    public ObjectTreeNode createObjectNode();

    // // // Databinding callbacks, tree deserialization

    public <T extends TreeNode> T readTree(JsonParser p) throws JacksonException;

    /**
     * Convenience method for traversing over given {@link TreeNode} by exposing
     * contents as a {@link JsonParser}.
     *<p>
     * NOTE! Returned parser has not been advanced to the first token; caller has to
     * do this.
     *
     * @param n Node to traverse over
     *
     * @return Parser that iterates over contents of given node
     */
    default JsonParser treeAsTokens(TreeNode n) {
        return n.traverse(this);
    }

    // // // Databinding callbacks, non-tree value deserialization

    public <T> T readValue(JsonParser p, Class<T> valueType) throws JacksonException;

    public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef) throws JacksonException;

    public <T> T readValue(JsonParser p, ResolvedType type) throws JacksonException;

    /**
     * Default no-op implementation.
     */
    public static class Base implements ObjectReadContext {
        protected static Base EMPTY_CONTEXT = new Base();

        // // // Config access methods

        @Override
        public FormatSchema getSchema() { return null; }

        @Override
        public StreamReadConstraints streamReadConstraints() {
            return StreamReadConstraints.defaults();
        }

        @Override
        public int getStreamReadFeatures(int defaults) {
            return defaults;
        }

        @Override
        public int getFormatReadFeatures(int defaults) {
            return defaults;
        }

        @Override
        public TokenStreamFactory tokenStreamFactory() {
            return _reportUnsupportedOperation();
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

        // // // Databind integration, trees

        @Override
        public <T extends TreeNode> T readTree(JsonParser p) {
            return _reportUnsupportedOperation();
        }

        // // // Databind integration, other values

        @Override
        public <T> T readValue(JsonParser p, Class<T> valueType) {
            return _reportUnsupportedOperation();
        }

        @Override
        public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef) {
            return _reportUnsupportedOperation();
        }

        @Override
        public <T> T readValue(JsonParser p, ResolvedType type) {
            return _reportUnsupportedOperation();
        }

        // // // Helper methods

        protected <T> T _reportUnsupportedOperation() {
            throw new UnsupportedOperationException("Operation not supported by `ObjectReadContext` of type "+getClass().getName());
        }
    }
}
