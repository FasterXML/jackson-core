package tools.jackson.core;

import java.io.OutputStream;
import java.io.Writer;

import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.exc.WrappedIOException;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.tree.ArrayTreeNode;
import tools.jackson.core.tree.ObjectTreeNode;

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

    /**
     * Accessor for getting {@link PrettyPrinter} instance to use for a new generator.
     * Note that this MUST BE a thread-safe instance: that is, if the pretty printer
     * implementation is stateful,
     * a new unshared instance needs to be returned -- caller will NOT try to make
     * a copy of {@link tools.jackson.core.util.Instantiatable} printers, context
     * must do that.
     *
     * @return {@link PrettyPrinter} instance to use for a single serialization operation
     */
    public PrettyPrinter getPrettyPrinter();

    /**
     * Accessor similar to {@link #getPrettyPrinter()} but which only indicates whether
     * a non-{@code null} instance would be constructed if requested, or not.
     * This is useful for backends that have custom pretty-printing instead of relying on
     * Jackson standard mechanism.
     *
     * @return True if {@link #getPrettyPrinter()} would return non-{@code null}; false otherwise.
     */
    public boolean hasPrettyPrinter();

    public SerializableString getRootValueSeparator(SerializableString defaultSeparator);

    public int getStreamWriteFeatures(int defaults);
    public int getFormatWriteFeatures(int defaults);

    public TokenStreamFactory tokenStreamFactory();

    // // // Generator construction: limited to targets that make sense for embedding
    // // // purposes (like "JSON in JSON" etc)

    default JsonGenerator createGenerator(OutputStream out) throws JacksonException {
        return tokenStreamFactory().createGenerator(this, out);
    }

    default JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws JacksonException {
        return tokenStreamFactory().createGenerator(this, out, enc);
    }

    default JsonGenerator createGenerator(Writer w) throws JacksonException {
        return tokenStreamFactory().createGenerator(this, w);
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

    // // // Databinding callbacks, value serialization

    /**
     * Method that may be called to serialize given value, using specified
     * token stream generator.
     *
     * @param g Generator to use for serialization
     * @param value Java value to be serialized
     *
     * @throws WrappedIOException for low-level write problems,
     * @throws StreamWriteException for encoding problems
     * @throws JacksonException (various subtypes) for databinding problems
     */
    public void writeValue(JsonGenerator g, Object value) throws JacksonException;

    public void writeTree(JsonGenerator g, TreeNode value) throws JacksonException;

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
        public boolean hasPrettyPrinter() {
            return getPrettyPrinter() != null;
        }

        @Override
        public SerializableString getRootValueSeparator(SerializableString defaultSeparator) {
            return defaultSeparator;
        }

        @Override
        public int getStreamWriteFeatures(int defaults) {
            return defaults;
        }

        @Override
        public TokenStreamFactory tokenStreamFactory() {
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
        public void writeValue(JsonGenerator g, Object value) {
            _reportUnsupportedOperation();
        }

        @Override
        public void writeTree(JsonGenerator g, TreeNode value) {
            _reportUnsupportedOperation();
        }

        protected <T> T _reportUnsupportedOperation() {
            throw new UnsupportedOperationException("Operation not supported by `ObjectWriteContext` of type "+getClass().getName());
        }
    }
}
