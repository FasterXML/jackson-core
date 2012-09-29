package com.fasterxml.jackson.core;

/**
 * Marker interface used to denote JSON Tree nodes, as far as
 * the core package knows them (which is very little): mostly
 * needed to allow {@link ObjectCodec} to have some level
 * of interoperability.
 * All real functionality is within <code>JsonNode</code>
 * base class in <code>mapper</code> package.
 *<p>
 * Note that in Jackson 1.x <code>JsonNode</code> itself
 * was part of core package: Jackson 2.x refactored this
 * since conceptually Tree Model is part of mapper package,
 * and so part visible to <code>core</code> package should
 * be minimized.
 */
public interface TreeNode
{
    /*
    /**********************************************************
    /* Minimal introspection methods
    /**********************************************************
     */
    
    /**
     * Method that can be used for efficient type detection
     * when using stream abstraction for traversing nodes.
     * Will return the first {@link JsonToken} that equivalent
     * stream event would produce (for most nodes there is just
     * one token but for structured/container types multiple)
     */
    JsonToken asToken();

    /**
     * If this node is a numeric type (as per {@link JsonToken#isNumeric}),
     * returns native type that node uses to store the numeric value;
     * otherwise returns null.
     * 
     * @return Type of number contained, if any; or null if node does not
     *  contain numeric value.
     */
    JsonParser.NumberType numberType();

    /*
    /**********************************************************
    /* Public API: converting to/from Streaming API
    /**********************************************************
     */

    /**
     * Method for constructing a {@link JsonParser} instance for
     * iterating over contents of the tree that this node is root of.
     * Functionally equivalent to first serializing tree using
     * {@link ObjectCodec} and then re-parsing but
     * more efficient.
     */
    JsonParser traverse();

}
