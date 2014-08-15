/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Abstract class that defines the interface that {@link JsonParser} and
 * {@link JsonGenerator} use to serialize and deserialize regular
 * Java objects (POJOs aka Beans).
 *<p>
 * The standard implementation of this class is
 * <code>com.fasterxml.jackson.databind.ObjectMapper</code>,
 * defined in the "jackson-databind".
 */
public abstract class ObjectCodec
    extends TreeCodec // since 2.3
    implements Versioned // since 2.3
{
    protected ObjectCodec() { }

    // Since 2.3: need baseline implementation to avoid backwards compatibility
    @Override
    public Version version() { return Version.unknownVersion(); }
    
    /*
    /**********************************************************
    /* API for de-serialization (JSON-to-Object)
    /**********************************************************
     */

    /**
     * Method to deserialize JSON content into a non-container
     * type (it can be an array type, however): typically a bean, array
     * or a wrapper type (like {@link java.lang.Boolean}).
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     */
    public abstract <T> T readValue(JsonParser jp, Class<T> valueType)
        throws IOException, JsonProcessingException;

    /**
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token" 
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     */
    public abstract <T> T readValue(JsonParser jp, TypeReference<?> valueTypeRef)
        throws IOException, JsonProcessingException;

    /**
     * Method to deserialize JSON content into a POJO, type specified
     * with fully resolved type object (so it can be a generic type,
     * including containers like {@link java.util.Collection} and
     * {@link java.util.Map}).
     */
    public abstract <T> T readValue(JsonParser jp, ResolvedType valueType)
        throws IOException, JsonProcessingException;

    /**
     * Method for reading sequence of Objects from parser stream,
     * all with same specified value type.
     */
    public abstract <T> Iterator<T> readValues(JsonParser jp, Class<T> valueType)
        throws IOException, JsonProcessingException;

    /**
     * Method for reading sequence of Objects from parser stream,
     * all with same specified value type.
     */
    public abstract <T> Iterator<T> readValues(JsonParser jp, TypeReference<?> valueTypeRef)
        throws IOException, JsonProcessingException;
    
    /**
     * Method for reading sequence of Objects from parser stream,
     * all with same specified value type.
     */
    public abstract <T> Iterator<T> readValues(JsonParser jp, ResolvedType valueType)
        throws IOException, JsonProcessingException;
    
    /*
    /**********************************************************
    /* API for serialization (Object-to-JSON)
    /**********************************************************
     */

    /**
     * Method to serialize given Java Object, using generator
     * provided.
     */
    public abstract void writeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonProcessingException;

    /*
    /**********************************************************
    /* TreeCodec pass-through methods
    /**********************************************************
     */

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link TreeNode} instances. Returns
     * root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container). Empty or whitespace
     * documents return null.
     *
     * @return next tree from jp, or null if empty.
     */
    @Override
    public abstract <T extends TreeNode> T readTree(JsonParser jp)
        throws IOException, JsonProcessingException;
    
    @Override
    public abstract void writeTree(JsonGenerator jg, TreeNode tree)
        throws IOException, JsonProcessingException;
    
    /**
     * Method for construct root level Object nodes
     * for Tree Model instances.
     */
    @Override
    public abstract TreeNode createObjectNode();

    /**
     * Method for construct root level Array nodes
     * for Tree Model instances.
     */
    @Override
    public abstract TreeNode createArrayNode();

    /**
     * Method for constructing a {@link JsonParser} for reading
     * contents of a JSON tree, as if it was external serialized
     * JSON content.
     */
    @Override
    public abstract JsonParser treeAsTokens(TreeNode n);

    /*
    /**********************************************************
    /* Extended tree conversions beyond TreeCodec
    /**********************************************************
     */
    
    /**
     * Convenience method for converting given JSON tree into instance of specified
     * value type. This is equivalent to first constructing a {@link JsonParser} to
     * iterate over contents of the tree, and using that parser for data binding.
     */
    public abstract <T> T treeToValue(TreeNode n, Class<T> valueType)
        throws JsonProcessingException;

    /*
    /**********************************************************
    /* Basic accessors
    /**********************************************************
     */

    /**
     * @deprecated Since 2.1: Use {@link #getFactory} instead.
     */
    @Deprecated
    public JsonFactory getJsonFactory() { return getFactory(); }

    /**
     * Accessor for finding underlying data format factory
     * ({@link JsonFactory}) codec will use for data binding.
     * 
     * @since 2.1
     */
    public JsonFactory getFactory() { return getJsonFactory(); }
}
